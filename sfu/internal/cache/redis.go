package cache

import (
	"context"
	"fmt"
	"log"
	"sfu/internal/config"

	"github.com/redis/go-redis/v9"
)

// InitRedisClient 初始化 Redis 客户端
func InitRedisClient(conf *config.RedisConfig) *redis.Client {
	client := redis.NewClient(&redis.Options{
		Addr:     conf.Addr,
		Password: conf.Password,
		DB:       conf.DB,
	})
	pong, err := client.Ping(context.Background()).Result()
	if err != nil {
		log.Fatalf("redis ping error: %v", err)
	}
	fmt.Printf("redis ping: %s\n", pong)
	return client
}
