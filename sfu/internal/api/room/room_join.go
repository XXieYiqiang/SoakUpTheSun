package room

import (
	"fmt"
	"sfu/internal/logger"
	roomLogic "sfu/internal/logic/room"
	"sfu/internal/model/res"
	"sfu/internal/ws"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"go.uber.org/zap"
)

// JoinRoom 加入房间
func (r *RoomApi) JoinRoom(c *gin.Context) {
	roomID := c.Query("roomID")
	if roomID == "" {
		logger.Log.Error("需要房间号")
		return
	}
	roomToken := c.Query("roomToken")
	fmt.Println("roomToken=", roomToken)
	if roomToken == "" {
		res.Failed(c, "您没有该房间权限")
		logger.Log.Error("没有该房间权限")
		return
	}

	logic := roomLogic.NewJoinRoomLogic(c.Request.Context(), r.App)
	room, err := logic.JoinRoom(roomToken, roomID)
	if err != nil {
		res.Failed(c, err.Error())
		return
	}

	// 升级websocket
	conn, err := ws.Upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		logger.Log.Error("创建websocket连接失败", zap.Error(err))
		return
	}

	// TODO 获取志愿者用户信息
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
		_ = conn.Close()
		return
	}

	logger.Log.Info("成功向新用户发送 server_ready 信令", zap.String("uid", user.UID))

	go ws.HandleWS(room, user)
}
