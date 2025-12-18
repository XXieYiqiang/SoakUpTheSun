package database

import (
	"fmt"
	"log"
	"sfu/internal/config"
	"sfu/internal/model"

	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

func InitMysql(conf *config.Config) *gorm.DB {
	dsn := fmt.Sprintf("%s:%s@tcp(%s:%d)/%s?charset=utf8mb4&parseTime=True&loc=Local",
		conf.Mysql.Username,
		conf.Mysql.Password,
		conf.Mysql.Host,
		conf.Mysql.Port,
		conf.Mysql.Database,
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
	_db.SetMaxIdleConns(conf.Mysql.MaxIdleConns)
	_db.SetMaxOpenConns(conf.Mysql.MaxOpenConns)
	// 自动迁移
	if conf.System.Migrate {
		migrateTable(db)
	}
	return db
}

func migrateTable(db *gorm.DB) {
	err := db.AutoMigrate(
		&model.Room{},
		&model.RoomMember{},
	)
	if err != nil {
		log.Fatalf("migrate table failed: %v", err)
	}
}
