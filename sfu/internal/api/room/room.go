package room

import (
	"sfu/internal/app"
	"sfu/utils/jwtx"

	"github.com/redis/go-redis/v9"
	"gorm.io/gorm"
)

type RoomApi struct {
	db    *gorm.DB
	redis *redis.Client
	jwt   *jwtx.Service[any]
}

func NewRoomApi(app *app.App) *RoomApi {
	return &RoomApi{
		db:    app.DB,
		redis: app.Redis,
		jwt:   app.Jwt,
	}
}
