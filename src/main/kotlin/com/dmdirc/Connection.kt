package com.dmdirc

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.messages.sendMessage
import com.dmdirc.ktirc.messages.sendPart
import com.dmdirc.ktirc.model.ServerFeature
import com.dmdirc.ktirc.model.User
import com.jukusoft.i18n.I.tr
import com.uchuhimo.konf.Item
import javafx.application.HostServices
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableSet
import javafx.scene.Node
import tornadofx.runLater
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val connectionCounter = AtomicLong(0)

object ConnectionContract {
    interface Controller {
        val children: Connection.WindowMap
        fun connect()
    }
}

class Connection(
    private val connectionDetails: ConnectionDetails,
    private val config1: ClientConfig,
    private val hostServices: HostServices
) : ConnectionContract.Controller {

    private val model = WindowModel(
        connectionDetails.hostname,
        WindowType.SERVER,
        this,
        true,
        connectionCounter.incrementAndGet().toString(16).padStart(20)
    )

    private val connected = SimpleBooleanProperty(false)

    override val children = WindowMap { client.caseMapping }.apply {
        this += Child(model, WindowUI(model, hostServices))
    }

    var networkName = ""

    private val client: IrcClient = IrcClient {
        server {
            host = connectionDetails.hostname
            port = connectionDetails.port
            password = connectionDetails.password
            useTls = connectionDetails.tls
        }
        profile {
            nickname = config1[ClientSpec.DefaultProfile.nickname]
            realName = config1[ClientSpec.DefaultProfile.realname]
            username = config1[ClientSpec.DefaultProfile.username]
        }
        behaviour {
            alwaysEchoMessages = true
        }
    }

    override fun connect() {
        client.onEvent(this::handleEvent)
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
                model.addLine(event.timestamp, ClientSpec.Formatting.serverEvent, tr("Connected"))
                connected.value = true
            }
            event is ServerReady -> {
                networkName = client.serverState.features[ServerFeature.Network] ?: ""
                model.name = client.serverState.serverName
            }
            event is ServerDisconnected -> runLater {
                model.addLine(event.timestamp, ClientSpec.Formatting.serverEvent, tr("Disconnected"))
                connected.value = false
            }
            event is ServerConnectionError -> runLater {
                model.addLine(
                    event.timestamp,
                    ClientSpec.Formatting.serverEvent,
                    tr("Error: %s - %s").format(event.error, event.details ?: "")
                )
            }
            event is ChannelJoined && client.isLocalUser(event.user) -> runLater {
                val model = WindowModel(
                    event.target,
                    WindowType.CHANNEL,
                    this,
                    false,
                    model.connectionId
                )
                children += Child(model, WindowUI(model, hostServices))
                withWindowModel(event.target) { handleTargetedEvent(event) }
            }
            event is ChannelParted && client.isLocalUser(event.user) -> runLater {
                children -= event.target
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
                addLine(
                    event.timestamp,
                    ClientSpec.Formatting.channelEvent,
                    tr("%s joined").format(event.user.formattedNickname)
                )
            }
            is ChannelParted -> {
                users.remove(event.user.nickname)
                if (event.reason.isEmpty()) {
                    addLine(
                        event.timestamp,
                        ClientSpec.Formatting.channelEvent,
                        tr("%s left").format(event.user.formattedNickname)
                    )
                } else {
                    addLine(
                        event.timestamp,
                        ClientSpec.Formatting.channelEvent,
                        tr("%s left (%s)").format(event.user.formattedNickname, event.reason)
                    )
                }
            }
            is MessageReceived -> addLine(
                event.timestamp,
                ClientSpec.Formatting.message,
                event.user.formattedNickname,
                event.message
            )
            is ActionReceived -> addLine(
                event.timestamp,
                ClientSpec.Formatting.action,
                event.user.formattedNickname,
                event.action
            )
            is ChannelNamesFinished -> {
                users.clear()
                users.addAll(client.channelState[event.target]?.users?.map { it.nickname } ?: emptyList())
            }
            is ChannelQuit -> {
                users.remove(event.user.nickname)
                addLine(
                    event.timestamp,
                    ClientSpec.Formatting.channelEvent,
                    tr("%s quit").format(event.user.formattedNickname)
                )
            }
            else -> {
            }
        }
    }

    private val User.formattedNickname: String
        get() = "${ControlCode.InternalNicknames}$nickname${ControlCode.InternalNicknames}"

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
        children[windowName]?.model?.block()

    fun disconnect() {
        client.disconnect()
        children.clear()
    }

    data class Child(val model: WindowModel, val ui: Node)

    class WindowMap(private val caseMappingProvider: () -> CaseMapping) : Iterable<Child> {

        private val values = mutableSetOf<Child>().observable().synchronized()
        val observable: ObservableSet<Child> = values.readOnly()

        operator fun get(name: String) = synchronized(values) {
            values.find { caseMappingProvider().areEquivalent(it.model.name, name) }
        }

        operator fun plusAssign(value: Child): Unit = synchronized(values) {
            values.add(value)
        }

        operator fun minusAssign(name: String): Unit = synchronized(values) {
            values.removeIf { caseMappingProvider().areEquivalent(it.model.name, name) }
        }

        operator fun contains(name: String) = get(name) != null

        fun clear() = synchronized(values) {
            values.clear()
        }

        override fun iterator() = HashSet(values).iterator().iterator()

    }

}
