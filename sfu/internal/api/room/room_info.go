package room

import (
	roomLogic "sfu/internal/logic/room"
	"sfu/internal/model/res"

	"github.com/gin-gonic/gin"
)

// GetRoomInfo 获取房间信息
func (r *RoomApi) GetRoomInfo(c *gin.Context) {
	roomID := c.Param("roomID")
	if roomID == "" {
		res.Failed(c, "请输入房间号")
		return
	}
	logic := roomLogic.NewInfoRoomLogic(c.Request.Context(), r.App)
	resp, err := logic.InfoRoom(roomID)
	if err != nil {
		res.Failed(c, err.Error())
		return
	}
	res.OkWithData(c, resp)
}
