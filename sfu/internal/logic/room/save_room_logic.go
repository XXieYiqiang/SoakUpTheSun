package logic

import (
	"context"
	"errors"
	"fmt"
	"sfu/internal/app"
	"sfu/internal/cache"
	"sfu/internal/logger"
	"sfu/internal/model"
	"sfu/internal/types"
	"sfu/internal/ws"
	"strings"
	"time"

	"github.com/bytedance/sonic"
	"github.com/google/uuid"
	"github.com/redis/go-redis/v9"
	"go.uber.org/zap"
	"gorm.io/gorm"
)

// RoomLogic 房间逻辑层
type SaveRoomLogic struct {
	ctx context.Context
	db  *gorm.DB
	rds *redis.Client
}

func NewSaveRoomLogic(ctx context.Context, app *app.App) *SaveRoomLogic {
	return &SaveRoomLogic{
		ctx: ctx,
		db:  app.DB,
		rds: app.Redis,
	}
}

// SaveRoom 创建房间
func (r *SaveRoomLogic) SaveRoom(userToken string) (*types.SaveRoomResp, error) {
	userInfo, err := r.getUserInfoByToken(r.ctx, userToken)
	if err != nil {
		logger.Log.Error("获取用户信息失败", zap.Error(err))
		return nil, errors.New("获取用户信息失败")
	}
	room := ws.NewRoom()
	if room == nil {
		logger.Log.Error("创建房间失败")
		return nil, errors.New("创建房间失败")
	}
	// 生成房间令牌
	roomToken, err := r.setRoomToken(r.ctx, room.ID, userInfo.ID)
	if err != nil {
		ws.DeleteRoom(room.ID)
		logger.Log.Error("生成房间令牌失败", zap.Error(err))
		return nil, errors.New("创建房间失败")
	}
	// 创建房间
	if err := r.db.Create(&model.Room{
		Name:        fmt.Sprintf("%s的房间", userInfo.Username),
		PatientID:   int64(userInfo.ID),
		PatientName: userInfo.Username,
		UID:         room.ID,
	}).Error; err != nil {
		ws.DeleteRoom(room.ID)
		logger.Log.Error("创建房间失败", zap.Error(err))
		return nil, errors.New("创建房间失败")
	}

	return &types.SaveRoomResp{
		RoomID: room.ID,
		Token:  roomToken,
	}, nil
}

// 设置房间令牌至redis
func (s *SaveRoomLogic) setRoomToken(ctx context.Context, roomUID string, userID uint64) (string, error) {

	token := strings.ReplaceAll(uuid.New().String(), "-", "")

	preload := types.RoomPreload{
		RoomID: roomUID,
		UserID: int64(userID),
		Role:   "patient",
	}

	val, err := sonic.MarshalString(preload)
	if err != nil {
		return "", fmt.Errorf("序列化房间信息失败: %w", err)
	}

	key := cache.ROOM_TOKEN_KEY + token

	if err := s.rds.SetNX(ctx, key, val, 10*time.Minute).Err(); err != nil {
		return "", fmt.Errorf("设置房间令牌失败: %w", err)
	}

	return token, nil
}

// 根据token令牌从redis获取用户信息
func (s *SaveRoomLogic) getUserInfoByToken(ctx context.Context, token string) (*types.CacheUserInfo, error) {
	tokenToUserCacheKey := cache.USER_LOGIN_KEY_TOKEN_TO_USER + token
	userStr, err := s.rds.Get(ctx, tokenToUserCacheKey).Result()
	if err != nil {
		return nil, fmt.Errorf("获取用户信息失败: %w", err)
	}
	userToTokenCacheKey := cache.USER_LOGIN_KEY_USER_TO_TOKEN + userStr
	userTokenStr, err := s.rds.HGet(ctx, userToTokenCacheKey, token).Result()
	if err != nil {
		return nil, fmt.Errorf("获取用户令牌失败: %w", err)
	}
	if err := s.rds.Expire(ctx, userToTokenCacheKey, 15*time.Minute).Err(); err != nil {
		return nil, fmt.Errorf("设置用户令牌过期时间失败: %w", err)
	}

	var user types.CacheUserInfo
	if err := sonic.UnmarshalString(userTokenStr, &user); err != nil {
		return nil, fmt.Errorf("获取用户信息失败,反序列化失败: %w", err)
	}
	return &user, nil
}
