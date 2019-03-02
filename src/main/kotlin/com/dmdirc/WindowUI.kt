package com.dmdirc

import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import org.fxmisc.flowless.VirtualizedScrollPane
import tornadofx.*

enum class WindowType {
    ROOT,
    SERVER,
    CHANNEL
}

class WindowModel(
    val name: String,
    val type: WindowType,
    var connection: Connection?,
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

class WindowUI(model: WindowModel) : View("Right bit") {

    private val textArea = IrcTextArea { url -> hostServices.showDocument(url) }

    override val root = borderpane {
        center = VirtualizedScrollPane(textArea.apply {
            isEditable = false
            isWrapText = true
        }).apply {
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
        }
        right = listview(model.users) {
            styleClass.add("nick-list")
            prefWidth = 148.0
        }

        bottom = textfield(model.inputField) {
            styleClass.add("input-field")
            action {
                runAsync {
                    model.handleInput()
                }
            }
        }
    }

    init {
        model.lines.onChange { change ->
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
        }
    }

}
