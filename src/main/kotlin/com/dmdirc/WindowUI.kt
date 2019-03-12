package com.dmdirc

import com.dmdirc.ClientSpec.Formatting.action
import com.dmdirc.ClientSpec.Formatting.channelEvent
import com.dmdirc.ClientSpec.Formatting.message
import com.dmdirc.ClientSpec.Formatting.notice
import com.dmdirc.ClientSpec.Formatting.serverEvent
import com.dmdirc.MessageFlags.Action
import com.dmdirc.MessageFlags.ChannelEvent
import com.dmdirc.MessageFlags.Message
import com.dmdirc.MessageFlags.Notice
import com.dmdirc.MessageFlags.ServerEvent
import com.dmdirc.WindowType.CHANNEL
import com.dmdirc.ktirc.events.ActionReceived
import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.events.ChannelMembershipAdjustment
import com.dmdirc.ktirc.events.ChannelNickChanged
import com.dmdirc.ktirc.events.ChannelParted
import com.dmdirc.ktirc.events.ChannelQuit
import com.dmdirc.ktirc.events.ChannelTopicChanged
import com.dmdirc.ktirc.events.ChannelTopicDiscovered
import com.dmdirc.ktirc.events.ChannelTopicMetadataDiscovered
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.MessageReceived
import com.dmdirc.ktirc.events.NoticeReceived
import com.dmdirc.ktirc.events.ServerConnected
import com.dmdirc.ktirc.events.ServerConnectionError
import com.dmdirc.ktirc.events.ServerDisconnected
import com.dmdirc.ktirc.events.SourcedEvent
import com.dmdirc.ktirc.model.User
import com.jukusoft.i18n.I.tr
import com.uchuhimo.konf.Item
import javafx.application.HostServices
import javafx.beans.Observable
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ListChangeListener
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.ListView
import javafx.scene.control.ScrollBar
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.fxmisc.flowless.VirtualizedScrollPane
import java.time.format.DateTimeFormatter

enum class WindowType {
    ROOT, SERVER, CHANNEL
}

class WindowModel(
    initialName: String,
    val type: WindowType,
    val connection: ConnectionContract.Controller?,
    private val config: ClientConfig,
    connectionId: String?
) {
    val name: Property<String> = SimpleStringProperty(initialName).threadAsserting()
    val title: Property<String> = SimpleStringProperty(initialName).threadAsserting()
    val hasUnreadMessages: Property<Boolean> = SimpleBooleanProperty(false).threadAsserting()
    val nickList = NickListModel()
    val isConnection = type == WindowType.SERVER
    val sortKey = "${connectionId ?: ""} ${if (isConnection) "" else initialName.toLowerCase()}"
    val lines = mutableListOf<Array<StyledSpan>>().observable()
    val inputField: Property<String> = SimpleStringProperty("").threadAsserting()

    companion object {
        fun extractor(): Callback<WindowModel, Array<Observable>> {
            return Callback { m ->
                arrayOf(
                    m.title, m.hasUnreadMessages, m?.connection?.connected ?: SimpleBooleanProperty(false)
                )
            }
        }
    }

    fun handleInput() {
        val text = inputField.value
        if (text.isNotEmpty()) {
            if (text.startsWith("/me ")) {
                connection?.sendAction(name.value, text.substring(4))
            } else {
                connection?.sendMessage(name.value, text)
            }
            runLater {
                inputField.value = ""
            }
        }
    }

    fun handleEvent(event: IrcEvent) {
        if (event is ChannelMembershipAdjustment) nickList.handleEvent(event)
        event.displayableText?.let { addLine(event.timestamp, event.flags(), it) }
    }

    private fun IrcEvent.flags() = sequence {
        yield(if (type == CHANNEL) ChannelEvent else ServerEvent)

        when (this@flags) {
            is MessageReceived -> yield(Message)
            is ActionReceived -> yield(Action)
            is NoticeReceived -> yield(Notice)
        }
    }.toSet()

    private val IrcEvent.displayableText: Array<String>?
        get() = when (this) {
            is ServerConnected -> arrayOf(tr("Connected"))
            is ServerDisconnected -> arrayOf(tr("Disconnected"))
            is ServerConnectionError -> arrayOf(tr("Error: %s - %s").format(error.translated(), details ?: ""))
            is ChannelJoined -> arrayOf(tr("%s joined").format(formattedNickname))
            is ChannelParted -> if (reason.isEmpty()) arrayOf(tr("%s left").format(formattedNickname))
            else arrayOf(tr("%s left (%s)").format(formattedNickname, reason))
            is ChannelQuit -> if (reason.isEmpty()) arrayOf(tr("%s quit").format(formattedNickname))
            else arrayOf(tr("%s quit (%s)").format(formattedNickname, reason))
            is ChannelNickChanged -> arrayOf(
                tr("%s is now known as %s").format(
                    formattedNickname, newNick.formattedNickname
                )
            )
            is ChannelTopicChanged -> arrayOf(tr("%s has changed the topic to: %s").format(formattedNickname, topic))
            is ChannelTopicDiscovered -> if (topic.isNullOrEmpty()) {
                arrayOf(tr("there is no topic set"))
            } else {
                arrayOf(tr("the topic is: %s").format(topic))
            }
            is ChannelTopicMetadataDiscovered -> arrayOf(
                tr("topic was set at %s on %s by %s").format(
                    setTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    setTime.format(DateTimeFormatter.ofPattern("cccc, d LLLL yyyy")),
                    user.formattedNickname
                )
            )
            is MessageReceived -> arrayOf(user.formattedNickname, message)
            is NoticeReceived -> arrayOf(user.formattedNickname, message)
            is ActionReceived -> arrayOf(user.formattedNickname, action)
            else -> null
        }

    fun addLine(timestamp: String, flags: Set<MessageFlags>, args: Array<String>) {
        val message = " ${config[MessageFlags.formatter(flags)].format(*args)}"
        val spans = message.detectLinks().convertControlCodes().toMutableList()
        spans.add(0, StyledSpan(timestamp, setOf(Style.CustomStyle("timestamp"))))
        hasUnreadMessages.value = true
        lines.add(spans.toTypedArray())
    }

    private val SourcedEvent.formattedNickname: String
        get() = user.formattedNickname

    private val User.formattedNickname: String
        get() = nickname.formattedNickname

    private val String.formattedNickname: String
        get() = "${ControlCode.InternalNicknames}$this${ControlCode.InternalNicknames}"

    private val IrcEvent.timestamp: String
        get() = metadata.time.format(DateTimeFormatter.ofPattern(config[ClientSpec.Formatting.timestamp]))
}

enum class MessageFlags {
    ServerEvent, ChannelEvent, Self, Message, Action, Notice, Highlight;

    companion object {
        fun formatter(flags: Set<MessageFlags>): Item<String> = when {
            Message in flags -> message
            Action in flags -> action
            Notice in flags -> notice
            ChannelEvent in flags -> channelEvent
            else -> serverEvent
        }
    }
}

class WindowUI(model: WindowModel, hostServices: HostServices) : AnchorPane() {

    private var scrollbar: ScrollBar? = null
    private val textArea = IrcTextArea { url -> hostServices.showDocument(url) }

    init {
        val borderPane = BorderPane().apply {
            center = VirtualizedScrollPane(textArea).apply {
                scrollbar = childrenUnmodifiable.filterIsInstance<ScrollBar>().find {
                    it.orientation == VERTICAL
                }
            }
            if (!model.isConnection) {
                right = ListView<String>(model.nickList.users).apply {
                    styleClass.add("nick-list")
                    prefWidth = 148.0
                }
            }
            bottom = TextField().apply {
                model.inputField.bindBidirectional(this.textProperty())
                styleClass.add("input-field")
                setOnAction {
                    GlobalScope.launch {
                        model.handleInput()
                    }
                }
            }
            AnchorPane.setTopAnchor(this, 0.0)
            AnchorPane.setLeftAnchor(this, 0.0)
            AnchorPane.setRightAnchor(this, 0.0)
            AnchorPane.setBottomAnchor(this, 0.0)
        }
        children.add(borderPane)
        model.lines.addListener(ListChangeListener { change ->
            val scrollVisible = scrollbar?.isVisible ?: false
            val autoscroll = scrollbar?.value == scrollbar?.max
            // TODO: Support ops other than just appending lines (editing, deleting, inserting earlier, etc).
            while (change.next()) {
                if (change.wasAdded()) {
                    change.addedSubList.forEach { line ->
                        line.forEach { segment ->
                            val position = textArea.length
                            textArea.appendText(segment.content)
                            textArea.setStyle(position, textArea.length, segment.styles)
                        }
                        textArea.appendText("\n")
                    }
                }
            }
            if (autoscroll || scrollVisible != scrollbar?.isVisible) {
                scrollbar?.valueProperty()?.value = scrollbar?.max
            }
        })
    }
}
