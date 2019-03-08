package com.dmdirc

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.SetChangeListener

object MainContract {
    interface Controller {
        val windows: ObservableList<WindowModel>
        val selectedWindow: ObjectProperty<WindowModel>
        fun connect(connectionDetails: ConnectionDetails)
        fun joinChannel(channel: String)
        fun leaveChannel(channel: String)
        fun joinDev()
    }
}

class MainController(
    private val config1: ClientConfig,
    private val connectionFactory: (ConnectionDetails) -> ConnectionContract.Controller
) : MainContract.Controller {
    override fun joinDev() {
        connect(
            ConnectionDetails(
                hostname = "chat.freenode.net",
                port = 6667,
                tls = false,
                autoJoin = listOf("#DMDirc")
            )
        )
    }

    override val windows: ObservableList<WindowModel> = FXCollections.observableArrayList(WindowModel.extractor())
    override val selectedWindow = SimpleObjectProperty<WindowModel>()

    init {
        autoConnect()
    }

    private fun autoConnect() {
        config1[ClientSpec.servers].filter { it.autoconnect }.forEach(this::connect)
    }

    override fun connect(connectionDetails: ConnectionDetails) {
        with(connectionFactory(connectionDetails)) {
            windows.addAll(children.map { it.model })
            children.observable.addListener(SetChangeListener<Connection.Child> {
                runLater {
                    when {
                        it.wasAdded() -> windows.add(it.elementAdded.model)
                        it.wasRemoved() -> windows.remove(it.elementRemoved.model)
                    }
                }
            })
            connect()
        }
    }

    override fun joinChannel(channel: String) {
        selectedWindow.value.connection?.joinChannel(channel)
    }

    override fun leaveChannel(channel: String) {
        selectedWindow.value.connection?.leaveChannel(channel)
    }
}
