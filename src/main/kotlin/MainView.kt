package com.dmdirc

import javafx.collections.transformation.SortedList
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
                    item("Server List") {
                        action {
                            find<ServerlistDialog>().openModal()
                        }
                    }
                    item("Connect") {
                        action {
                            find<ConnectDialog>().openModal()
                        }
                    }
                    item("Join") {
                        action {
                            find<JoinDialog>().openModal()
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
            left = vbox {
                scrollpane {
                    listview(SortedList(controller.windows, compareBy { it.path })) {
                        isFitToHeight = true
                        bindSelected(controller.selectedWindow)
                        cellFormat {
                            text = when (it.type) {
                                WindowType.SERVER -> "${it.name} [${it.connection?.networkName ?: ""}]"
                                WindowType.CHANNEL -> "\t${it.name}"
                                else -> it.name
                            }
                        }
                        controller.selectedWindow.addListener(ChangeListener { _, _, newValue ->
                                center = newValue.windowUI.root
                            }
                        )
                        prefWidth = 148.0
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
            center = vbox {
            }
            bottom = statusbar {
                text = ""
            }
            addStageIcon(Image(resources.stream("/logo.png")))
        }
}