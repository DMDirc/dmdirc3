package com.dmdirc

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.messages.sendMessage
import com.dmdirc.ktirc.model.ServerFeature
import javafx.beans.property.SimpleBooleanProperty
import tornadofx.ConfigProperties
import tornadofx.runLater
import java.time.format.DateTimeFormatter

class Connection(
    private val host: String,
    private val port: Int,
    private val password: String?,
    private val tls: Boolean,
    private val config: ConfigProperties,
    private val controller: MainController
) {
    private val window: Window = Window(
        host,
        WindowType.SERVER,
        WindowUI(this),
        this
    )
    private val connected = SimpleBooleanProperty(false)
    var networkName = ""
    private val client: IrcClient = IrcClient {
        server(host, port, tls, password)
        profile {
            nickname = config.getProperty("nickname")
            realName = config.getProperty("realname")
            username = config.getProperty("username")
        }
    }

    fun connect() {
        client.onEvent(this::handleEvent)
        controller.windows.add(window)
        client.connect()
    }

    fun sendMessage(channel: String, name: String) {
        client.sendMessage(channel, name)
    }

    fun joinChannel(channel: String) {
        client.sendJoin(channel)
    }

    private fun handleEvent(event: IrcEvent) {
        when {
            event is BatchReceived -> event.events.forEach(this::handleEvent)
            event is ServerConnected -> runLater {
                window.windowUI.addLine("${event.timestamp} *** Connected")
                connected.value = true
            }
            event is ServerReady -> {
                networkName = client.serverState.features[ServerFeature.Network] ?: ""
            }
            event is ServerDisconnected -> runLater {
                window.windowUI.addLine("${event.timestamp} *** Disconnected")
                connected.value = false
            }
            event is ServerConnectionError -> runLater {
                window.windowUI.addLine("${event.timestamp} *** Error: ${event.error} ${event.details ?: ""}")
            }
            event is ChannelJoined && client.isLocalUser(event.user) -> runLater {
                controller.windows.add(
                    Window(
                        event.target,
                        WindowType.CHANNEL,
                        WindowUI(this),
                        this
                    )
                )
            }
            event is TargetedEvent -> runLaterWithWindowUi(event.target) { handleTargetedEvent(event) }
            else -> {
            }
        }
    }

    private fun WindowUI.handleTargetedEvent(event: TargetedEvent) {
        when (event) {
            is ChannelJoined -> {
                users.add(event.user.nickname)
                addLine("${event.timestamp} -- ${event.user.nickname} Joined")
            }
            is ChannelParted -> {
                users.remove(event.user.nickname)
                addLine("${event.timestamp} -- ${event.user.nickname} Left${if (event.reason.isNotEmpty()) " ${event.reason}" else ""}")
            }
            is MessageReceived -> addLine("${event.timestamp} <${event.user.nickname}> ${event.message}")
            is ActionReceived -> addLine("${event.timestamp} * ${event.user.nickname} ${event.action}")
            is ChannelNamesFinished -> {
                users.clear()
                users.addAll(client.channelState[event.target]?.users?.map { it.nickname } ?: emptyList())
            }
            is ChannelQuit -> {
                users.remove(event.user.nickname)
                addLine("${event.timestamp} -- ${event.user.nickname} Quit")
            }
            else -> {
            }
        }
    }

    private val IrcEvent.timestamp: String
        get() = metadata.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    private fun WindowUI.addLine(line: String) {
        textArea.appendText("$line\n")
    }

    private fun runLaterWithWindowUi(windowName: String, block: WindowUI.() -> Unit) =
        runLater {
            withWindowUi(windowName, block)
        }

    private fun withWindowUi(windowName: String, block: WindowUI.() -> Unit) =
        controller.windows.find {
            it.connection == this && it.name == windowName
        }?.windowUI?.block()

}