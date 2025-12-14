package ws

import (
	"sfu/utils/base58x"
	"sync"

	"github.com/google/uuid"
	"github.com/gorilla/websocket"
	"github.com/pion/webrtc/v4"
)

// UserRole 用户角色
type UserRole string

const (
	RolePatient   UserRole = "patient"   // 患者
	RoleVolunteer UserRole = "volunteer" // 志愿者
)

var (
	Rooms   = make(map[string]*Room)
	RoomsMu sync.Mutex
)

// Room 代表一个会议室，包含所有用户和他们发布的轨道
type Room struct {
	ID     string
	Users  map[string]*User       // UID -> User
	Tracks map[string]*UserTracks // Publisher UID -> UserTracks
	Mu     sync.RWMutex           // 保护 Users 和 Tracks
}

// User 代表一个连接到 SFU 的客户端
type User struct {
	UID              string
	WS               *websocket.Conn
	UpPC             *webrtc.PeerConnection // 上行 PC (Publisher)
	DownPC           *webrtc.PeerConnection // 下行 PC (Subscriber)
	Role             UserRole               // 用户角色
	closed           bool
	mu               sync.Mutex                // 保护 UpPC, DownPC, closed
	wsMu             sync.Mutex                // 保护 WS 写入操作
	UpCandidateQueue []webrtc.ICECandidateInit // 新增：用于缓冲在上行 Offer 之前到达的 ICE Candidate
	candidateMu      sync.Mutex                // 新增锁来保护 UpCandidateQueue，
}

// UserTracks 存储用户发布的本地轨道
type UserTracks struct {
	Audio *webrtc.TrackLocalStaticRTP
	Video *webrtc.TrackLocalStaticRTP
}

func NewRoom() *Room {
	r := &Room{
		ID:     base58x.UUIDToBase58(uuid.New()),
		Users:  make(map[string]*User),
		Tracks: make(map[string]*UserTracks),
	}
	RoomsMu.Lock()
	Rooms[r.ID] = r
	RoomsMu.Unlock()
	return r
}

func GetRoom(id string) (*Room, bool) {
	RoomsMu.Lock()
	defer RoomsMu.Unlock()
	r, ok := Rooms[id]
	if !ok {
		return nil, false
	}
	return r, true
}

func DeleteRoom(id string) {
	RoomsMu.Lock()
	defer RoomsMu.Unlock()
	delete(Rooms, id)
}
