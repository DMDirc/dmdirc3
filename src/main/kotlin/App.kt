package com.dmdirc

import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import java.util.logging.Level
import java.util.logging.LogManager

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
}

fun main(args: Array<String>) {
    launch<MainApp>(args)
}

fun randomString(length: Int = 16): String {
    val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..length)
            .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
}