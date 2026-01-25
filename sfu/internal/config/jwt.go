package config

type JwtConfig struct {
	Expire int64  `yaml:"expire" mapstructure:"expire" json:"expire"`
	Secret string `yaml:"secret" mapstructure:"secret" json:"secret"`
	Issuer string `yaml:"issuer" mapstructure:"issuer" json:"issuer"`
}
