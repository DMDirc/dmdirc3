package com.dmdirc

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.messages.sendMessage
import com.dmdirc.ktirc.messages.sendPart
import com.dmdirc.ktirc.model.ServerFeature
import com.jukusoft.i18n.I.tr
import javafx.application.HostServices
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableSet
import javafx.scene.Node
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val connectionCounter = AtomicLong(0)

object ConnectionContract {
    interface Controller {
        val children: Connection.WindowMap
        var networkName: String
        fun connect()
        fun sendMessage(channel: String, name: String)
        fun joinChannel(channel: String)
        fun leaveChannel(channel: String)
        fun disconnect()
    }
}

class Connection(
    private val connectionDetails: ConnectionDetails,
    private val config1: ClientConfig,
    private val hostServices: HostServices
) : ConnectionContract.Controller {

    private val connectionId = connectionCounter.incrementAndGet().toString(16).padStart(20)

    private val model = WindowModel(
        connectionDetails.hostname,
        WindowType.SERVER,
        this,
        config1,
        connectionId
    )

    private val connected = SimpleBooleanProperty(false)

    override val children = WindowMap { client.caseMapping }.apply {
        this += Child(model, WindowUI(model, hostServices))
    }

    override var networkName = ""

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

    override fun sendMessage(channel: String, name: String) {
        client.sendMessage(channel, name)
    }

    override fun joinChannel(channel: String) {
        client.sendJoin(channel)
    }

    override fun leaveChannel(channel: String) {
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
                    config1,
                    connectionId
                )
                model.addImageHandler(config1)
                children += Child(model, WindowUI(model, hostServices))
                withWindowModel(event.target) { handleTargetedEvent(event) }
            }
            event is ChannelParted && client.isLocalUser(event.user) -> runLater {
                children -= event.target
            }
            event is TargetedEvent -> runLaterWithWindowUi(event.target) { handleTargetedEvent(event) }
        }
    }

    private fun WindowModel.handleTargetedEvent(event: TargetedEvent) {
        displayEvent(event)
        when (event) {
            is ChannelJoined -> users.add(event.user.nickname)
            is ChannelParted -> users.remove(event.user.nickname)
            is ChannelNamesFinished -> {
                users.clear()
                users.addAll(client.channelState[event.target]?.users?.map { it.nickname } ?: emptyList())
            }
            is ChannelQuit -> users.remove(event.user.nickname)
        }
    }

    private val IrcEvent.timestamp: String
        get() = metadata.time.format(DateTimeFormatter.ofPattern(config1[ClientSpec.Formatting.timestamp]))

    private fun runLaterWithWindowUi(windowName: String, block: WindowModel.() -> Unit) =
        runLater {
            withWindowModel(windowName, block)
        }

    private fun withWindowModel(windowName: String, block: WindowModel.() -> Unit) =
        children[windowName]?.model?.block()

    override fun disconnect() {
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
