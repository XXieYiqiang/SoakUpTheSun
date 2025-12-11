package app

import (
	"sfu/internal/config"
	"sfu/internal/database"
	"sfu/internal/logger"

	"gorm.io/gorm"
)

type App struct {
	Config *config.Config
	DB     *gorm.DB
}

func NewApp() *App {
	// 初始化配置文件
	conf := config.InitConfig()
	// 初始化日志(全局变量)
	logger.InitZapLog(&conf.Log)
	// 初始化数据库
	db := database.InitMysql(&conf.Mysql)
	return &App{
		Config: conf,
		DB:     db,
	}
}
