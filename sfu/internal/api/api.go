package api

import (
	"sfu/internal/api/room"
	"sfu/internal/app"
)

type API struct {
	RoomApi *room.RoomApi
}

func NewAPI(app *app.App) *API {
	return &API{
		RoomApi: room.NewRoomApi(app),
	}
}
