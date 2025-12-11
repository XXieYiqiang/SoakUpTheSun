package internal

import (
	"os"
	"sfu/internal/config"
	"time"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

type ZapCore struct {
	level   zapcore.Level
	logConf *config.LogConfig
	zapcore.Core
}

func NewZapCore(logConf *config.LogConfig, level zapcore.Level) *ZapCore {
	core := &ZapCore{
		level:   level,
		logConf: logConf,
	}
	writeSyncer := core.Syncer()
	levelEnabler := zap.LevelEnablerFunc(func(l zapcore.Level) bool {
		return l == level
	})
	core.Core = zapcore.NewCore(logConf.Encoder(), writeSyncer, levelEnabler)
	return core
}

func (z *ZapCore) Syncer(formats ...string) zapcore.WriteSyncer {
	cutter := NewLogCutter(
		z.logConf.Director,
		z.level.String(),
		z.logConf.RetentionDay,
		LogCutterWithFormats(formats...),
		LogCutterWithLayout(time.DateOnly),
	)
	if z.logConf.LogInConsole {
		multiSyncer := zapcore.NewMultiWriteSyncer(os.Stdout, cutter)
		return zapcore.AddSync(multiSyncer)
	}
	return zapcore.AddSync(cutter)
}
