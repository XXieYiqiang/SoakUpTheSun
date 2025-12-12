package room

import (
	"sfu/internal/logger"
	"sfu/internal/model/res"
	"sfu/internal/ws"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"go.uber.org/zap"
)

func (RoomApi) JoinRoom(c *gin.Context) {
	roomID := c.Query("roomID")
	if roomID == "" {
		logger.Log.Error("需要房间号")
		return
	}

	room, ok := ws.GetRoom(roomID)
	if !ok {
		logger.Log.Error("房间不存在")
		return
	}

	// 升级websocket
	conn, err := ws.Upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		res.Failed(c, "创建连接失败")
		logger.Log.Error("创建websocket连接失败", zap.Error(err))
		return
	}

	// TODO 获取用户信息
	user := &ws.User{
		UID:  uuid.New().String(),
		WS:   conn,
		Role: ws.RoleVolunteer,
	}

	room.Mu.Lock()
	room.Users[user.UID] = user
	room.Mu.Unlock()

	go ws.HandleWS(room, user)
}
