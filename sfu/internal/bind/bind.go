package bind

import "github.com/gin-gonic/gin"

// BindJson 绑定 JSON 请求体到结构体
func BindJson[T any](c *gin.Context) (*T, error) {
	req := new(T)
	if err := c.ShouldBindJSON(&req); err != nil {
		return nil, err
	}
	return req, nil
}

// BindQuery 绑定查询参数到结构体
func BindQuery[T any](c *gin.Context) (*T, error) {
	req := new(T)
	if err := c.ShouldBindQuery(&req); err != nil {
		return nil, err
	}
	return req, nil
}

// BindUri 绑定 URI 参数到结构体
func BindUri[T any](c *gin.Context) (*T, error) {
	req := new(T)
	if err := c.ShouldBindUri(&req); err != nil {
		return nil, err
	}
	return req, nil
}

// BindHeader 绑定请求头到结构体
func BindHeader[T any](c *gin.Context) (*T, error) {
	req := new(T)
	if err := c.ShouldBindHeader(&req); err != nil {
		return nil, err
	}
	return req, nil
}
