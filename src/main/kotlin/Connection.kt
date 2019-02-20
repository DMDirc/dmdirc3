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
            is ServerConnected -> {
                connected.value = true
            }
            is ChannelJoined -> {
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
                    runLater {
                        val rb = controller.windows.find {
                            it.connection == this && it.name == event.channel
                        }?.windowUI ?: return@runLater
                        rb.users.add(event.user.nickname)
                        rb.textArea.appendText("${event.channel} > ${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} ${event.user} Joined\n")
                    }
                }
            }
            is MessageReceived -> {
                runLater {
                    val rb = controller.windows.find {
                        it.connection == this && it.name == event.target
                    }?.windowUI ?: return@runLater
                    rb.textArea.appendText("${event.target} > ${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} <${event.user.nickname}> ${event.message}\n")
                }
            }
            is ChannelNamesFinished -> {
                runLater {
                    val rb = controller.windows.find {
                        it.connection == this && it.name == event.channel
                    }?.windowUI ?: return@runLater
                    rb.users.clear()
                    rb.users.addAll(client.channelState[event.channel]?.users?.map { it.nickname } ?: emptyList())
                }
            }
            is UserQuit -> {
                runLater {
                    controller.windows.forEach {
                        it.windowUI.textArea.appendText("${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} <${event.user}> Quit\n")
                    }
                }
            }
            else -> {
            }
        }
    }

}