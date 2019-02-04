package com.dmdirc

import javafx.scene.image.Image
import javafx.scene.layout.Priority
import org.fxmisc.flowless.VirtualizedScrollPane
import tornadofx.*
import tornadofx.controlsfx.statusbar

class MainView : View() {
    private val controller: MainController by inject()
    private val users = controller.users
    private val channels = controller.channels
    private val inputText = controller.inputText
    private val textArea = controller.textArea
    override val root =
            borderpane {
                maxHeight = Double.MAX_VALUE
                maxWidth = Double.MAX_VALUE
                top = menubar {
                    menu("File") {
                        item("Connect") {
                            action {
                                find<ConnectDialog>().openModal()
                            }
                        }
                        item("Quit") {
                            action {
                                close()
                            }
                        }
                    }
                }
                left = vbox {
                    scrollpane {
                        listview(channels) {
                            isFitToHeight = true
                        }
                        vboxConstraints {
                            vgrow = Priority.ALWAYS
                            hgrow = Priority.ALWAYS
                        }
                    }
                }
                center = hbox {
                    vbox {
                        val moo = VirtualizedScrollPane(textArea)
                        add(moo)
                        textArea.isEditable = false
                        textArea.isWrapText = true
                        textArea.insertText(0, "")
                        moo.vgrow = Priority.ALWAYS
                        textfield(inputText) {
                            action {
                                if (inputText.value.isNotEmpty()) {
                                    runAsync {
                                        controller.sendMessage(inputText.value)
                                        inputText.value = ""
                                    }
                                }
                            }
                        }
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
                }
                bottom = statusbar {
                    text = ""
                }
                //controller.connect()
                addStageIcon(Image(resources.stream("/logo.png")))
            }
}