package com.dmdirc

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.messages.sendMessage
import com.dmdirc.ktirc.messages.sendPart
import com.dmdirc.ktirc.model.ServerFeature
import com.jukusoft.i18n.I.tr
import com.uchuhimo.konf.Item
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
    private val config1: ClientConfig,
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
            nickname = config1[ClientSpec.DefaultProfile.nickname]
            realName = config1[ClientSpec.DefaultProfile.realname]
            username = config1[ClientSpec.DefaultProfile.username]
        }
        behaviour {
            alwaysEchoMessages = true
        }
    }

    init {
        controller.windowUis[window] = WindowUI(window)
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
                window.addLine(event.timestamp, ClientSpec.Formatting.serverEvent, tr("Connected"))
                connected.value = true
            }
            event is ServerReady -> {
                networkName = client.serverState.features[ServerFeature.Network] ?: ""
                serverName = client.serverState.serverName
            }
            event is ServerDisconnected -> runLater {
                window.addLine(event.timestamp, ClientSpec.Formatting.serverEvent, tr("Disconnected"))
                connected.value = false
            }
            event is ServerConnectionError -> runLater {
                window.addLine(event.timestamp, ClientSpec.Formatting.serverEvent, tr("Error: %s - %s").format(event.error, event.details ?: ""))
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
                controller.windowUis[model] = WindowUI(model)
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
                addLine(event.timestamp, ClientSpec.Formatting.channelEvent, tr("%s joined").format(event.user.nickname))
            }
            is ChannelParted -> {
                users.remove(event.user.nickname)
                if (event.reason.isEmpty()) {
                    addLine(event.timestamp, ClientSpec.Formatting.channelEvent, tr("%s left").format(event.user.nickname))
                } else {
                    addLine(event.timestamp, ClientSpec.Formatting.channelEvent, tr("%s left (%s)").format(event.user.nickname, event.reason))
                }
                if (client.isLocalUser(event.user)) {
                    controller.windows.removeIf { it.connection == this@Connection && it.name == event.target }
                    controller.windowUis.remove(this)
                }
            }
            is MessageReceived -> addLine(event.timestamp, ClientSpec.Formatting.message, event.user.nickname, event.message)
            is ActionReceived -> addLine(event.timestamp, ClientSpec.Formatting.action, event.user.nickname, event.action)
            is ChannelNamesFinished -> {
                users.clear()
                users.addAll(client.channelState[event.target]?.users?.map { it.nickname } ?: emptyList())
            }
            is ChannelQuit -> {
                users.remove(event.user.nickname)
                addLine(event.timestamp, ClientSpec.Formatting.channelEvent, tr("%s quit").format(event.user.nickname))
            }
            else -> {
            }
        }
    }

    private val IrcEvent.timestamp: String
        get() = metadata.time.format(DateTimeFormatter.ofPattern(config1[ClientSpec.Formatting.timestamp]))

    private fun WindowModel.addLine(timestamp: String, format: Item<String>, vararg args: String) =
        addLine(
            sequenceOf(
                StyledSpan(
                    timestamp,
                    setOf(Style.CustomStyle("timestamp"))
                )
            ) + " ${config1[format].format(*args)}".detectLinks().convertControlCodes()
        )

    private fun WindowModel.addLine(spans: Sequence<StyledSpan>) {
        lines.add(spans.toList().toTypedArray())
        if (config1[ClientSpec.Display.embedImages]) {
            val images = spans
                .toList()
                .flatMap { it.styles }
                .filterIsInstance(Style.Link::class.java)
                .map { it.url }
                .filter { it.matches(Regex(".*\\.(png|jpg|jpeg)$", RegexOption.IGNORE_CASE)) }
                .map { StyledSpan("${ControlCode.InternalImages}$it", emptySet()) }
                .toTypedArray()
            if (images.isNotEmpty()) {
                lines.add(images)
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
        controller.windowUis.remove(window)
    }

}
