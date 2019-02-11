package com.dmdirc

import javafx.scene.control.TreeItem
import javafx.scene.image.Image
import javafx.scene.layout.Priority
import tornadofx.*
import tornadofx.controlsfx.statusbar

class MainView : View() {
    private val controller: MainController by inject()
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
                        root = TreeItem(controller.root)
                        isFitToHeight = true
                        onUserSelect {selected ->
                            when (selected.type) {
                                WindowType.SERVER -> {
                                    center = controller.root.children.find {
                                        it.type == WindowType.SERVER && it.connection == selected.connection
                                    }?.windowUI?.root ?: vbox {}
                                }
                                WindowType.CHANNEL -> {
                                    center = controller.root.children.find {
                                        it.type == WindowType.SERVER && it.connection == selected.connection
                                    }?.children?.find {
                                        selected.name == it.name
                                    }?.windowUI?.root ?: vbox {}
                                }
                                else -> {}
                            }
                        }
                        bindSelected(controller.selectedChannel)
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
            center = vbox {
            }
            bottom = statusbar {
                text = ""
            }
            addStageIcon(Image(resources.stream("/logo.png")))
        }
}