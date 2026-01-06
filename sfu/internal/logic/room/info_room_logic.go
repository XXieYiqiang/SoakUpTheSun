package room

import (
	"errors"
	"fmt"
	"sfu/internal/logger"
	"sfu/internal/model"
	"sfu/internal/types"
	"sfu/internal/ws"

	"go.uber.org/zap"
	"gorm.io/gorm"
)

func (i *RoomLogic) InfoRoom(req *types.GetRoomInfoReq) (*types.GetRoomInfoResp, error) {
	_room, err := gorm.G[model.Room](i.db).Where("room_token = ? AND status != ?", req.RoomToken, model.RoomStatusClosed).First(i.ctx)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, fmt.Errorf("房间不存在")
		}
		logger.Log.Error("获取房间信息失败", zap.Error(err))
		return nil, fmt.Errorf("获取房间信息失败")
	}

	if _room.Status == model.RoomStatusFull {
		return nil, fmt.Errorf("房间已满")
	}

	wsRoom, ok := ws.GetRoom(_room.UID)
	if !ok {
		if _, err = gorm.G[model.Room](i.db).Where("id = ?", _room.ID).Update(i.ctx, "status", model.RoomStatusClosed); err != nil {
			logger.Log.Error("更新房间状态失败", zap.Error(err))
		}
		return nil, fmt.Errorf("房间不存在")
	}
	return &types.GetRoomInfoResp{
		RoomID:   _room.UID,
		RoomName: _room.Name,
		Status:   _room.Status,
		Num:      len(wsRoom.Users),
	}, nil
}
