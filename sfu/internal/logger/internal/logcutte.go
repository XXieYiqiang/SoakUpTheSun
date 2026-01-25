package internal

import (
	"io"
	"os"
	"path/filepath"
	"sync"
	"time"
)

var _ io.Writer = (*LogCutter)(nil)

type LogCutter struct {
	level        string        // 日志级别(debug, info, warn, error, dpanic, panic, fatal)
	layout       string        // 时间格式 2006-01-02 15:04:05
	formats      []string      // 自定义参数([]string{Director,"2006-01-02", "business"(此参数可不写), level+".log"}
	director     string        // 日志文件夹
	retentionDay int           //日志保留天数
	file         *os.File      // 文件句柄
	mutex        *sync.RWMutex // 读写锁
}

type LogCutterOption func(*LogCutter)

// LogCutterWithLayout 时间格式
func LogCutterWithLayout(layout string) LogCutterOption {
	return func(c *LogCutter) {
		c.layout = layout
	}
}

// LogCutterWithFormats 格式化参数
func LogCutterWithFormats(format ...string) LogCutterOption {
	return func(c *LogCutter) {
		if len(format) > 0 {
			c.formats = format
		}
	}
}

func NewLogCutter(director string, level string, retentionDay int, options ...LogCutterOption) *LogCutter {
	mut := new(sync.RWMutex)
	c := &LogCutter{
		level:        level,
		mutex:        mut,
		director:     director,
		retentionDay: retentionDay,
	}
	for _, option := range options {
		option(c)
	}
	return c
}

// Sync implements zapcore.WriteSyncer.
func (l *LogCutter) Sync() error {
	l.mutex.Lock()
	defer l.mutex.Unlock()
	if l.file != nil {
		return l.file.Sync()
	}
	return nil
}

// Write implements io.Writer.
func (l *LogCutter) Write(p []byte) (n int, err error) {
	l.mutex.Lock()
	defer func() {
		if l.file != nil {
			_ = l.file.Close()
			l.file = nil
		}
		l.mutex.Unlock()
	}()
	length := len(l.formats)
	values := make([]string, 0, 3+length)
	values = append(values, l.director)
	if l.layout != "" {
		values = append(values, time.Now().Format(l.layout))
	}
	for i := range length {
		values = append(values, l.formats[i])
	}
	values = append(values, l.level+".log")
	filename := filepath.Join(values...)
	director := filepath.Dir(filename)
	err = os.MkdirAll(director, os.ModePerm)
	if err != nil {
		return 0, err
	}
	err = removeNDaysFolders(l.director, l.retentionDay)
	if err != nil {
		return 0, err
	}
	l.file, err = os.OpenFile(filename, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0644)
	if err != nil {
		return 0, err
	}
	return l.file.Write(p)
}

// 增加日志目录文件清理 小于等于零的值默认忽略不再处理
func removeNDaysFolders(dir string, days int) error {
	if days <= 0 {
		return nil
	}
	cutoff := time.Now().AddDate(0, 0, -days)
	return filepath.Walk(dir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() && info.ModTime().Before(cutoff) && path != dir {
			err = os.RemoveAll(path)
			if err != nil {
				return err
			}
		}
		return nil
	})
}
