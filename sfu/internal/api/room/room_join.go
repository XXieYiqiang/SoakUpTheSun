package room

import (
	"sfu/internal/logger"
	"sfu/internal/model/res"
	"sfu/internal/ws"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"go.uber.org/zap"
)

// JoinRoom 加入房间
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

	// 房间最多3人,1个患者和2个志愿者
	// TODO 可以读取房间角色判断是否满人
	currentNum := len(room.Users)
	if currentNum+1 > 3 {
		res.Failed(c, "房间已满")
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

	readyMsg := map[string]any{
		"event": "server_ready",
		"data": map[string]string{
			"uid": user.UID,
		},
	}

	if err := user.WS.WriteJSON(readyMsg); err != nil {
		logger.Log.Error("发送 server_ready 信令失败", zap.Error(err))
		// 发送失败后，应该立即关闭连接并清理用户
		conn.Close()
		return
	}

	logger.Log.Info("成功向新用户发送 server_ready 信令", zap.String("uid", user.UID))

	go ws.HandleWS(room, user)
}
