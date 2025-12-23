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

type CloseRoomReq struct {
	RoomID string `json:"roomID"`
}

// CloseRoom 关闭房间
func (r *RoomApi) CloseRoom(c *gin.Context) {
	req := new(CloseRoomReq)
	if err := c.ShouldBindJSON(&req); err != nil {
		res.Failed(c, "请输入房间号")
		logger.Log.Error("房间号输入错误", zap.Error(err))
		return
	}

	room, err := gorm.G[model.Room](r.db).Where("uid = ?", req.RoomID).First(c.Request.Context())
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			res.Failed(c, "房间不存在")
			return
		}
		res.Failed(c, "获取房间信息失败")
		logger.Log.Error("获取房间失败", zap.Error(err))
		return
	}

	// 数据库找得到房间，但是map中没有，说明房间已经关闭，删除map里的房间
	wsRoom, ok := ws.GetRoom(room.UID)
	if !ok {
		if _, err = gorm.G[model.Room](r.db).Updates(c.Request.Context(), model.Room{
			Status: model.RoomStatusClosed,
		}); err != nil {
			logger.Log.Error("关闭房间失败,更新房间状态失败", zap.Error(err))
		}
	}
	ws.DeleteRoom(wsRoom.ID)

	res.OkWithMsg(c, "关闭房间成功")
}
