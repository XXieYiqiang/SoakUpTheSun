package ws

import (
	"sfu/utils/base58x"
	"sync"

	"github.com/google/uuid"
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
	Users  map[string]*Peer       // UID -> Peer
	Tracks map[string]*PeerTracks // Publisher UID -> PeerTracks
	Mu     sync.RWMutex           // 保护 Users 和 Tracks
}

func NewRoom() *Room {
	r := &Room{
		ID:     base58x.UUIDToBase58(uuid.New()),
		Users:  make(map[string]*Peer),
		Tracks: make(map[string]*PeerTracks),
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
	delete(Rooms, id)
	RoomsMu.Unlock()
}
