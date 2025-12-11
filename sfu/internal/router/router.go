package router

import (
	"sfu/internal/app"
	"sfu/internal/middleware"
	"sfu/internal/ws"

	"github.com/gin-gonic/gin"
)

func NewRouter(app *app.App) *gin.Engine {

	switch app.Config.System.Env {
	case "release":
		gin.SetMode(gin.ReleaseMode)
	case "debug":
		gin.SetMode(gin.DebugMode)
	case "test":
		gin.SetMode(gin.TestMode)
	default:
		gin.SetMode(gin.DebugMode)
	}

	router := gin.New()

	router.Use(middleware.RecoveryMiddleware(true))

	userRoomApi := ws.NewUserRoomApi(app)
	router.GET("/create-room", userRoomApi.CreateRoom)

	return router
}
