package com.dmdirc

import com.dmdirc.ktirc.IrcClientImpl
import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.events.ChannelNamesFinished
import com.dmdirc.ktirc.events.MessageReceived
import com.dmdirc.ktirc.events.ServerWelcome
import com.dmdirc.ktirc.events.UserQuit
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.messages.sendMessage
import com.dmdirc.ktirc.model.Profile
import com.dmdirc.ktirc.model.Server
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import org.fxmisc.richtext.StyleClassedTextArea
import tornadofx.*
import java.time.format.DateTimeFormatter

class MainController : Controller() {
    internal val users = emptyList<String>().toMutableList().observable()
    internal val channels = emptyList<String>().toMutableList().observable()
    internal val inputText = SimpleStringProperty()
    internal val textArea = StyleClassedTextArea()
    internal val connectHostname = SimpleStringProperty()
    internal val connectPort = SimpleIntegerProperty()
    internal val connectPassword = SimpleStringProperty()
    private lateinit var client: IrcClientImpl

    fun connect(host: String, port: Int = 6667, tls: Boolean = false, password: String ?= null) {
        client = IrcClientImpl(
                Server(host=host, port=port, password=password, tls=tls),
                Profile(randomString(5), randomString(10), randomString(10)))
        client.onEvent { event ->
            when (event) {
                is ServerWelcome -> {}
                is ChannelJoined ->
                    if (client.isLocalUser(event.user)) {
                        runLater {
                            channels.add(event.channel)
                        }
                    } else {
                        runLater {
                            users.add(event.user.nickname)
                            textArea.appendText("${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} ${event.user} Joined\n")
                        }
                    }
                is MessageReceived ->
                    runLater {
                        textArea.appendText("${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} <${event.user.nickname}> ${event.message}\n")
                    }
                is ChannelNamesFinished ->
                    runAsync {
                        client.channelState[event.channel]?.users?.map { it.nickname } ?: emptyList()
                    } ui {
                        users.clear()
                        users.addAll(it)
                    }
                is UserQuit ->
                    runLater {
                        users.remove(event.user.nickname)
                        textArea.appendText("${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} <${event.user}> Quit\n")
                    }
                else -> {
                }
            }
        }
        client.connect()
    }

    fun sendMessage(message: String) {
        client.sendMessage("#mdbot", message)
    }
}