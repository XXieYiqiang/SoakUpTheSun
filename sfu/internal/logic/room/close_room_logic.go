package logic

import (
	"context"
	"errors"
	"sfu/internal/app"
	"sfu/internal/logger"
	"sfu/internal/model"
	"sfu/internal/types"
	"sfu/internal/ws"

	"github.com/redis/go-redis/v9"
	"go.uber.org/zap"
	"gorm.io/gorm"
)

type CloseRoomLogic struct {
	ctx context.Context
	db  *gorm.DB
	rds *redis.Client
}

func NewCloseRoomLogic(ctx context.Context, app *app.App) *CloseRoomLogic {
	return &CloseRoomLogic{
		ctx: ctx,
		db:  app.DB,
		rds: app.Redis,
	}
}

func (l *CloseRoomLogic) CloseRoom(req *types.CloseRoomReq) error {
	room, err := gorm.G[model.Room](l.db).Where("uid = ?", req.RoomID).First(l.ctx)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return errors.New("房间不存在")
		}
		logger.Log.Error("获取房间失败", zap.Error(err))
		return errors.New("获取房间信息失败")
	}

	// 数据库找得到房间，但是map中没有，说明房间已经关闭，删除map里的房间
	wsRoom, ok := ws.GetRoom(room.UID)
	if !ok {
		if _, err = gorm.G[model.Room](l.db).Updates(l.ctx, model.Room{
			Status: model.RoomStatusClosed,
		}); err != nil {
			logger.Log.Error("关闭房间失败,更新房间状态失败", zap.Error(err))
		}
	}
	ws.DeleteRoom(wsRoom.ID)
	return nil
}
