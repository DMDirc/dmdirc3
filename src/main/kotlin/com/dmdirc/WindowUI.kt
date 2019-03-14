package com.dmdirc

import com.dmdirc.ClientSpec.Formatting.action
import com.dmdirc.ClientSpec.Formatting.channelEvent
import com.dmdirc.ClientSpec.Formatting.message
import com.dmdirc.ClientSpec.Formatting.notice
import com.dmdirc.ClientSpec.Formatting.serverEvent
import com.dmdirc.ktirc.events.ChannelMembershipAdjustment
import com.dmdirc.ktirc.events.IrcEvent
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
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
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
        val texts = inputField.value.split("\n")
        texts.forEach { text ->
            if (text.isNotEmpty()) {
                if (text.startsWith("/me ")) {
                    connection?.sendAction(name.value, text.substring(4))
                } else {
                    connection?.sendMessage(
                        name.value,
                        text
                    )
                }
                runLater {
                    inputField.value = ""
                }
            }
        }
    }

    fun handleEvent(event: IrcEvent) {
        if (event is ChannelMembershipAdjustment) nickList.handleEvent(event)
        eventMapper.displayableText(event)?.let {
            addLine(event.timestamp, eventMapper.flags(event), it)
        }
    }

    fun addLine(timestamp: String, flags: Set<MessageFlags>, args: Array<String>) {
        val message = " ${config[MessageFlags.formatter(flags)].format(*args)}"
        val spans = message.detectLinks().convertControlCodes().toMutableList()
        spans.add(0, StyledSpan(timestamp, setOf(Style.CustomStyle("timestamp"))))
        hasUnreadMessages.value = true
        lines.add(spans.toTypedArray())
    }

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
    val inputField = MagicInput(model.inputField, model)
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
            bottom = inputField
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

class MagicInput(private val testFromModel: Property<String>, model: WindowModel) : VBox() {
    private var active: TextInputControl? = null
    private val single = TextField().apply {
        styleClass.add("input-field")
    }
    private val multi = TextArea().apply {
        styleClass.add("input-field")
        isWrapText = true
        prefRowCount = 12
    }
    init {
        single.textProperty().bindBidirectional(testFromModel)
        multi.textProperty().bindBidirectional(testFromModel)
        active = single
        children.add(single)
        addEventFilter(KeyEvent.KEY_PRESSED) {
            if (active == single && it.isShiftDown && it.code == KeyCode.ENTER) {
                swap()
            }
            if (active == multi && !testFromModel.value.contains("\n")) {
                swap()
            }
            if (it.isShiftDown && !it.isControlDown && it.code == KeyCode.ENTER) {
                testFromModel.value += "\n"
                runLater {
                    active?.end()
                }
            } else if (!it.isShiftDown && !it.isControlDown && it.code == KeyCode.ENTER) {
                model.handleInput()
            }
        }
    }

    private fun swap() {
        runLater {
            val focused = active?.focusedProperty()?.value ?: false
            val pos = active?.caretPosition ?: 0
            if (active == single) {
                single.textProperty().unbindBidirectional(testFromModel)
                multi.textProperty().bindBidirectional(testFromModel)
                children.remove(single)
                children.add(multi)
                active = multi
            } else {
                multi.textProperty().unbindBidirectional(testFromModel)
                single.textProperty().bindBidirectional(testFromModel)
                children.remove(multi)
                children.add(single)
                active = single
            }
            if (focused) {
                active?.requestFocus()
            }
        }
    }

    override fun requestFocus() {
        active?.requestFocus()
    }
}
