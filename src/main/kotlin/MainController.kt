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
    var connection: Connection?,
    var isConnection: Boolean
) {
    val path = if (isConnection) {
        connection?.serverName?.toLowerCase() ?: ""
    } else {
        "${connection?.serverName?.toLowerCase() ?: ""} ${name.toLowerCase()}"
    }
}

class MainController : Controller() {

    private val config1 by kodein.instance<ClientConfig>()
    val windows: ObservableList<Window> = emptyList<Window>().toMutableList().observable()
    val selectedWindow: SimpleObjectProperty<Window> = SimpleObjectProperty()

    init {
        autoConnect()
    }

    private fun autoConnect() {
        config1[ClientSpec.servers].filter { it.autoconnect }.forEach(this::connect)
    }

    fun connect(connectionDetails: ConnectionDetails) {
        Connection(connectionDetails.hostname,  connectionDetails.port, connectionDetails.password, connectionDetails.tls, config1, this).connect()
    }

    fun joinChannel(value: String) {
        selectedWindow.value.connection?.joinChannel(value) ?: return
    }

    fun leaveChannel(name: String) {
        selectedWindow.value.connection?.leaveChannel(name) ?: return
    }

}