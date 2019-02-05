package com.dmdirc

import com.dmdirc.ktirc.IrcClientImpl
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.messages.sendJoin
import com.dmdirc.ktirc.messages.sendMessage
import com.dmdirc.ktirc.model.Profile
import com.dmdirc.ktirc.model.Server
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import org.fxmisc.richtext.StyleClassedTextArea
import tornadofx.Controller
import tornadofx.observable
import tornadofx.runLater
import java.time.format.DateTimeFormatter

enum class WindowType {
    ROOT,
    SERVER,
    CHANNEL
}

data class Window(val name: String, val children: ObservableList<Window>, val type: WindowType)

class MainController : Controller() {
    internal val users = emptyList<String>().toMutableList().observable()
    internal val channels = Window("root", emptyList<Window>().toMutableList().observable(), WindowType.ROOT)
    internal val selectedChannel = SimpleObjectProperty<Window>()
    internal val inputText = SimpleStringProperty()
    internal val textArea = StyleClassedTextArea()
    private var client: IrcClientImpl? = null
    private var serverName: String? = null
    val isConnected = SimpleBooleanProperty(false)

    fun connect(host: String, port: Int = 6667, tls: Boolean = false, password: String? = null) {
        serverName = host
        client = IrcClientImpl(
            Server(host = host, port = port, password = password, tls = tls),
            Profile(
                initialNick = app.config.getProperty("nickname"),
                realName = app.config.getProperty("realname"),
                userName = app.config.getProperty("username")
            )
        )
        if (client == null) {
            return
        }
        isConnected.set(true)
        client!!.onEvent { event ->
            when (event) {
                is ServerConnected -> {
                    channels.children.add(
                        Window(
                            serverName!!,
                            emptyList<Window>().toMutableList().observable(),
                            WindowType.SERVER
                        )
                    )
                }
                is ChannelJoined ->
                    if (client!!.isLocalUser(event.user)) {
                        runLater {
                            channels.children.find {
                                it.name == serverName
                            }?.children?.add(
                                Window(
                                    event.channel,
                                    emptyList<Window>().toMutableList().observable(),
                                    WindowType.CHANNEL
                                )
                            )
                        }
                    } else {
                        runLater {
                            users.add(event.user.nickname)
                            textArea.appendText("${event.channel} > ${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} ${event.user} Joined\n")
                        }
                    }
                is MessageReceived ->
                    runLater {
                        textArea.appendText("${event.target} > ${event.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} <${event.user.nickname}> ${event.message}\n")
                    }
                is ChannelNamesFinished ->
                    runAsync {
                        client!!.channelState[event.channel]?.users?.map { it.nickname } ?: emptyList()
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
        client!!.connect()
    }


    fun sendMessage(message: String) {
        val selected = selectedChannel.value ?: return
        when (selected.type) {
            WindowType.CHANNEL ->
                client!!.sendMessage(selected.name, message)
            else -> {
            }
        }
    }

    fun joinChannel(name: String) {
        client!!.sendJoin(name)
    }
}