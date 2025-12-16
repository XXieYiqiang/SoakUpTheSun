package router

import (
	"sfu/internal/api"
	"sfu/internal/app"
	"sfu/internal/middleware"

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

	router.Use(middleware.RecoveryMiddleware(true), gin.Logger())

	api := api.NewAPI(app)
	roomApi := api.RoomApi

	router.POST("/room", roomApi.SaveRoom)           // 创建房间
	router.GET("/room/join", roomApi.JoinRoom)       // 加入房间
	router.GET("/room/:roomID", roomApi.GetRoomInfo) // 获取房间信息
	router.POST("/room/close", roomApi.CloseRoom)    // 关闭房间

	return router
}
