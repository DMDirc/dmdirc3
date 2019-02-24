package com.dmdirc

import javafx.collections.transformation.SortedList
import javafx.scene.image.Image
import tornadofx.*

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
                    item("Server List") {
                        action {
                            ServerListController(controller).create()
                        }
                    }
                    item("Join") {
                        action {
                            JoinDialogController(controller).create()
                        }
                        enableWhen(controller.selectedWindow.isNotNull)
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

            left = listview(SortedList(controller.windows, compareBy { it.sortKey })) {
                styleClass.add("tree-view")
                bindSelected(controller.selectedWindow)
                cellFormat {
                    text = when (it.type) {
                        WindowType.SERVER -> "${it.name} [${it.connection?.networkName ?: ""}]"
                        WindowType.CHANNEL -> it.name
                        else -> it.name
                    }
                    styleClass.removeIf { it.startsWith("node-") }
                    styleClass.add("node-${it.type.name.toLowerCase()}")
                }
                controller.selectedWindow.addListener(ChangeListener { _, _, newValue ->
                    center = newValue.windowUI.root
                })
                prefWidth = 148.0
                contextmenu {
                    item("Close") {
                        action {
                            selectedItem?.let {
                                if (!it.isConnection) {
                                    controller.leaveChannel(it.name)
                                } else {
                                    it.connection?.disconnect()
                                }
                            }
                        }
                    }
                }
            }
            center = vbox {
            }
            addStageIcon(Image(resources.stream("/logo.png")))
        }
}