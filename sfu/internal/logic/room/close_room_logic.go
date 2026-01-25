package room

import (
	"errors"
	"sfu/internal/cache"
	"sfu/internal/logger"
	"sfu/internal/model"
	"sfu/internal/types"
	"sfu/internal/ws"

	"go.uber.org/zap"
	"gorm.io/gorm"
)

func (r *RoomLogic) CloseRoom(req *types.CloseRoomReq) error {
	_room, err := gorm.G[model.Room](r.db).Where("uid = ?", req.RoomID).First(r.ctx)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return errors.New("房间不存在")
		}
		logger.Log.Error("获取房间失败", zap.Error(err))
		return errors.New("获取房间信息失败")
	}

	// 数据库找得到房间，但是map中没有，说明房间已经关闭，删除map里的房间
	wsRoom, ok := ws.GetRoom(_room.UID)
	if !ok {
		if _, err = gorm.G[model.Room](r.db).Updates(r.ctx, model.Room{
			Status: model.RoomStatusClosed,
		}); err != nil {
			logger.Log.Error("关闭房间失败,更新房间状态失败", zap.Error(err))
		}
	}

	if err := r.rds.Del(r.ctx, cache.ROOM_TOKEN_KEY+_room.RoomToken).Err(); err != nil {
		logger.Log.Error("删除房间令牌失败", zap.Error(err))
	}

	ws.DeleteRoom(wsRoom.ID)
	return nil
}
