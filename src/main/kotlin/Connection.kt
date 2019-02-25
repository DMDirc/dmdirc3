package com.dmdirc

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.messages.sendMessage
import com.dmdirc.ktirc.messages.sendPart
import com.dmdirc.ktirc.model.ServerFeature
import com.jukusoft.i18n.I.tr
import javafx.beans.property.SimpleBooleanProperty
import tornadofx.runLater
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

private val connectionCounter = AtomicLong(0)

class Connection(
    private val host: String,
    private val port: Int,
    private val password: String?,
    private val tls: Boolean,
    private val config: ClientConfig,
    private val controller: MainController
) {
    private val window: Window = Window(
        host,
        WindowType.SERVER,
        WindowUI(this),
        this,
        true,
        connectionCounter.incrementAndGet().toString(16).padStart(20)
    )
    private val connected = SimpleBooleanProperty(false)
    var networkName = ""
    var serverName = ""
    private val client: IrcClient = IrcClient {
        server(host, port, tls, password)
        profile {
            nickname = config[ClientSpec.DefaultProfile.nickname]
            realName = config[ClientSpec.DefaultProfile.realname]
            username = config[ClientSpec.DefaultProfile.username]
        }
        behaviour {
            alwaysEchoMessages = true
        }
    }

    fun connect() {
        client.onEvent(this::handleEvent)
        serverName = client.serverState.serverName
        controller.windows.add(window)
        client.connect()
    }

    fun sendMessage(channel: String, name: String) {
        client.sendMessage(channel, name)
    }

    fun joinChannel(channel: String) {
        client.sendJoin(channel)
    }

    fun leaveChannel(channel: String) {
        client.sendPart(channel)
    }

    private fun handleEvent(event: IrcEvent) {
        when {
            event is BatchReceived -> event.events.forEach(this::handleEvent)
            event is ServerConnected -> runLater {
                window.windowUI.addLine(event, tr("*** Connected"))
                connected.value = true
            }
            event is ServerReady -> {
                networkName = client.serverState.features[ServerFeature.Network] ?: ""
                serverName = client.serverState.serverName
            }
            event is ServerDisconnected -> runLater {
                window.windowUI.addLine(event, tr("*** Disconnected"))
                connected.value = false
            }
            event is ServerConnectionError -> runLater {
                window.windowUI.addLine(event, tr("*** Error: %s - %s").format(event.error, event.details ?: ""))
            }
            event is ChannelJoined && client.isLocalUser(event.user) -> runLater {
                controller.windows.add(
                    Window(
                        event.target,
                        WindowType.CHANNEL,
                        WindowUI(this),
                        this,
                        false,
                        window.connectionId
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
                addLine(event, tr("-- %s joined").format(event.user.nickname))
            }
            is ChannelParted -> {
                users.remove(event.user.nickname)
                if (event.reason.isEmpty()) {
                    addLine(event, tr("-- %s left").format(event.user.nickname))
                } else {
                    addLine(event, tr("-- %s left (%s)").format(event.user.nickname, event.reason))
                }
                if (client.isLocalUser(event.user)) {
                    controller.windows.removeIf { it.connection == this@Connection && it.name == event.target }
                }
            }
            is MessageReceived -> addLine(event, "<${event.user.nickname}> ${event.message}")
            is ActionReceived -> addLine(event, "* ${event.user.nickname} ${event.action}")
            is ChannelNamesFinished -> {
                users.clear()
                users.addAll(client.channelState[event.target]?.users?.map { it.nickname } ?: emptyList())
            }
            is ChannelQuit -> {
                users.remove(event.user.nickname)
                addLine(event, tr("-- %s quit").format(event.user.nickname))
            }
            else -> {
            }
        }
    }

    private val IrcEvent.timestamp: String
        get() = metadata.time.format(DateTimeFormatter.ofPattern(config[ClientSpec.Formatting.timestamp]))

    private fun WindowUI.addLine(event: IrcEvent, line: String) =
        addLine(sequenceOf(StyledSpan(event.timestamp, setOf(Style.CustomStyle("timestamp")))) + " $line\n".detectLinks().convertControlCodes())

    private fun WindowUI.addLine(spans: Sequence<StyledSpan>) {
        spans.forEach {
            textArea.appendText(it.content)
            textArea.setStyle(textArea.length - it.content.length, textArea.length, it.styles)
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

    fun disconnect() {
        client.disconnect()
        controller.windows.removeIf { it.connection == this@Connection }
    }

}
