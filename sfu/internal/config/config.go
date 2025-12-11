package config

import (
	"log"

	"github.com/spf13/viper"
)

type Config struct {
	Log    LogConfig    `mapstructure:"log" json:"log" yaml:"log"`
	System SystemConfig `mapstructure:"system" json:"system" yaml:"system"`
	Mysql  MysqlConfig  `mapstructure:"mysql" json:"mysql" yaml:"mysql"`
}

// InitConfig 初始化配置文件
func InitConfig() *Config {
	var conf *Config
	viper.SetConfigName("config")
	viper.SetConfigType("yaml")
	viper.AddConfigPath(".")
	if err := viper.ReadInConfig(); err != nil {
		log.Fatalf("read config file failed: %v", err)
	}
	if err := viper.Unmarshal(&conf); err != nil {
		log.Fatalf("unmarshal config file failed: %v", err)
	}
	return conf
}
