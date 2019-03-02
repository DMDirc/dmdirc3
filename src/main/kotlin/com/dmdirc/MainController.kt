package com.dmdirc

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.application.HostServices

object MainContract {
    interface Controller {
        val windows: ObservableList<WindowModel>
        val selectedWindow: ObjectProperty<WindowModel>
        fun connect(connectionDetails: ConnectionDetails)
        fun joinChannel(channel: String)
        fun leaveChannel(channel: String)
    }
}

class MainController(private val config1: ClientConfig, private val hostServices: HostServices) : MainContract.Controller {

    override val windows: ObservableList<WindowModel> = emptyList<WindowModel>().toMutableList().observable()
    override val selectedWindow: SimpleObjectProperty<WindowModel> = SimpleObjectProperty()

    init {
        autoConnect()
    }

    private fun autoConnect() {
        config1[ClientSpec.servers].filter { it.autoconnect }.forEach(this::connect)
    }

    override fun connect(connectionDetails: ConnectionDetails) {
        Connection(connectionDetails.hostname,  connectionDetails.port, connectionDetails.password, connectionDetails.tls, config1, this, hostServices).connect()
    }

    override fun joinChannel(channel: String) {
        selectedWindow.value.connection?.joinChannel(channel)
    }

    override fun leaveChannel(channel: String) {
        selectedWindow.value.connection?.leaveChannel(channel)
    }

}