package logic

import (
	"context"
	"errors"
	"fmt"
	"sfu/internal/app"
	"sfu/internal/logger"
	"sfu/internal/model"
	"sfu/internal/types"
	"sfu/internal/ws"

	"github.com/redis/go-redis/v9"
	"go.uber.org/zap"
	"gorm.io/gorm"
)

type InfoRoomLogic struct {
	ctx context.Context
	db  *gorm.DB
	rds *redis.Client
}

func NewInfoRoomLogic(ctx context.Context, app *app.App) *InfoRoomLogic {
	return &InfoRoomLogic{
		ctx: ctx,
		db:  app.DB,
		rds: app.Redis,
	}
}

func (i *InfoRoomLogic) InfoRoom(roomID string) (*types.GetRoomInfoResp, error) {
	roomModel, err := gorm.G[model.Room](i.db).Where("uid = ? AND status != ?", roomID, model.RoomStatusClosed).First(i.ctx)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, fmt.Errorf("房间不存在")
		}
		logger.Log.Error("获取房间信息失败", zap.Error(err))
		return nil, fmt.Errorf("获取房间信息失败")
	}

	if roomModel.Status == model.RoomStatusFull {
		return nil, fmt.Errorf("房间已满")
	}

	room, ok := ws.GetRoom(roomID)
	if !ok {
		if _, err = gorm.G[model.Room](i.db).Update(i.ctx, "status", model.RoomStatusClosed); err != nil {
			logger.Log.Error("更新房间状态失败", zap.Error(err))
		}
		return nil, fmt.Errorf("房间不存在")
	}
	return &types.GetRoomInfoResp{
		RoomID:   roomModel.UID,
		RoomName: roomModel.Name,
		Status:   roomModel.Status,
		Num:      len(room.Users),
	}, nil
}
