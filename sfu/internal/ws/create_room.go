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

	// TODO 从redis获取和存储房间
	roomLock.Lock()
	Rooms[room.ID] = room
	roomLock.Unlock()

	go func() {
		for range time.NewTicker(time.Second * 3).C {
			dispatchKeyFrame(room)
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
			signalPeerConnections(room)
		default:
		}
	})

	peerConnection.OnTrack(func(tr *webrtc.TrackRemote, r *webrtc.RTPReceiver) {
		logger.Log.Sugar().Infof("Got remote track: Kind=%s, ID=%s, PayloadType=%d", tr.Kind(), tr.ID(), tr.PayloadType())
		trackLocal := addTrack(room, tr)
		defer removeTrack(room, trackLocal)

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
	signalPeerConnections(room)

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
