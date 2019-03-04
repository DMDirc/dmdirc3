package com.dmdirc

import com.dmdirc.ClientSpec.Formatting.action
import com.dmdirc.ClientSpec.Formatting.channelEvent
import com.dmdirc.ClientSpec.Formatting.message
import com.dmdirc.ClientSpec.Formatting.serverEvent
import com.dmdirc.ktirc.events.*
import com.dmdirc.ktirc.model.User
import com.jukusoft.i18n.I.tr
import com.uchuhimo.konf.Item
import javafx.application.HostServices
import javafx.beans.Observable
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.ListChangeListener
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.fxmisc.flowless.VirtualizedScrollPane
import java.time.format.DateTimeFormatter

enum class WindowType {
    ROOT,
    SERVER,
    CHANNEL
}

class WindowModel(
    initialName: String,
    val type: WindowType,
    val connection: ConnectionContract.Controller?,
    private val config: ClientConfig,
    connectionId: String?
) {
    val name: StringProperty = SimpleStringProperty(initialName)
    val title: StringProperty = SimpleStringProperty(initialName)
    val hasUnreadMessages: BooleanProperty = SimpleBooleanProperty(false)
    val nickList = NickListModel(connection)
    val isConnection = type == WindowType.SERVER
    val sortKey = "${connectionId ?: ""} ${if (isConnection) "" else initialName.toLowerCase()}"
    val lines = mutableListOf<Array<StyledSpan>>().observable()
    val inputField: StringProperty = SimpleStringProperty("")

    companion object {
        fun extractor(): Callback<WindowModel, Array<Observable>> {
            return Callback { m -> arrayOf(m.title) }
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
            inputField.value = ""
        }
    }

    fun handleEvent(event: IrcEvent) {
        displayEvent(event)
        nickList.handleEvent(event)
    }

    private fun displayEvent(event: IrcEvent) {
        val ts = event.timestamp
        when (event) {
            is ServerConnected ->
                addLine(ts, serverEvent, tr("Connected"))
            is ServerDisconnected ->
                addLine(ts, serverEvent, tr("Disconnected"))
            is ServerConnectionError ->
                addLine(ts, serverEvent, tr("Error: %s - %s").format(event.error.translated(), event.details ?: ""))

            is ChannelJoined ->
                addLine(ts, channelEvent, tr("%s joined").format(event.user.formattedNickname))
            is ChannelParted ->
                addLine(
                    ts, channelEvent,
                    if (event.reason.isEmpty())
                        tr("%s left").format(event.user.formattedNickname)
                    else
                        tr("%s left (%s)").format(event.user.formattedNickname, event.reason)
                )
            is ChannelQuit ->
                addLine(
                    ts, channelEvent,
                    if (event.reason.isEmpty())
                        tr("%s quit").format(event.user.formattedNickname)
                    else
                        tr("%s quit (%s)").format(event.user.formattedNickname, event.reason)
                )

            is MessageReceived ->
                addLine(ts, message, event.user.formattedNickname, event.message)
            is ActionReceived ->
                addLine(ts, action, event.user.formattedNickname, event.action)
        }
    }

    fun addLine(timestamp: String, format: Item<String>, vararg args: String) =
        addLine(
            sequenceOf(StyledSpan(timestamp, setOf(Style.CustomStyle("timestamp"))))
                    + " ${config[format].format(*args)}".detectLinks().convertControlCodes()
        )

    private fun addLine(spans: Sequence<StyledSpan>) {
        lines.add(spans.toList().toTypedArray())
    }

    private val User.formattedNickname: String
        get() = "${ControlCode.InternalNicknames}$nickname${ControlCode.InternalNicknames}"

    private val IrcEvent.timestamp: String
        get() = metadata.time.format(DateTimeFormatter.ofPattern(config[ClientSpec.Formatting.timestamp]))

}

class WindowUI(model: WindowModel, hostServices: HostServices) : AnchorPane() {

    private val textArea = IrcTextArea { url -> hostServices.showDocument(url) }

    init {
        val borderPane = BorderPane().apply {
            center = VirtualizedScrollPane(textArea)
            right = ListView<String>(model.nickList.users).apply {
                styleClass.add("nick-list")
                prefWidth = 148.0
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
        })
    }
}
