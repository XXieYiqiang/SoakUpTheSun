package room

import (
	"errors"
	"sfu/internal/logger"
	"sfu/internal/model"
	"sfu/internal/model/res"
	"sfu/internal/ws"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
	"gorm.io/gorm"
)

type GetRoomInfoResponse struct {
	RoomID   string `json:"roomID"`   // 房间号
	RoomName string `json:"roomName"` // 房间名称
	Status   string `json:"status"`   // 房间状态
	Num      int    `json:"num"`      // 房间人数
}

// GetRoomInfo 获取房间信息
func (r RoomApi) GetRoomInfo(c *gin.Context) {
	roomID := c.Param("roomID")
	if roomID == "" {
		res.Failed(c, "请输入房间号")
		return
	}

	roomModel, err := gorm.G[model.Room](r.db).Where("uid = ? AND status != ?", roomID, model.RoomStatusClosed).First(c.Request.Context())
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			res.Failed(c, "房间不存在")
			return
		}
		res.Failed(c, "获取房间信息失败")
		logger.Log.Error("获取房间信息失败", zap.Error(err))
		return
	}

	if roomModel.Status == model.RoomStatusFull {
		res.Failed(c, "房间已满")
		return
	}

	room, ok := ws.GetRoom(roomID)
	if !ok {
		if _, err = gorm.G[model.Room](r.db).Update(c.Request.Context(), "status", model.RoomStatusClosed); err != nil {
			logger.Log.Error("更新房间状态失败", zap.Error(err))
		}
		res.Failed(c, "房间不存在")
		return
	}

	res.OkWithData(c, GetRoomInfoResponse{
		RoomID:   roomModel.UID,
		RoomName: roomModel.Name,
		Status:   roomModel.Status,
		Num:      len(room.Users),
	})
}
