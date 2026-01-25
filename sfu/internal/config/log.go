package config

import (
	"time"

	"go.uber.org/zap/zapcore"
)

type LogConfig struct {
	Director     string `mapstructure:"director" json:"director" yaml:"director"`                   // 日志目录
	Prefix       string `mapstructure:"prefix" json:"prefix" yaml:"prefix"`                         // 日志前缀
	RetentionDay int    `mapstructure:"retention_day" json:"retention_day" yaml:"retention_day"`    // 日志保留天数
	LevelEncode  string `mapstructure:"level_encode" json:"level_encode" yaml:"level_encode"`       // 日志级别
	Format       string `mapstructure:"format" json:"format" yaml:"format"`                         // 日志输出格式
	Level        string `mapstructure:"levels" json:"levels" yaml:"levels"`                         // 日志级别
	LogInConsole bool   `mapstructure:"log_in_console" json:"log_in_console" yaml:"log_in_console"` // 是否在控制台输出日志
	ShowLine     bool   `mapstructure:"show_line" json:"show_line" yaml:"show_line"`                // 是否显示行号
}

// Levels 根据字符串转化为 zapcore.Levels
func (c *LogConfig) Levels() []zapcore.Level {
	levels := make([]zapcore.Level, 0, 7)
	level, err := zapcore.ParseLevel(c.Level)
	if err != nil {
		level = zapcore.DebugLevel
	}
	for ; level <= zapcore.FatalLevel; level++ {
		levels = append(levels, level)
	}
	return levels
}

func (l *LogConfig) Encoder() zapcore.Encoder {
	config := zapcore.EncoderConfig{
		TimeKey:       "time",
		NameKey:       "name",
		LevelKey:      "level",
		CallerKey:     "caller",
		MessageKey:    "message",
		StacktraceKey: "stacktrace",
		LineEnding:    zapcore.DefaultLineEnding,
		EncodeTime: func(t time.Time, encoder zapcore.PrimitiveArrayEncoder) {
			encoder.AppendString(l.Prefix + t.Format("2006-01-02 15:04:05.000"))
		},
		EncodeLevel:    l.LevelEncoder(),
		EncodeCaller:   zapcore.FullCallerEncoder,
		EncodeDuration: zapcore.SecondsDurationEncoder,
	}
	if l.Format == "json" {
		return zapcore.NewJSONEncoder(config)
	}
	return zapcore.NewConsoleEncoder(config)
}

func (l *LogConfig) LevelEncoder() zapcore.LevelEncoder {
	switch l.LevelEncode {
	case "LowercaseLevelEncoder": // 小写编码器(默认)
		return zapcore.LowercaseLevelEncoder
	case "LowercaseColorLevelEncoder": // 小写编码器带颜色
		return zapcore.LowercaseColorLevelEncoder
	case "CapitalLevelEncoder": // 大写编码器
		return zapcore.CapitalLevelEncoder
	case "CapitalColorLevelEncoder": // 大写编码器带颜色
		return zapcore.CapitalColorLevelEncoder
	default:
		return zapcore.LowercaseLevelEncoder
	}
}
