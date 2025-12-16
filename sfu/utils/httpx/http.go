package httpx

import (
	"bytes"
	"io"
	"net/http"
	"net/url"
	"time"

	"github.com/bytedance/sonic"
)

type HTTPClient struct {
	Client *http.Client
}

func NewClient() *HTTPClient {
	return &HTTPClient{
		Client: &http.Client{
			Timeout: 15 * time.Second,
		},
	}
}

type Result[T any] struct {
	StatusCode int
	Header     http.Header
	Data       T
	RawBody    []byte
}

// Get 发送 GET 请求并自动将响应体反序列化到 T 类型
// T: 响应体类型
func Get[T any](urlStr string, params map[string]string) (*Result[T], error) {
	reqURL, err := url.Parse(urlStr)
	if err != nil {
		return nil, err
	}

	query := reqURL.Query()
	for key, value := range params {
		query.Add(key, value)
	}
	reqURL.RawQuery = query.Encode()

	req, err := http.NewRequest("GET", reqURL.String(), nil)
	if err != nil {
		return nil, err
	}

	return doRequest[T](req)
}

// PostBody 发送 Content-Type 为 application/json 的 POST 请求
// T: 响应体类型
// bodyData: 将会被 JSON 序列化的任意结构体或 map
func PostBody[T any](urlStr string, bodyData any) (*Result[T], error) {
	bodyBytes, err := sonic.Marshal(bodyData)
	if err != nil {
		return nil, err
	}

	req, err := http.NewRequest(
		"POST",
		urlStr,
		bytes.NewBuffer(bodyBytes),
	)
	if err != nil {
		return nil, err
	}

	req.Header.Set("Content-Type", "application/json")

	return doRequest[T](req)
}

// PostForm 发送 application/x-www-form-urlencoded 的 POST 请求
// T: 响应体类型
func PostForm[T any](urlStr string, formData map[string]string) (*Result[T], error) {
	data := url.Values{}
	for key, value := range formData {
		data.Add(key, value)
	}

	req, err := http.NewRequest(
		"POST",
		urlStr,
		bytes.NewBufferString(data.Encode()),
	)
	if err != nil {
		return nil, err
	}

	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	return doRequest[T](req)
}

// doRequest 是核心执行方法，处理请求和响应体的反序列化
// T 响应体类型
func doRequest[T any](req *http.Request) (*Result[T], error) {
	c := NewClient()
	resp, err := c.Client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	// 1. 读取原始响应体
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	var data T
	// 只在状态码是 2xx 的时候尝试解析 JSON
	if resp.StatusCode >= 200 && resp.StatusCode < 300 {
		// 注意: T 必须是可导出的结构体指针或支持 JSON 解析的类型
		if err := sonic.Unmarshal(body, &data); err != nil {
			// 如果解析失败，可能是非 JSON 响应或 T 类型不匹配，返回原始 Body 但解析失败的错误
			return &Result[T]{
				StatusCode: resp.StatusCode,
				Header:     resp.Header,
				RawBody:    body,
			}, err
		}
	}

	return &Result[T]{
		StatusCode: resp.StatusCode,
		Header:     resp.Header,
		Data:       data,
		RawBody:    body,
	}, nil
}
