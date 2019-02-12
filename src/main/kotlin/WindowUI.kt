package com.dmdirc

import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.StyleClassedTextArea
import tornadofx.*

class WindowUI(connection: Connection?) : View("Right bit") {
    private val controller: MainController by inject()
    val textArea = StyleClassedTextArea()
    private val inputText = SimpleStringProperty()
    val users = emptyList<String>().toMutableList().observable()

    lateinit var myConnection : Connection;

    init { connection?.let { myConnection = it } }

    override val root = borderpane {
        center = hbox {
            vbox {
                add(VirtualizedScrollPane(textArea.apply {
                    isEditable = false
                    isWrapText = true
                }).apply {
                    vgrow = Priority.ALWAYS
                })
                vboxConstraints {
                    vgrow = Priority.ALWAYS
                    hgrow = Priority.ALWAYS
                }
            }
        }
        right = vbox {
            scrollpane {
                listview(users) {
                    isFitToHeight = true
                }
                vboxConstraints {
                    vgrow = Priority.ALWAYS
                    hgrow = Priority.ALWAYS
                }
            }
            borderpaneConstraints {
                maxWidth = 150.00
            }
        }
        bottom = textfield(inputText) {
            action {
                if (inputText.value.isNotEmpty()) {
                    runAsync {
                        myConnection.sendMessage(controller.selectedChannel.value.name, inputText.value)
                        inputText.value = ""
                    }
                }
            }
        }
    }
}