package app

import (
	"sfu/internal/cache"
	"sfu/internal/config"
	"sfu/internal/database"
	"sfu/internal/logger"
	"sfu/utils/jwtx"

	"github.com/redis/go-redis/v9"
	"gorm.io/gorm"
)

type App struct {
	Config *config.Config
	DB     *gorm.DB
	Redis  *redis.Client
	Jwt    *jwtx.Service[any]
}

func NewApp() *App {
	// 初始化配置文件
	conf := config.InitConfig()
	// 初始化日志(全局变量)
	logger.InitZapLog(&conf.Log)
	// 初始化数据库
	db := database.InitMysql(conf)
	// 初始化 Redis 客户端
	redisClient := cache.InitRedisClient(&conf.Redis)
	return &App{
		Config: conf,
		DB:     db,
		Redis:  redisClient,
		Jwt:    jwtx.NewJwt[any](conf.Jwt.Secret, conf.Jwt.Expire, conf.Jwt.Issuer),
	}
}
