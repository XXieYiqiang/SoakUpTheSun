package room

import "sfu/internal/app"

type RoomApi struct {
	*app.App
}

func NewRoomApi(app *app.App) *RoomApi {
	return &RoomApi{
		App: app,
	}
}
