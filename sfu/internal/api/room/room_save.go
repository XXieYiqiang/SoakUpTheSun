package room

import (
	"context"
	"fmt"
	"sfu/internal/cache"
	"sfu/internal/logger"
	"sfu/internal/model"
	"sfu/internal/model/res"
	"sfu/internal/ws"
	"strings"
	"time"

	"github.com/bytedance/sonic"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"go.uber.org/zap"
)

// CacheUserInfo 缓存的用户信息
type CacheUserInfo struct {
	ID          uint64 `json:"id"`          // 用户ID
	UserAccount string `json:"userAccount"` // 用户账号
	UserAvatar  string `json:"userAvatar"`  // 用户头像
	Username    string `json:"userName"`    // 用户名
	UserProfile string `json:"userProfile"` // 用户个人介绍
	UserRole    string `json:"userRole"`    // 用户角色
	CreateTime  string `json:"createTime"`  // 创建时间
	EditTime    string `json:"editTime"`    // 最后编辑时间
	UpdateTime  string `json:"updateTime"`  // 最后更新时间
}

// RoomPreload 房间预加载信息
type RoomPreload struct {
	RoomID string `json:"roomID"`
	UserID int64  `json:"userID"`
	Role   string `json:"role"`
}

type SaveRoomResponse struct {
	RoomID string `json:"roomID"`
	Token  string `json:"token"`
}

// SaveRoom 创建房间
func (r RoomApi) SaveRoom(c *gin.Context) {
	// 获取用户信息
	userToken := c.GetHeader("token")
	if userToken == "" {
		res.Failed(c, "请先登录")
		return
	}
	userInfo, err := r.getUserInfoByToken(c.Request.Context(), userToken)
	if err != nil {
		res.Failed(c, "获取用户信息失败")
		logger.Log.Error("获取用户信息失败", zap.Error(err))
		return
	}

	room := ws.NewRoom()
	if room == nil {
		res.Failed(c, "创建房间失败")
		logger.Log.Error("创建房间失败")
		return
	}

	// 生成房间令牌
	roomToken, err := r.setRoomToken(c.Request.Context(), room.ID, userInfo.ID)
	if err != nil {
		ws.DeleteRoom(room.ID)
		res.Failed(c, "创建房间失败")
		logger.Log.Error("生成房间令牌失败", zap.Error(err))
		return
	}

	// 创建房间
	if err := r.db.Create(&model.Room{
		Name:        fmt.Sprintf("%s的房间", userInfo.Username),
		PatientID:   int64(userInfo.ID),
		PatientName: userInfo.Username,
		UID:         room.ID,
	}).Error; err != nil {
		res.Failed(c, "创建房间失败")
		ws.DeleteRoom(room.ID)
		logger.Log.Error("创建房间失败", zap.Error(err))
		return
	}

	res.OkWithData(c, &SaveRoomResponse{
		RoomID: room.ID,
		Token:  roomToken,
	})
}

// setRoomToken 设置房间令牌至redis
func (r *RoomApi) setRoomToken(ctx context.Context, roomUID string, userID uint64) (string, error) {

	token := strings.ReplaceAll(uuid.New().String(), "-", "")

	preload := RoomPreload{
		RoomID: roomUID,
		UserID: int64(userID),
		Role:   "patient",
	}

	val, err := sonic.MarshalString(preload)
	if err != nil {
		return "", fmt.Errorf("序列化房间信息失败: %w", err)
	}

	key := cache.ROOM_TOKEN_KEY + token

	if err := r.redis.SetNX(ctx, key, val, 10*time.Minute).Err(); err != nil {
		return "", fmt.Errorf("设置房间令牌失败: %w", err)
	}

	return token, nil
}

// 根据token令牌从redis获取用户信息
func (r *RoomApi) getUserInfoByToken(ctx context.Context, token string) (*CacheUserInfo, error) {
	tokenToUserCacheKey := cache.USER_LOGIN_KEY_TOKEN_TO_USER + token
	userStr, err := r.redis.Get(ctx, tokenToUserCacheKey).Result()
	if err != nil {
		return nil, fmt.Errorf("获取用户信息失败: %w", err)
	}
	userToTokenCacheKey := cache.USER_LOGIN_KEY_USER_TO_TOKEN + userStr
	userTokenStr, err := r.redis.HGet(ctx, userToTokenCacheKey, token).Result()
	if err != nil {
		return nil, fmt.Errorf("获取用户令牌失败: %w", err)
	}
	if err := r.redis.Expire(ctx, userToTokenCacheKey, 15*time.Minute).Err(); err != nil {
		return nil, fmt.Errorf("设置用户令牌过期时间失败: %w", err)
	}

	var user CacheUserInfo
	if err := sonic.UnmarshalString(userTokenStr, &user); err != nil {
		return nil, fmt.Errorf("获取用户信息失败,反序列化失败: %w", err)
	}
	return &user, nil
}
