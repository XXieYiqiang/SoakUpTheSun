package room

import (
	"context"
	"errors"
	"fmt"
	"sfu/internal/cache"
	"sfu/internal/logger"
	"sfu/internal/model"
	"sfu/internal/types"
	"sfu/internal/ws"

	"github.com/bytedance/sonic"
	"go.uber.org/zap"
	"gorm.io/gorm"
)

func (j *RoomLogic) JoinRoom(roomToken, roomID string) (*ws.Room, error) {
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

	_room, ok := ws.GetRoom(roomID)
	if !ok {
		logger.Log.Error("房间不存在")
		return nil, fmt.Errorf("房间不存在")
	}
	// 房间最多3人,1个患者和2个志愿者
	// TODO 可以读取房间角色判断是否满人
	currentNum := len(_room.Users)
	if currentNum+1 > 3 {
		return nil, fmt.Errorf("房间已满")
	}

	// TODO 判断加入患者或志愿者
	if err := gorm.G[model.RoomMember](j.db).Create(j.ctx, &model.RoomMember{
		RoomUID: roomID,
		UserID:  12345,
		Role:    "volunteer",
	}); err != nil {
		logger.Log.Error("加入房间失败", zap.Error(err))
		return nil, errors.New("加入房间失败")
	}

	return _room, nil
}

func (j *RoomLogic) getRoomTokenInfo(ctx context.Context, roomToken string) (*types.RoomPreload, error) {
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
