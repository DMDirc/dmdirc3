package com.dmdirc

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import org.kodein.di.generic.instance
import tornadofx.Controller
import tornadofx.observable

enum class WindowType {
    ROOT,
    SERVER,
    CHANNEL
}

data class Window(
    val name: String,
    val type: WindowType,
    var windowUI: WindowUI,
    var connection: Connection?)

class MainController : Controller() {

    private val config1 by kodein.instance<ClientConfig>()
    val windows: ObservableList<Window> = emptyList<Window>().toMutableList().observable()

    fun connect(host: String, port: Int, password: String, tls: Boolean) {
        Connection(host,  port, password, tls, config1, this).connect()
    }

    fun joinChannel(value: String) {
        selectedChannel.value.connection?.joinChannel(value) ?: return
    }

    val selectedChannel: SimpleObjectProperty<Window> = SimpleObjectProperty()
}