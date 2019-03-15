package com.dmdirc

import com.dmdirc.ClientSpec.Formatting.action
import com.dmdirc.ClientSpec.Formatting.channelEvent
import com.dmdirc.ClientSpec.Formatting.message
import com.dmdirc.ClientSpec.Formatting.notice
import com.dmdirc.ClientSpec.Formatting.serverEvent
import com.dmdirc.Style.CustomStyle
import com.dmdirc.ktirc.events.ChannelMembershipAdjustment
import com.dmdirc.ktirc.events.IrcEvent
import com.uchuhimo.konf.Item
import javafx.application.HostServices
import javafx.beans.Observable
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ListChangeListener
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.ListView
import javafx.scene.control.ScrollBar
import javafx.scene.control.TextField
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
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
    private val eventMapper: IrcEventMapper,
    private val config: ClientConfig,
    connectionId: String?
) {
    val name: Property<String> = SimpleStringProperty(initialName).threadAsserting()
    val title: Property<String> = SimpleStringProperty(initialName).threadAsserting()
    val unreadStatus: Property<MessageFlag?> = SimpleObjectProperty<MessageFlag?>(null).threadAsserting()
    val nickList = NickListModel()
    val isConnection = type == WindowType.SERVER
    val sortKey = "${connectionId ?: ""} ${if (isConnection) "" else initialName.toLowerCase()}"
    val lines = mutableListOf<Array<StyledSpan>>().observable()
    val inputField: Property<String> = SimpleStringProperty("").threadAsserting()

    companion object {
        fun extractor(): Callback<WindowModel, Array<Observable>> {
            return Callback { m ->
                arrayOf(
                    m.title, m.unreadStatus, m?.connection?.connected ?: SimpleBooleanProperty(false)
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
        eventMapper.displayableText(event)?.let {
            addLine(event.timestamp, eventMapper.flags(event), it)
        }
    }

    fun addLine(timestamp: String, flags: Set<MessageFlag>, args: Array<String>) {
        val message = " ${config[MessageFlag.formatter(flags)].format(*args)}"
        val spans = message.detectLinks().convertControlCodes().toMutableList()
        spans.add(0, StyledSpan(timestamp, setOf(Style.CustomStyle("timestamp"))))

        val unreadFlags = unreadStatus.value?.let { flags + it } ?: flags
        unreadStatus.value = unreadFlags.maxBy { it.ordinal }

        val flagStyles = flags.map { CustomStyle("messagetype-${it.name}") }
        lines.add(spans.map { StyledSpan(it.content, it.styles + flagStyles) }.toTypedArray())

        if (MessageFlag.Highlight in flags) {
            connection?.notify(this, message)
        }
    }

    private val IrcEvent.timestamp: String
        get() = metadata.time.format(DateTimeFormatter.ofPattern(config[ClientSpec.Formatting.timestamp]))
}

enum class MessageFlag {
    ServerEvent, ChannelEvent, Self, Message, Action, Notice, Highlight;

    companion object {
        fun formatter(flags: Set<MessageFlag>): Item<String> = when {
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
    val inputField = TextField()
    private var autoScroll = true

    init {
        val borderPane = BorderPane().apply {
            center = VirtualizedScrollPane(textArea.apply {
                isFocusTraversable = false
            }).apply {
                scrollbar = childrenUnmodifiable.filterIsInstance<ScrollBar>().find {
                    it.orientation == VERTICAL
                }
            }
            if (!model.isConnection) {
                right = ListView<String>(model.nickList.users).apply {
                    isFocusTraversable = false
                    styleClass.add("nick-list")
                    prefWidth = 148.0
                }
            }
            bottom = inputField.apply {
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
        textArea.totalHeightEstimateProperty().addListener { _, _, _ ->
            if (autoScroll) {
                scrollbar?.valueProperty()?.value = scrollbar?.max
            }
        }
        scrollbar?.addEventFilter(MouseEvent.MOUSE_PRESSED) { _ ->
            runLater {
                autoScroll = scrollbar?.valueProperty()?.value == scrollbar?.max
            }
        }
        scrollbar?.addEventFilter(MouseEvent.MOUSE_RELEASED) { _ ->
            runLater {
                autoScroll = scrollbar?.valueProperty()?.value == scrollbar?.max
            }
        }
        textArea.addEventFilter(ScrollEvent.SCROLL) { _ ->
            GlobalScope.launch {
                delay(100)
                runLater {
                    autoScroll = scrollbar?.valueProperty()?.value == scrollbar?.max
                }
            }
        }
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
