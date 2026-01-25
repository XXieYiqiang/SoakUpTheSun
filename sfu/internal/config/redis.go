package config

type RedisConfig struct {
	Addr     string `json:"addr" yaml:"addr" mapstructure:"addr"`
	Password string `json:"password" yaml:"password" mapstructure:"password"`
	DB       int    `json:"db" yaml:"db" mapstructure:"db"`
}
