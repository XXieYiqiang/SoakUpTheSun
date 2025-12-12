package ws

import (
	"encoding/json"
	"sfu/internal/logger"
	"time"

	"github.com/pion/rtcp"
	"github.com/pion/webrtc/v4"
	"go.uber.org/zap"
)

// addTrack 添加track到房间
func addTrack(room *Room, t *webrtc.TrackRemote) *webrtc.TrackLocalStaticRTP {
	room.Mutex.Lock()
	defer func() {
		room.Mutex.Unlock()
		signalPeerConnections(room)
	}()
	// 创建本地track
	trackLocal, err := webrtc.NewTrackLocalStaticRTP(t.Codec().RTPCodecCapability, t.ID(), t.StreamID())
	if err != nil {
		logger.Log.Error("Failed to create track local", zap.Error(err))
		return nil
	}
	room.tracksLocals[t.ID()] = trackLocal
	return trackLocal
}

func removeTrack(room *Room, t *webrtc.TrackLocalStaticRTP) {
	room.Mutex.Lock()
	defer func() {
		room.Mutex.Unlock()
		signalPeerConnections(room)
	}()

	delete(room.tracksLocals, t.ID())
}

func signalPeerConnections(room *Room) {
	room.Mutex.Lock()
	defer func() {
		room.Mutex.Unlock()
		dispatchKeyFrame(room)
	}()
	attemptSync := func() (tryAgain bool) {
		for i := range room.peerConnections {

			if room.peerConnections[i].peerConnection.ConnectionState() == webrtc.PeerConnectionStateClosed {
				room.peerConnections = append(room.peerConnections[:i], room.peerConnections[i+1:]...)
				return true // 修改了切片，从头开始
			}

			/*
				记录我们已经发送过的 sender，这样就不会重复发送 track 了
				WebRTC 的 PeerConnection 会有很多 RTPSender（每个 Track 对应一个 Sender）
				如果不记录已经发送过哪些 Track：
				1. 会重复发送 track，导致接收端重复解码
				2. 重复发送同一条轨道
				3. 导致客户端出现多个重复的视频/音频轨道
			*/
			existingSenders := map[string]bool{}

			for _, sender := range room.peerConnections[i].peerConnection.GetSenders() {
				if sender.Track() == nil {
					continue
				}
				existingSenders[sender.Track().ID()] = true

				// 不要接收自己发送出去的track，避免产生循环
				if _, ok := room.tracksLocals[sender.Track().ID()]; ok {
					if err := room.peerConnections[i].peerConnection.RemoveTrack(sender); err != nil {
						return true
					}
				}
			}

			// 不要接收自己发送的track，确保不会产生循环
			for _, receiver := range room.peerConnections[i].peerConnection.GetReceivers() {
				if receiver.Track() == nil {
					continue
				}
				existingSenders[receiver.Track().ID()] = true
			}

			// 遍历所有track，检查是否需要添加接收者
			for trackID := range room.tracksLocals {
				if _, ok := existingSenders[trackID]; !ok {
					if _, err := room.peerConnections[i].peerConnection.AddTrack(room.tracksLocals[trackID]); err != nil {
						return true
					}
				}
			}

			offer, err := room.peerConnections[i].peerConnection.CreateOffer(nil)
			if err != nil {
				return true
			}
			if err = room.peerConnections[i].peerConnection.SetLocalDescription(offer); err != nil {
				return true
			}

			offerString, err := json.Marshal(offer)
			if err != nil {
				return true
			}

			err = room.peerConnections[i].Conn.WriteJSON(&websocketMessage{
				Event: "offer",
				Data:  string(offerString),
			})
			if err != nil {
				return true
			}

		}
		return tryAgain
	}

	//尝试最多 25 次调用 attemptSync()，如果每次 attemptSync() 都失败（返回 true 代表需要重试），那么暂停 3 秒后重新调用 signalPeerConnections()（重新同步）。
	for syncAttempt := 0; ; syncAttempt++ {
		if syncAttempt == 25 {
			go func() {
				time.Sleep(time.Second * 3)
				signalPeerConnections(room)
			}()

			return
		}

		if !attemptSync() {
			break
		}
	}
}

func dispatchKeyFrame(room *Room) {
	room.Mutex.Lock()
	defer func() {
		room.Mutex.Unlock()
	}()

	for i := range room.peerConnections {
		for _, receiver := range room.peerConnections[i].peerConnection.GetReceivers() {
			if receiver.Track() == nil {
				continue
			}

			_ = room.peerConnections[i].peerConnection.WriteRTCP([]rtcp.Packet{
				&rtcp.PictureLossIndication{
					MediaSSRC: uint32(receiver.Track().SSRC()),
				},
			})
		}
	}
}
