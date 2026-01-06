package model

import "gorm.io/gorm"

type RoomMember struct {
	gorm.Model
	RoomUID string `gorm:"type:varchar(50);not null;comment:房间唯一标识(uid),即房间号"`
	UserID  int64  `gorm:"type:bigint;not null;comment:用户id"`
	Role    string `gorm:"type:varchar(20);not null;default:'patient';comment:房间成员角色(patient-患者/volunteer-志愿者)"`
}

func (RoomMember) TableName() string {
	return "t_room_member"
}
