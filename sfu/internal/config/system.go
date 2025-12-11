package config

type SystemConfig struct {
	Port    int    `json:"port" yaml:"port"`
	Env     string `json:"env" yaml:"env"`
	Migrate bool   `json:"migrate" yaml:"migrate"`
}
