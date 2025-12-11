package app

import (
	"sfu/internal/config"
	"sfu/internal/logger"
)

type App struct {
	Config *config.Config
}

func NewApp() *App {
	conf := config.InitConfig()
	logger.InitZapLog(&conf.Log)
	return &App{
		Config: conf,
	}
}
