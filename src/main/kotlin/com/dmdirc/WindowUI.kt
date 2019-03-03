package com.dmdirc

import javafx.application.HostServices
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ListChangeListener
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.fxmisc.flowless.VirtualizedScrollPane

enum class WindowType {
    ROOT,
    SERVER,
    CHANNEL
}

class WindowModel(
    var name: String,
    val type: WindowType,
    var connection: ConnectionContract.Controller?,
    var isConnection: Boolean,
    val connectionId: String?
) {
    val sortKey = "${connectionId ?: ""} ${if (isConnection) "" else name.toLowerCase()}"
    val users = mutableListOf<String>().observable()
    val lines = mutableListOf<Array<StyledSpan>>().observable()
    val inputField = SimpleStringProperty("")

    fun handleInput() {
        if (inputField.value.isNotEmpty()) {
            connection?.sendMessage(name, inputField.value)
            inputField.value = ""
        }
    }
}

class WindowUI(model: WindowModel, hostServices: HostServices) : AnchorPane() {

    private val textArea = IrcTextArea { url -> hostServices.showDocument(url) }

    init {
        val borderPane = BorderPane().apply {
            center = VirtualizedScrollPane(textArea.apply {
                isEditable = false
                isWrapText = true
            })
            right = ListView<String>(model.users).apply {
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
