package model

import "gorm.io/gorm"

// Room 房间模型
type Room struct {
	gorm.Model
	Name        string `gorm:"column:name;type:varchar(50);not null;comment:房间名称(xxx的房间)"`
	PatientID   uint   `gorm:"column:patient_id;type:int(11);not null;comment:患者id"`
	PatientName string `gorm:"column:patient_name;type:varchar(50);not null;comment:患者名称"`
	UID         string `gorm:"column:uid;type:varchar(100);not null;uniqueIndex;comment:房间唯一标识(uuid),即房间号"`
	Status      string `gorm:"column:status;type:varchar(20);not null;default:'active';comment:房间状态(active-开启中/full-已满/closed-已关闭)"`
}

const (
	RoomStatusActive = "active"
	RoomStatusFull   = "full"
	RoomStatusClosed = "closed"
)

func (Room) TableName() string {
	return "t_room"
}
