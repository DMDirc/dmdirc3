package com.dmdirc

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.messages.sendMessage
import javafx.beans.property.SimpleBooleanProperty
import tornadofx.ConfigProperties
import tornadofx.observable
import tornadofx.runLater
import java.time.format.DateTimeFormatter

class Connection(
    private val host: String,
    private val port: Int,
    private val password: String?,
    private val tls: Boolean,
    private val config: ConfigProperties,
    private val root: Window
) {
    private val window: Window = Window(
        host,
        emptyList<Window>().toMutableList().observable(),
        WindowType.SERVER,
        WindowUI(this),
        this
    )
    private val connected = SimpleBooleanProperty(false)
    private val client: IrcClient = IrcClient {
        server {
            host = this@Connection.host
            port = this@Connection.port
            password = this@Connection.password
            useTls = this@Connection.tls
        }
        profile {
            nickname = config.getProperty("nickname")
            realName = config.getProperty("realname")
            username = config.getProperty("username")
        }
    }

    fun connect() {
        client.onEvent(this::handleEvent)
        root.children.add(window)
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
                        window.children.add(
                            Window(
                                event.channel,
                                emptyList<Window>().toMutableList().observable(),
                                WindowType.CHANNEL,
                                WindowUI(this),
                                this
                            )
                        )
                    }
                } else {
                    runLater {
                        val rb = window.children.find {
                            event.channel == it.name
                        }?.windowUI ?: return@runLater
                        rb.users.add(event.user.nickname)
                        rb.textArea.appendText("${event.channel} > ${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} ${event.user} Joined\n")
                    }
                }
            }
            is MessageReceived -> {
                runLater {
                    println("Message: $event")
                    val rb = window.children.find {
                        event.target == it.name && WindowType.CHANNEL == it.type
                    }?.windowUI ?: return@runLater
                    rb.textArea.appendText("${event.target} > ${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} <${event.user.nickname}> ${event.message}\n")
                }
            }
            is ChannelNamesFinished -> {
                runLater {
                    val rb = window.children.find {
                        event.channel == it.name && WindowType.CHANNEL == it.type
                    }?.windowUI ?: return@runLater
                    rb.users.clear()
                    rb.users.addAll(client.channelState[event.channel]?.users?.map { it.nickname } ?: emptyList())
                }
            }
            is UserQuit -> {
                runLater {
                    root.children.forEach {
                        it.windowUI.users.remove(event.user.nickname)
                    }
                    root.children.map { it.windowUI }.map { it.textArea }.forEach {
                        it.appendText("${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} <${event.user}> Quit\n")
                    }
                }
            }
            else -> {
            }
        }
    }

}