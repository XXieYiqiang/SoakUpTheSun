package main

import (
	"log"
	"sfu/internal/app"
	"sfu/internal/router"
)

func main() {
	app := app.NewApp()
	router := router.NewRouter(app)

	if err := router.Run(":8080"); err != nil {
		log.Fatal(err)
	}
}
