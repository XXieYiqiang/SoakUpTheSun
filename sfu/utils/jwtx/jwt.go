package jwtx

import (
	"errors"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

type Service[T any] struct {
	secret string // 密钥
	expire int64  // 秒
	issuer string // 签发人
}

type Claims[T any] struct {
	Info T // 自定义信息
	jwt.RegisteredClaims
}

func NewJwt[T any](secret string, expire int64, issuer string) *Service[T] {
	return &Service[T]{
		secret: secret,
		expire: expire,
		issuer: issuer,
	}
}

// GenerateToken 生成token
func (s *Service[T]) GenerateToken(info T) (string, error) {
	claims := Claims[T]{
		Info: info,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(time.Duration(s.expire) * time.Second)),
			Issuer:    s.issuer,
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(s.secret))
}

// ParseToken 解析token
func (s *Service[T]) ParseToken(tokenString string) (*Claims[T], error) {
	token, err := jwt.ParseWithClaims(tokenString, &Claims[T]{}, func(token *jwt.Token) (any, error) {
		return []byte(s.secret), nil
	})
	if err != nil {
		return nil, err
	}
	claims, ok := token.Claims.(*Claims[T])
	if ok && token.Valid {
		issuer, err := claims.GetIssuer()
		if err != nil {
			return nil, err
		}
		if issuer != s.issuer {
			return nil, errors.New("invalid issuer")
		}
		return claims, nil
	}
	return nil, errors.New("invalid token")
}
