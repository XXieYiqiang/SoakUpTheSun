package res

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type Response[T any] struct {
	Code int    `json:"code"`
	Msg  string `json:"msg"`
	Data T      `json:"data"`
}

func message[T any](c *gin.Context, code int, msg string, data T) {
	c.JSON(code, Response[T]{
		Code: code,
		Msg:  msg,
		Data: data,
	})
}

func OK(c *gin.Context) {
	message(c, http.StatusOK, "success", map[string]any{})
}

func OkWithMsg(c *gin.Context, msg string) {
	message(c, http.StatusOK, msg, map[string]any{})
}

func OkWithData[T any](c *gin.Context, data T) {
	message(c, http.StatusOK, "success", data)
}

func Failed(c *gin.Context, msg string) {
	message(c, http.StatusBadRequest, msg, map[string]any{})
}

func FailedWithData[T any](c *gin.Context, msg string, data T) {
	message(c, http.StatusBadRequest, msg, data)
}
