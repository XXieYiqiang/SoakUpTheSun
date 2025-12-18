package room

import (
	"sfu/internal/logger"
	"sfu/internal/model"
	"sfu/internal/model/res"
	"sfu/internal/ws"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

// SaveRoom 创建房间
func (r RoomApi) SaveRoom(c *gin.Context) {
	room := ws.NewRoom()
	if room == nil {
		res.Failed(c, "创建房间失败")
		logger.Log.Error("创建房间失败")
		return
	}

	// TODO 获取用户信息创建房间
	// 创建房间
	if err := r.db.Create(&model.Room{
		Name:        "张三的房间",
		PatientID:   114514,
		PatientName: "张三",
		UID:         room.ID,
	}).Error; err != nil {
		res.Failed(c, "创建房间失败")
		ws.DeleteRoom(room.ID)
		logger.Log.Error("创建房间失败", zap.Error(err))
		return
	}

	res.OkWithData(c, room.ID)
}

type RoomPreload struct {
	RoomID string `json:"roomID"`
	UserID int64  `json:"userID"`
	Role   string `json:"role"`
}

// 生成房间令牌
func (r RoomApi) generateRoomToken(roomUID string) (string, error) {
	// 生成房间令牌
	token, err := r.jwt.GenerateToken(RoomPreload{
		RoomID: roomUID,
		UserID: 114514,
		Role:   "patient",
	})
	if err != nil {
		return "", err
	}
	return token, nil
}
