package ws

import (
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"sfu/internal/app"
	"sfu/internal/logger"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/pion/rtcp"
	"github.com/pion/rtp"
	"github.com/pion/webrtc/v4"
	"go.uber.org/zap"
)

type UserRoom struct {
	*app.App
}

type UpgradeRequest struct {
	UserID string `form:"userID"`
}

type websocketMessage struct {
	Event string `json:"event"`
	Data  string `json:"data"`
}

var (
	Rooms    = make(map[string]*Room)
	roomLock = new(sync.Mutex)
)

func NewUserRoomApi(app *app.App) *UserRoom {
	return &UserRoom{
		App: app,
	}
}

// CreateRoom 患者创建房间
func (u UserRoom) CreateRoom(c *gin.Context) {
	// TODO 调用获取用户信息接口
	room := NewRoom(&User{
		UID:   "131",
		Addr:  net.IP(c.ClientIP()),
		Name:  "user_131",
		Owner: true,
		Role:  RolePatient,
	})

	roomLock.Lock()
	Rooms[room.ID] = room
	roomLock.Unlock()

	go func() {
		for range time.NewTicker(time.Second * 3).C {
			u.dispatchKeyFrame(room)
		}
	}()

	conn, err := room.wsUpgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    0,
			"message": "Internal Server Error",
		})
		logger.Log.Error("CreateRoom failed", zap.Error(err))
		return
	}
	defer func() {
		_ = conn.Close()
	}()

	// 创建PeerConnection
	peerConnection, err := webrtc.NewPeerConnection(webrtc.Configuration{})
	if err != nil {
		logger.Log.Error("Failed to creates a PeerConnection", zap.Error(err))
		return
	}
	defer func() {
		_ = peerConnection.Close()
	}()

	// 添加音频和视频接收通道
	for _, rtpCodeType := range []webrtc.RTPCodecType{webrtc.RTPCodecTypeAudio, webrtc.RTPCodecTypeVideo} {
		_, err = peerConnection.AddTransceiverFromKind(rtpCodeType, webrtc.RTPTransceiverInit{
			Direction: webrtc.RTPTransceiverDirectionRecvonly,
		})
		if err != nil {
			logger.Log.Error("Failed to add transceiver", zap.Error(err))
			return
		}
	}
	// 添加peerConnection到房间
	room.Mutex.Lock()
	room.peerConnections = append(room.peerConnections, peerConnectionState{peerConnection, conn})
	room.Mutex.Unlock()

	// 监听ICE候选事件
	peerConnection.OnICECandidate(func(candidate *webrtc.ICECandidate) {
		if candidate == nil {
			return
		}
		candidateString, err := json.Marshal(candidate.ToJSON())
		if err != nil {
			logger.Log.Error("Failed to marshal candidate to json: %v", zap.Error(err))
			return
		}
		logger.Log.Info("Send candidate to client", zap.String("candidate", string(candidateString)))

		if err = conn.WriteJSON(&websocketMessage{
			Event: "candidate",
			Data:  string(candidateString),
		}); err != nil {
			logger.Log.Error("Failed to write JSON", zap.Error(err))
			return
		}
	})

	// 监听PeerConnection状态变化事件
	peerConnection.OnConnectionStateChange(func(state webrtc.PeerConnectionState) {
		logger.Log.Info("PeerConnection state changed", zap.String("state", state.String()))
		switch state {
		case webrtc.PeerConnectionStateFailed:
			if err := peerConnection.Close(); err != nil {
				logger.Log.Error("Failed to close PeerConnection", zap.Error(err))
			}
		case webrtc.PeerConnectionStateClosed:
			u.signalPeerConnections(room)
		default:
		}
	})

	peerConnection.OnTrack(func(tr *webrtc.TrackRemote, r *webrtc.RTPReceiver) {
		logger.Log.Sugar().Infof("Got remote track: Kind=%s, ID=%s, PayloadType=%d", tr.Kind(), tr.ID(), tr.PayloadType())
		trackLocal := u.addTrack(room, tr)
		defer u.removeTrack(room, trackLocal)

		buf := make([]byte, 1500)
		rtpPkt := new(rtp.Packet)

		for {
			i, _, err := tr.Read(buf)
			if err != nil {
				return
			}
			if err = rtpPkt.Unmarshal(buf[:i]); err != nil {
				logger.Log.Error("Failed to unmarshal incoming RTP packet: %v", zap.Error(err))
				return
			}
			rtpPkt.Extension = false
			rtpPkt.Extensions = nil
			if err = trackLocal.WriteRTP(rtpPkt); err != nil {
				return
			}
		}
	})

	peerConnection.OnICEConnectionStateChange(func(state webrtc.ICEConnectionState) {
		logger.Log.Info("ICEConnection state changed", zap.String("state", state.String()))
	})

	// 为新的 PeerConnection 触发信令
	u.signalPeerConnections(room)

	message := new(websocketMessage)
	for {
		_, raw, err := conn.ReadMessage()
		if err != nil {
			logger.Log.Error("Failed to read message:", zap.Error(err))
			return
		}
		logger.Log.Sugar().Infof("Got message: %s", raw)

		fmt.Println("get message", message)

		if err := json.Unmarshal(raw, &message); err != nil {
			logger.Log.Error("Failed to unmarshal json to message:", zap.Error(err))
			return
		}

		switch message.Event {
		case "candidate":
			candidate := webrtc.ICECandidateInit{}
			if err := json.Unmarshal([]byte(message.Data), &candidate); err != nil {
				logger.Log.Error("Failed to unmarshal json to candidate:", zap.Error(err))

				return
			}

			logger.Log.Sugar().Infof("Got candidate:", candidate)

			if err := peerConnection.AddICECandidate(candidate); err != nil {
				logger.Log.Error("Failed to add ICE candidate:", zap.Error(err))

				return
			}
		case "answer":
			answer := webrtc.SessionDescription{}
			if err := json.Unmarshal([]byte(message.Data), &answer); err != nil {
				logger.Log.Error("Failed to unmarshal json to answer:", zap.Error(err))

				return
			}

			logger.Log.Sugar().Infof("Got answer: %v", answer)

			if err := peerConnection.SetRemoteDescription(answer); err != nil {
				logger.Log.Error("Failed to set remote description:", zap.Error(err))

				return
			}
		default:
			logger.Log.Error("unknown message:", zap.Any("message", message))
		}
	}

}

// addTrack 添加track到房间
func (u UserRoom) addTrack(room *Room, t *webrtc.TrackRemote) *webrtc.TrackLocalStaticRTP {
	room.Mutex.Lock()
	defer func() {
		room.Mutex.Unlock()
		u.signalPeerConnections(room)
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

func (u UserRoom) removeTrack(room *Room, t *webrtc.TrackLocalStaticRTP) {
	room.Mutex.Lock()
	defer func() {
		room.Mutex.Unlock()
		u.signalPeerConnections(room)
	}()

	delete(room.tracksLocals, t.ID())
}

func (u UserRoom) signalPeerConnections(room *Room) {
	room.Mutex.Lock()
	defer func() {
		room.Mutex.Unlock()
		u.dispatchKeyFrame(room)
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
				u.signalPeerConnections(room)
			}()

			return
		}

		if !attemptSync() {
			break
		}
	}
}

func (u UserRoom) dispatchKeyFrame(room *Room) {
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
