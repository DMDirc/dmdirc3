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
    val users = emptyList<String>().toMutableList().observable()
    val inputField = SimpleStringProperty("")

    fun handleInput() {
        if (inputField.value.isNotEmpty()) {
            connection?.sendMessage(name, inputField.value)
            inputField.value = ""
        }
    }
}

class WindowUI(model: WindowModel) : View("Right bit") {

    val textArea = IrcTextArea { url -> hostServices.showDocument(url) }

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
}
