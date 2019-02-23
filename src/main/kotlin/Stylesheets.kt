package com.dmdirc

import com.sun.javafx.application.PlatformImpl
import java.nio.file.*


fun installStyles(file: Path) {
    file.checkAndInstall()
    val directory = file.toAbsolutePath().parent
    directory.watchFile(file) {
        PlatformImpl.setDefaultPlatformUserAgentStylesheet()
        file.checkAndInstall()
    }
}

private fun Path.checkAndInstall() {
    if (Files.exists(this)) {
        PlatformImpl.setPlatformUserAgentStylesheet(this.toAbsolutePath().toUri().toURL().toExternalForm())
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