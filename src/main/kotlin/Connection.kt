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
    private val window = WindowModel(
        host,
        WindowType.SERVER,
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

    init {
        controller.hackyWindowMap[window] = WindowUI(window)
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
                window.addLine(event, tr("*** Connected"))
                connected.value = true
            }
            event is ServerReady -> {
                networkName = client.serverState.features[ServerFeature.Network] ?: ""
                serverName = client.serverState.serverName
            }
            event is ServerDisconnected -> runLater {
                window.addLine(event, tr("*** Disconnected"))
                connected.value = false
            }
            event is ServerConnectionError -> runLater {
                window.addLine(event, tr("*** Error: %s - %s").format(event.error, event.details ?: ""))
            }
            event is ChannelJoined && client.isLocalUser(event.user) -> runLater {
                val model = WindowModel(
                    event.target,
                    WindowType.CHANNEL,
                    this,
                    false,
                    window.connectionId
                )
                controller.windows.add(model)
                controller.hackyWindowMap[model] = WindowUI(model)
            }
            event is TargetedEvent -> runLaterWithWindowUi(event.target) { handleTargetedEvent(event) }
            else -> {
            }
        }
    }

    private fun WindowModel.handleTargetedEvent(event: TargetedEvent) {
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

    private fun WindowModel.addLine(event: IrcEvent, line: String) =
        addLine(
            sequenceOf(
                StyledSpan(
                    event.timestamp,
                    setOf(Style.CustomStyle("timestamp"))
                )
            ) + " $line\n".detectLinks().convertControlCodes()
        )

    private fun WindowModel.addLine(spans: Sequence<StyledSpan>) {
        controller.hackyWindowMap[this]?.let { window ->
            // TODO: The model should handle all this, not the view directly.
            val images = mutableListOf<Style.Link>()
            spans.forEach {
                window.textArea.appendText(it.content)
                window.textArea.setStyle(window.textArea.length - it.content.length, window.textArea.length, it.styles)
                it.styles
                    .filterIsInstance(Style.Link::class.java)
                    .filter { s -> s.url.matches(Regex(".*\\.(png|jpg|jpeg)$", RegexOption.IGNORE_CASE)) }
                    .let(images::addAll)
            }

            if (this@Connection.config[ClientSpec.Display.embedImages]) {
                images.forEach {
                    window.textArea.appendText("${ControlCode.InternalImages}${it.url}")
                }
                if (images.isNotEmpty()) {
                    window.textArea.appendText("\n")
                }
            }
        }
    }

    private fun runLaterWithWindowUi(windowName: String, block: WindowModel.() -> Unit) =
        runLater {
            withWindowModel(windowName, block)
        }

    private fun withWindowModel(windowName: String, block: WindowModel.() -> Unit) =
        controller.windows.find {
            it.connection == this && it.name == windowName
        }?.block()

    fun disconnect() {
        client.disconnect()
        controller.windows.removeIf { it.connection == this@Connection }
    }

}
