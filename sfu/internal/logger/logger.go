package logger

import (
	"fmt"
	"log"
	"os"
	"sfu/internal/config"
	"sfu/internal/logger/internal"
	"sfu/utils/dir"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

var Log *zap.Logger

// 默认日志目录
const logDefaultPath = "log"

func InitZapLog(logConf *config.Log) {
	if logConf == nil {
		log.Fatalf("log config is nil")
	}
	if logConf.Director == "" {
		logConf.Director = logDefaultPath
	}
	if ok, _ := dir.PathExists(logConf.Director); !ok { // 判断是否有Director文件夹
		fmt.Printf("创建 %v 文件夹成功\n", logConf.Director)
		_ = os.Mkdir(logConf.Director, os.ModePerm)
	}
	levels := logConf.Levels()
	length := len(levels)
	cores := make([]zapcore.Core, 0, length)
	for i := range length {
		core := internal.NewZapCore(logConf, levels[i])
		cores = append(cores, core)
	}
	logger := zap.New(zapcore.NewTee(cores...))
	opts := []zap.Option{zap.AddStacktrace(zapcore.ErrorLevel)}
	if logConf.ShowLine {
		opts = append(opts, zap.AddCaller())
	}
	logger = logger.WithOptions(opts...)
	Log = logger
}
