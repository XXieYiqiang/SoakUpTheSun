package encrypt

import (
	"fmt"

	"github.com/btcsuite/btcd/btcutil/base58"
	"github.com/google/uuid"
)

// UUIDToBase58  UUID 转换为 Base58 编码的字符串。
func UUIDToBase58(u uuid.UUID) string {
	data := u[:]
	return base58.Encode(data)
}

// Base58ToUUID 将 Base58 编码的字符串解码回 uuid.UUID
func Base58ToUUID(s string) (uuid.UUID, error) {
	data := base58.Decode(s)

	if len(data) != 16 {
		return uuid.Nil, fmt.Errorf("解码后的数据长度错误: 期望 16 字节, 实际 %d 字节", len(data))
	}
	var u uuid.UUID
	copy(u[:], data)
	return u, nil
}
