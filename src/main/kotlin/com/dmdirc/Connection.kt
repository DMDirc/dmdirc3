package com.dmdirc

import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.io.CaseMapping
import com.dmdirc.ktirc.messages.sendAction
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.messages.sendMessage
import com.dmdirc.ktirc.messages.sendPart
import com.dmdirc.ktirc.model.ChannelUser
import com.dmdirc.ktirc.model.ServerFeature
import javafx.application.HostServices
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableSet
import javafx.scene.Node
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val connectionCounter = AtomicLong(0)

object ConnectionContract {
    interface Controller {
        val children: Connection.WindowMap
        val connected: BooleanProperty
        var networkName: String
        fun connect()
        fun sendMessage(channel: String, message: String)
        fun sendAction(channel: String, action: String)
        fun joinChannel(channel: String)
        fun leaveChannel(channel: String)
        fun getUsers(channel: String): Iterable<ChannelUser>
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

    override val connected = SimpleBooleanProperty(false)

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

    override fun sendMessage(channel: String, message: String) {
        client.sendMessage(channel, message)
    }

    override fun sendAction(channel: String, action: String) {
        client.sendAction(channel, action)
    }

    override fun joinChannel(channel: String) {
        client.sendJoin(channel)
    }

    override fun leaveChannel(channel: String) {
        client.sendPart(channel)
    }

    override fun getUsers(channel: String): Iterable<ChannelUser> = client.channelState[channel]?.users ?: emptyList()

    private fun handleEvent(event: IrcEvent) {
        when {
            event is BatchReceived -> event.events.forEach(this::handleEvent)
            event is ServerReady -> {
                connected.value = true
                networkName = client.serverState.features[ServerFeature.Network] ?: ""
                runLater {
                    model.name.value = client.serverState.serverName
                    model.title.value = "${model.name.value} [${model.connection?.networkName ?: ""}]"
                }
            }
            event is ServerDisconnected -> runLater { connected.value = false }
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
            }
            event is ChannelParted && client.isLocalUser(event.user) -> runLater { children -= event.target }
        }

        runLater {
            if (event is TargetedEvent) {
                if (client.isLocalUser(event.target) || event.target == "*") {
                    handleOwnEvent(event)
                } else {
                    windowModel(event.target)?.handleEvent(event)
                }
            } else {
                model.handleEvent(event)
            }
        }
    }

    private fun handleOwnEvent(event: TargetedEvent) {
        when (event) {
            is NoticeReceived -> model.handleEvent(event)
        }
    }

    private fun windowModel(windowName: String) = children[windowName]?.model

    override fun disconnect() {
        client.disconnect()
        children.clear()
    }

    data class Child(val model: WindowModel, val ui: Node)

    class WindowMap(private val caseMappingProvider: () -> CaseMapping) : Iterable<Child> {

        private val values = mutableSetOf<Child>().observable().synchronized()
        val observable: ObservableSet<Child> = values.readOnly()

        operator fun get(name: String) = synchronized(values) {
            values.find { caseMappingProvider().areEquivalent(it.model.name.value, name) }
        }

        operator fun plusAssign(value: Child): Unit = synchronized(values) {
            values.add(value)
        }

        operator fun minusAssign(name: String): Unit = synchronized(values) {
            values.removeIf { caseMappingProvider().areEquivalent(it.model.name.value, name) }
        }

        operator fun contains(name: String) = get(name) != null

        fun clear() = synchronized(values) {
            values.clear()
        }

        override fun iterator() = HashSet(values).iterator().iterator()

    }

}
