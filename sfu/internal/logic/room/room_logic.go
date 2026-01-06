package room

import (
	"context"
	"sfu/internal/app"

	"github.com/redis/go-redis/v9"
	"gorm.io/gorm"
)

type RoomLogic struct {
	ctx context.Context
	db  *gorm.DB
	rds *redis.Client
}

func NewRoomLogic(ctx context.Context, app *app.App) *RoomLogic {
	return &RoomLogic{
		ctx: ctx,
		db:  app.DB,
		rds: app.Redis,
	}
}
