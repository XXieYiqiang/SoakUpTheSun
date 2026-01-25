package config

type SystemConfig struct {
	Port    int    `json:"port" yaml:"port" mapstructure:"port"`
	Env     string `json:"env" yaml:"env" mapstructure:"env"`
	Migrate bool   `json:"migrate" yaml:"migrate" mapstructure:"migrate"`
}
