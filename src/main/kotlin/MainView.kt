package com.dmdirc

import com.jukusoft.i18n.I.tr
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.transformation.SortedList
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.util.StringConverter
import tornadofx.*

class MainView : View() {
    private val controller: MainController by inject()
    private val windowProperty = SimpleObjectProperty<Node>()
    override val root =
        borderpane {
            maxHeight = Double.MAX_VALUE
            maxWidth = Double.MAX_VALUE
            top = menubar {
                menu(tr("File")) {
                    item(tr("Quit")) {
                        action {
                            close()
                        }
                    }
                }
                menu(tr("IRC")) {
                    item(tr("Server List")) {
                        action {
                            ServerListController(controller).create()
                        }
                    }
                    item(tr("Join")) {
                        action {
                            JoinDialogController(controller).create()
                        }
                        enableWhen(controller.selectedWindow.isNotNull)
                    }
                }
                menu(tr("Settings")) {
                    item(tr("Settings")) {
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
                    styleClass.removeIf { s -> s.startsWith("node-") }
                    styleClass.add("node-${it.type.name.toLowerCase()}")
                }

                prefWidth = 148.0
                contextmenu {
                    item(tr("Close")) {
                        action {
                            selectedItem?.let {
                                if (it.isConnection) {
                                    it.connection?.disconnect()
                                } else {
                                    controller.leaveChannel(it.name)
                                }
                            }
                        }
                    }
                }
            }
            centerProperty().bindBidirectional(windowProperty)
            addStageIcon(Image(resources.stream("/logo.png")))
            titleProperty.bindBidirectional(controller.selectedWindow, TitleStringConverter())
        }

    init {
        controller.selectedWindow.addListener(ChangeListener { _, _, newValue ->
            windowProperty.value = controller.windowUis[newValue]?.root ?: vbox {}
        })
    }
}

class TitleStringConverter : StringConverter<WindowModel>() {

    override fun fromString(string: String?) = TODO("not implemented")

    override fun toString(window: WindowModel?) = when {
        window == null -> tr("DMDirc")
        window.isConnection -> tr("DMDirc: %s").format(window.connection?.networkName ?: "")
        else -> tr("DMDirc: %s | %s").format(window.name, window.connection?.networkName ?: "")
    }

}
