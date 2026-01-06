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
	roomModel, err := gorm.G[model.Room](i.db).Where("uid = ? AND status != ?", req.RoomID, model.RoomStatusClosed).First(i.ctx)
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

	_room, ok := ws.GetRoom(req.RoomID)
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
		Num:      len(_room.Users),
	}, nil
}
