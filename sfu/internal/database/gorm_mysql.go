package database

import (
	"fmt"
	"log"
	"sfu/internal/config"

	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

func InitMysql(conf *config.MysqlConfig) *gorm.DB {
	dsn := fmt.Sprintf("%s:%s@tcp(%s:%d)/%s?charset=utf8mb4&parseTime=True&loc=Local",
		conf.Username,
		conf.Password,
		conf.Host,
		conf.Port,
		conf.Database,
	)

	mysqlConfig := mysql.Config{
		DSN:                       dsn,
		SkipInitializeWithVersion: true,
	}
	db, err := gorm.Open(mysql.New(mysqlConfig))
	if err != nil {
		log.Fatalf("open mysql failed: %v", err)
	}
	db.InstanceSet("gorm:table_options", "ENGINE=InnoDB")
	_db, _ := db.DB()
	_db.SetMaxIdleConns(conf.MaxIdleConns)
	_db.SetMaxOpenConns(conf.MaxOpenConns)
	return db
}
