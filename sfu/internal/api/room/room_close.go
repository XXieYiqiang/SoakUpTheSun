package room

import (
	"sfu/internal/bind"
	roomLogic "sfu/internal/logic/room"
	"sfu/internal/model/res"
	"sfu/internal/types"

	"github.com/gin-gonic/gin"
)

// CloseRoom 关闭房间
func (r *RoomApi) CloseRoom(c *gin.Context) {
	req, err := bind.BindJson[types.CloseRoomReq](c)
	if err != nil {
		res.Failed(c, "请输入房间号")
		return
	}

	logic := roomLogic.NewCloseRoomLogic(c.Request.Context(), r.App)
	if err := logic.CloseRoom(req); err != nil {
		res.Failed(c, err.Error())
		return
	}

	res.OkWithMsg(c, "关闭房间成功")
}
