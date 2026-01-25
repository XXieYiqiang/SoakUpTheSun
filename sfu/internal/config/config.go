package config

import (

	// _ "embed"
	"bytes"
	"fmt"
	"os"

	"github.com/spf13/viper"
)

// //go:embed config.yaml
// var configFile []byte

type Config struct {
	Log    LogConfig    `mapstructure:"log" json:"log" yaml:"log"`
	System SystemConfig `mapstructure:"system" json:"system" yaml:"system"`
	Mysql  MysqlConfig  `mapstructure:"mysql" json:"mysql" yaml:"mysql"`
	Jwt    JwtConfig    `mapstructure:"jwt" json:"jwt" yaml:"jwt"`
	Redis  RedisConfig  `mapstructure:"redis" json:"redis" yaml:"redis"`
}

// InitConfig 初始化配置文件
func LoadConfig[T any](path string) (*T, error) {
	v := viper.New()

	content, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read config file failed: %w", err)
	}

	v.SetConfigType("yaml") // 或根据扩展名判断

	if err := v.ReadConfig(bytes.NewBuffer(content)); err != nil {
		return nil, fmt.Errorf("read config failed: %w", err)
	}

	var cfg T
	if err := v.Unmarshal(&cfg); err != nil {
		return nil, fmt.Errorf("unmarshal config failed: %w", err)
	}

	return &cfg, nil
}

// InitConfigByEmbed 初始化配置文件（从 embed 读取默认配置）
// func InitConfigByEmbed() *Config {
// 	var conf Config

// 	v := viper.New()
// 	v.SetConfigType("yaml")

// 	if err := v.ReadConfig(bytes.NewBuffer(configFile)); err != nil {
// 		log.Fatalf("read embedded config failed: %v", err)
// 	}

// 	if err := v.Unmarshal(&conf); err != nil {
// 		log.Fatalf("unmarshal config failed: %v", err)
// 	}

// 	return &conf
// }
