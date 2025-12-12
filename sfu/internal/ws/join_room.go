package ws

import "github.com/gin-gonic/gin"

func (u UserRoom) JoinRoom(c *gin.Context) {
	roomID := c.Query("roomID")
	if roomID == "" {

	}
}
