package com.dmdirc

import javafx.stage.Stage
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import tornadofx.App
import tornadofx.launch
import tornadofx.runAsync
import java.nio.file.Paths
import java.util.logging.Level
import java.util.logging.LogManager



internal var kodein = Kodein {
    bind<ClientConfig>() with singleton { ClientConfig.loadFrom(Paths.get("config.yml")) }
}

class MainApp : App(MainView::class) {
    override fun start(stage: Stage) {
        val rootLogger = LogManager.getLogManager().getLogger("")
        rootLogger.level = Level.OFF
        for (h in rootLogger.handlers) {
            h.level = Level.OFF
        }
        with(stage) {
            minWidth = 800.0
            minHeight = 600.0
            super.start(this)
        }

    }
    init {
        runAsync {
            installStyles(Paths.get("stylesheet.css"))
        }
    }
}

fun main(args: Array<String>) {
    launch<MainApp>(args)
}