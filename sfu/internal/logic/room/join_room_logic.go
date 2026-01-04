package logic

import (
	"context"
	"fmt"
	"sfu/internal/app"
	"sfu/internal/cache"
	"sfu/internal/logger"
	"sfu/internal/types"
	"sfu/internal/ws"

	"github.com/bytedance/sonic"
	"github.com/redis/go-redis/v9"
	"go.uber.org/zap"
	"gorm.io/gorm"
)

type JoinRoomLogic struct {
	ctx context.Context
	db  *gorm.DB
	rds *redis.Client
}

func NewJoinRoomLogic(ctx context.Context, app *app.App) *JoinRoomLogic {
	return &JoinRoomLogic{
		ctx: ctx,
		db:  app.DB,
		rds: app.Redis,
	}
}

func (j *JoinRoomLogic) JoinRoom(roomToken, roomID string) (*ws.Room, error) {
	// 获取房间token信息
	roomTokenInfo, err := j.getRoomTokenInfo(j.ctx, roomToken)
	if err != nil {
		logger.Log.Error("房间token不存在", zap.Error(err))
		return nil, err
	}
	if roomTokenInfo.RoomID != roomID {
		logger.Log.Error("房间不匹配")
		return nil, fmt.Errorf("房间不匹配")
	}

	room, ok := ws.GetRoom(roomID)
	if !ok {
		logger.Log.Error("房间不存在")
		return nil, fmt.Errorf("房间不存在")
	}
	// 房间最多3人,1个患者和2个志愿者
	// TODO 可以读取房间角色判断是否满人
	currentNum := len(room.Users)
	if currentNum+1 > 3 {
		return nil, fmt.Errorf("房间已满")
	}

	return room, nil
}

func (j *JoinRoomLogic) getRoomTokenInfo(ctx context.Context, roomToken string) (*types.RoomPreload, error) {
	roomTokenKey := cache.ROOM_TOKEN_KEY + roomToken
	roomTokenInfoStr, err := j.rds.Get(ctx, roomTokenKey).Result()
	if err != nil {
		return nil, fmt.Errorf("获取房间token信息失败: %w", err)
	}
	var roomTokenInfo types.RoomPreload

	if err := sonic.UnmarshalString(roomTokenInfoStr, &roomTokenInfo); err != nil {
		return nil, fmt.Errorf("解析房间token信息失败: %w", err)
	}
	return &roomTokenInfo, nil
}
