package snowflakex

import (
	"sync"

	"github.com/bwmarrin/snowflake"
)

var (
	node *snowflake.Node
	once sync.Once
)

func Init(nodeID int64) error {
	var err error
	once.Do(func() {
		node, err = snowflake.NewNode(nodeID)
	})
	return err
}

func Generate() snowflake.ID {
	if node == nil {
		panic("snowflake node not initialized")
	}
	return node.Generate()
}
