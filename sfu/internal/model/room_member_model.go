package model

import "gorm.io/gorm"

type RoomMember struct {
	gorm.Model
	RoomUID string `gorm:"type:varchar(100);not null;comment:房间唯一标识(uid),即房间号"`
	UserID  uint   `gorm:"type:bigint;not null;comment:用户id"`
	Role    string `gorm:"type:varchar(20);not null;default:'patient';comment:房间成员角色(patient-患者/doctor-医生)"`
}

func (RoomMember) TableName() string {
	return "t_room_member"
}
