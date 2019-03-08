package com.dmdirc

import javafx.scene.Scene
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService

fun installStyles(root: Scene, file: Path) {
    file.checkAndInstall(root)
    val directory = file.toAbsolutePath().parent
    directory.watchFile(file) {
        file.checkAndInstall(root)
    }
}

private fun Path.checkAndInstall(root: Scene) {
    runLater {
        root.stylesheets.clear()
        root.stylesheets.add(MainApp::class.java.getResource("/stylesheet.css").toExternalForm())
        if (Files.exists(this)) {
            root.stylesheets.add(this.toAbsolutePath().toUri().toURL().toExternalForm())
        }
    }
}

private fun Path.watchFile(file: Path, action: () -> Unit) {
    if (!Files.isDirectory(this)) return
    watch().takeRepeat {
        val context = it.context()
        if (context is Path) {
            if (context == file) {
                action.invoke()
            }
        }
    }
}

private fun WatchService.takeRepeat(process: (WatchEvent<*>) -> Unit) {
    while (true) {
        take()?.let {
            for (event in it.pollEvents()) {
                process(event)
            }
            it.reset()
        } ?: break
    }
}

private fun Path.watch(): WatchService {
    val watchService = this.fileSystem.newWatchService()
    register(
        watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.OVERFLOW,
        StandardWatchEventKinds.ENTRY_DELETE
    )
    return watchService
}
