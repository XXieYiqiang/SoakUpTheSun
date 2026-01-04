package types

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

// SaveRoomResp 创建房间响应
type SaveRoomResp struct {
	RoomID string `json:"roomID"`
	Token  string `json:"token"`
}

// GetRoomInfoReq 获取房间信息请求
type GetRoomInfoReq struct {
	RoomID string `uri:"roomID"`
}

// GetRoomInfoResp 获取房间信息响应
type GetRoomInfoResp struct {
	RoomID   string `json:"roomID"`   // 房间号
	RoomName string `json:"roomName"` // 房间名称
	Status   string `json:"status"`   // 房间状态
	Num      int    `json:"num"`      // 房间人数
}

// CloseRoomReq 关闭房间请求
type CloseRoomReq struct {
	RoomID string `json:"roomID"`
}
