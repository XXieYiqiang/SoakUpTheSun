package ws

import (
	"net"
	"net/http"
	"sync"

	"github.com/google/uuid"
	"github.com/gorilla/websocket"
	"github.com/pion/webrtc/v4"
)

// UserRole 用户角色
type UserRole string

const (
	RolePatient UserRole = "patient"   // 患者
	RoleDoctor  UserRole = "volunteer" // 志愿者
)

type Room struct {
	ID              string                                 // 房间ID
	Users           map[string]*User                       // 房间用户
	wsUpgrader      websocket.Upgrader                     // websocket 升级器
	peerConnections []peerConnectionState                  // 房间peer连接
	tracksLocals    map[string]*webrtc.TrackLocalStaticRTP // 房间track
	Mutex           *sync.Mutex                            // 锁
}

type User struct {
	UID   string   // 用户UID
	Addr  net.IP   // 用户IP
	Name  string   // 用户名称
	Owner bool     // 是否为房间所有者
	Role  UserRole // 用户角色
}

type peerConnectionState struct {
	peerConnection *webrtc.PeerConnection
	*websocket.Conn
}

/*
 * @Description: 创建房间
 * @Return *Room 房间
 */
func NewRoom(user *User) *Room {
	// 患者发起房间
	patient := &User{
		UID:   user.UID,
		Addr:  user.Addr,
		Name:  user.Name,
		Owner: true,
		Role:  RolePatient,
	}
	usersMap := make(map[string]*User)
	usersMap[patient.UID] = patient
	return &Room{
		ID:           uuid.New().String(),
		Users:        usersMap,
		Mutex:        new(sync.Mutex),
		tracksLocals: make(map[string]*webrtc.TrackLocalStaticRTP),
		wsUpgrader: websocket.Upgrader{
			ReadBufferSize:  1024, // 读取缓冲区大小
			WriteBufferSize: 1024, // 写入缓冲区大小
			CheckOrigin:     func(r *http.Request) bool { return true },
		},
	}
}
