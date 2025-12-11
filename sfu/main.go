package main

import (
	"fmt"
	"log"
	"sfu/internal/app"
	"sfu/internal/router"
)

func main() {
	app := app.NewApp()
	router := router.NewRouter(app)

	if err := router.Run(fmt.Sprintf(":%d", app.Config.System.Port)); err != nil {
		log.Fatal(err)
	}
}
