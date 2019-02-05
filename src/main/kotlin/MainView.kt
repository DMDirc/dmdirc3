package com.dmdirc

import javafx.scene.control.TreeItem
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import org.fxmisc.flowless.VirtualizedScrollPane
import tornadofx.*
import tornadofx.controlsfx.statusbar

class MainView : View() {
    private val controller: MainController by inject()
    private val users = controller.users
    private val channels = controller.channels
    private val selectedChannel = controller.selectedChannel
    private val inputText = controller.inputText
    private val textArea = controller.textArea
    private val document = textArea.document
    override val root =
        borderpane {
            maxHeight = Double.MAX_VALUE
            maxWidth = Double.MAX_VALUE
            top = menubar {
                menu("File") {
                    item("Quit") {
                        action {
                            close()
                        }
                    }
                }
                menu("IRC") {
                    item("Connect") {
                        action {
                            find<ConnectDialog>().openModal()
                        }
                    }
                    item("Join") {
                        action {
                            find<JoinDialog>().openModal()
                        }
                    }
                }
                menu("Settings") {
                    item("Settings") {
                        action {
                            find<SettingsDialog>().openModal()
                        }
                    }
                }
            }
            left = vbox {
                scrollpane {
                    treeview<Window> {
                        isShowRoot = false
                        root = TreeItem(channels)
                        isFitToHeight = true
                        bindSelected(selectedChannel)
                        populate {
                            it.value.children
                        }
                        cellFormat {
                            text = it.name
                        }
                    }
                    vboxConstraints {
                        vgrow = Priority.ALWAYS
                        hgrow = Priority.ALWAYS
                    }
                }
            }
            center = hbox {
                vbox {
                    add(VirtualizedScrollPane(textArea.apply {
                        isEditable = false
                        isWrapText = true
                    }).apply {
                        vgrow = Priority.ALWAYS
                    })
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
            addStageIcon(Image(resources.stream("/logo.png")))
        }
}