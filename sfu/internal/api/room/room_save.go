package room

import (
	roomLogic "sfu/internal/logic/room"
	"sfu/internal/model/res"

	"github.com/gin-gonic/gin"
)

// SaveRoom 创建房间
func (r *RoomApi) SaveRoom(c *gin.Context) {
	userToken := c.GetHeader("token")
	if userToken == "" {
		res.Failed(c, "请先登录")
		return
	}
	logic := roomLogic.NewSaveRoomLogic(c.Request.Context(), r.App)
	resp, err := logic.SaveRoom(userToken)
	if err != nil {
		res.Failed(c, err.Error())
		return
	}
	res.OkWithData(c, resp)
}
