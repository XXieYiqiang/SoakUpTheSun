package room

import (
	"context"
	"fmt"
	"sfu/internal/cache"
	"sfu/internal/logger"
	"sfu/internal/model/res"
	"sfu/internal/ws"

	"github.com/bytedance/sonic"
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

	// 获取房间token信息
	roomTokenInfo, err := r.getRoomTokenInfo(c.Request.Context(), roomToken)
	if err != nil {
		logger.Log.Error("房间token不存在", zap.Error(err))
		return
	}
	if roomTokenInfo.RoomID != roomID {
		logger.Log.Error("房间不匹配")
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
		logger.Log.Error("房间已满")
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
		// 发送失败后，应该立即关闭连接并清理用户
		_ = conn.Close()
		return
	}

	logger.Log.Info("成功向新用户发送 server_ready 信令", zap.String("uid", user.UID))

	go ws.HandleWS(room, user)
}

func (r *RoomApi) getRoomTokenInfo(ctx context.Context, roomToken string) (*RoomPreload, error) {
	roomTokenKey := cache.ROOM_TOKEN_KEY + roomToken
	roomTokenInfoStr, err := r.redis.Get(ctx, roomTokenKey).Result()
	if err != nil {
		return nil, fmt.Errorf("获取房间token信息失败: %w", err)
	}
	var roomTokenInfo RoomPreload
	// @author:lml; 这里必须要加&引用传参
	if err := sonic.UnmarshalString(roomTokenInfoStr, &roomTokenInfo); err != nil {
		return nil, fmt.Errorf("解析房间token信息失败: %w", err)
	}
	return &roomTokenInfo, nil
}
