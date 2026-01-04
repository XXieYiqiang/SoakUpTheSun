package room

import (
	"sfu/internal/logger"
	roomLogic "sfu/internal/logic/room"
	"sfu/internal/model/res"
	"sfu/internal/types"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

// CloseRoom 关闭房间
func (r *RoomApi) CloseRoom(c *gin.Context) {
	req := new(types.CloseRoomReq)
	if err := c.ShouldBindJSON(&req); err != nil {
		res.Failed(c, "请输入房间号")
		logger.Log.Error("房间号输入错误", zap.Error(err))
		return
	}

	logic := roomLogic.NewCloseRoomLogic(c.Request.Context(), r.App)
	if err := logic.CloseRoom(req); err != nil {
		res.Failed(c, err.Error())
		return
	}

	res.OkWithMsg(c, "关闭房间成功")
}
