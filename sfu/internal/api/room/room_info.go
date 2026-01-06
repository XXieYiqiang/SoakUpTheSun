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
	req, err := bind.BindUri[types.GetRoomInfoReq](c)
	if err != nil {
		res.Failed(c, "非法参数")
		return
	}
	logic := roomLogic.NewRoomLogic(c.Request.Context(), r.App)
	resp, err := logic.InfoRoom(req)
	if err != nil {
		res.Failed(c, err.Error())
		return
	}
	res.OkWithData(c, resp)
}
