package app

import (
	"flag"
	"log"
	"sfu/internal/cache"
	"sfu/internal/config"
	"sfu/internal/database"
	"sfu/internal/logger"
	"sfu/utils/snowflakex"

	"github.com/redis/go-redis/v9"
	"gorm.io/gorm"
)

type App struct {
	Config *config.Config
	DB     *gorm.DB
	Redis  *redis.Client
}

// 默认配置文件路径
var configFile = flag.String("f", "./config.yaml", "config file path")

func NewApp() *App {
	// 初始化配置文件
	conf, err := config.LoadConfig[config.Config](*configFile)
	if err != nil {
		log.Fatalf("load config file failed: %v", err)
	}
	// 初始化日志(全局变量)
	logger.InitZapLog(&conf.Log)
	// 初始化雪花算法节点
	if err := snowflakex.Init(1); err != nil {
		log.Fatalf("init snowflake node failed: %v", err)
	}
	// 初始化数据库
	db := database.InitMysql(conf)
	// 初始化 Redis 客户端
	redisClient := cache.InitRedisClient(&conf.Redis)
	return &App{
		Config: conf,
		DB:     db,
		Redis:  redisClient,
	}
}
