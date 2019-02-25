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

data class Window(
    val name: String,
    val type: WindowType,
    var windowUI: WindowUI,
    var connection: Connection?,
    var isConnection: Boolean,
    val connectionId: String?
) {
    val sortKey = "${connectionId ?: ""} ${if (isConnection) "" else name.toLowerCase()}"
}

class WindowUI(connection: Connection?) : View("Right bit") {
    private val controller: MainController by inject()
    val textArea = IrcTextArea { url -> hostServices.showDocument(url) }
    private val inputText = SimpleStringProperty()
    val users = emptyList<String>().toMutableList().observable()

    lateinit var myConnection: Connection

    init {
        connection?.let { myConnection = it }
    }

    override val root = borderpane {
        center = VirtualizedScrollPane(textArea.apply {
            isEditable = false
            isWrapText = true
        }).apply {
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
        }
        right = listview(users) {
            styleClass.add("nick-list")
            prefWidth = 148.0
        }

        bottom = textfield(inputText) {
            styleClass.add("input-field")
            action {
                if (inputText.value.isNotEmpty()) {
                    runAsync {
                        myConnection.sendMessage(controller.selectedWindow.value.name, inputText.value)
                        inputText.value = ""
                    }
                }
            }
        }
    }
}
