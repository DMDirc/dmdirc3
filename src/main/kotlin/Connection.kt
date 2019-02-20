package com.dmdirc

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.messages.sendMessage
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
        when (event) {
            is ServerConnected -> connected.value = true
            is ChannelJoined ->
                if (client.isLocalUser(event.user)) {
                    runLater {
                        controller.windows.add(
                            Window(
                                event.channel,
                                WindowType.CHANNEL,
                                WindowUI(this),
                                this
                            )
                        )
                    }
                } else {
                    runLaterWithWindowUi(event.channel) {
                        users.add(event.user.nickname)
                        textArea.appendText("${event.metadata.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} ${event.user.nickname} Joined\n")
                    }
                }
            is MessageReceived ->
                runLaterWithWindowUi(event.target) {
                    textArea.appendText("${event.metadata.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} <${event.user.nickname}> ${event.message}\n")
                }
            is ActionReceived ->
                runLaterWithWindowUi(event.target) {
                    textArea.appendText("${event.metadata.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} * ${event.user.nickname} ${event.action}\n")
                }
            is ChannelNamesFinished ->
                runLaterWithWindowUi(event.channel) {
                    users.clear()
                    users.addAll(client.channelState[event.channel]?.users?.map { it.nickname } ?: emptyList())
                }
            is ChannelQuit ->
                runLaterWithWindowUi(event.channel) {
                    users.remove(event.user.nickname)
                    textArea.appendText("${event.metadata.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} -- ${event.user.nickname} Quit\n")
                }
            else -> {
            }
        }
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