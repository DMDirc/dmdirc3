package com.dmdirc

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import tornadofx.Controller
import tornadofx.observable

enum class WindowType {
    ROOT,
    SERVER,
    CHANNEL
}

data class Window(
    val name: String,
    val children: ObservableList<Window>,
    val type: WindowType,
    var windowUI: WindowUI,
    var connection: Connection?)

class MainController : Controller() {

    fun sendMessage(ui: WindowUI, message: String) {
        val window = root.children.find {
            it.windowUI == ui
        } ?: return
        window.connection?.sendMessage(window.name, message)
    }

    fun connect(host: String, port: Int, password: String, tls: Boolean) {
        Connection(host,  port, password, tls, app.config, root).connect()
    }

    fun joinChannel(value: String) {
        selectedChannel.value.connection?.joinChannel(value)
    }

    internal val root: Window = Window(
        "Root",
        emptyList<Window>().toMutableList().observable(),
        WindowType.ROOT,
        WindowUI(null),
        null
    )
    val selectedChannel: SimpleObjectProperty<Window> = SimpleObjectProperty()
}