package middleware

import (
	"net"
	"net/http"
	"net/http/httputil"
	"os"
	"runtime/debug"
	"sfu/internal/logger"
	"strings"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

func RecoveryMiddleware(stack bool) gin.HandlerFunc {
	return func(c *gin.Context) {
		defer func() {
			if err := recover(); err != nil {
				//判断是否是“管道破裂/连接重置”这类网络异常
				var brokenPipe bool
				if ne, ok := err.(*net.OpError); ok { // 类型断言为网络操作错误
					if se, ok := ne.Err.(*os.SyscallError); ok { // 进一步断言为系统调用错误
						// 检查错误信息是否包含“broken pipe”或“connection reset by peer”
						if strings.Contains(strings.ToLower(se.Error()), "broken pipe") || strings.Contains(strings.ToLower(se.Error()), "connection reset by peer") {
							brokenPipe = true
						}
					}
				}
				// Dump 请求信息（URL、方法、头部等，不含请求体）
				httpRequest, _ := httputil.DumpRequest(c.Request, false)

				if brokenPipe {
					// 记录错误日志（URL路径 + 错误信息 + 请求详情）
					logger.Log.Error(c.Request.URL.Path,
						zap.Any("error", err),
						zap.String("request", string(httpRequest)),
					)
					// 客户端已断开，无法返回状态码，仅记录错误并终止请求
					_ = c.Error(err.(error))
					c.Abort()
					return
				}
				if stack { // 如果传入stack=true，记录堆栈信息
					logger.Log.Error("[Recovery from panic]",
						zap.Any("error", err),
						zap.String("request", string(httpRequest)),
						zap.String("stack", string(debug.Stack())), // 打印调用堆栈
					)
				} else { // 不记录堆栈，仅记录基础信息
					logger.Log.Error("[Recovery from panic]",
						zap.Any("error", err),
						zap.String("request", string(httpRequest)),
					)
				}
				// 返回500内部服务器错误
				c.AbortWithStatus(http.StatusInternalServerError)
			}
		}()
		c.Next()
	}
}
