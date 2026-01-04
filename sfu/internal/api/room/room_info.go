package room

import (
	"sfu/internal/bind"
	roomLogic "sfu/internal/logic/room"
	"sfu/internal/model/res"
	"sfu/internal/types"

	"github.com/gin-gonic/gin"
)

// GetRoomInfo 获取房间信息
func (r *RoomApi) GetRoomInfo(c *gin.Context) {
	room, err := bind.BindUri[types.GetRoomInfoReq](c)
	if err != nil {
		res.Failed(c, "请输入房间号")
		return
	}
	logic := roomLogic.NewInfoRoomLogic(c.Request.Context(), r.App)
	resp, err := logic.InfoRoom(room)
	if err != nil {
		res.Failed(c, err.Error())
		return
	}
	res.OkWithData(c, resp)
}
