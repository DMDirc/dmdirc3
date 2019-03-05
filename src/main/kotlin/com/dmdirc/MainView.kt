package com.dmdirc

import com.jukusoft.i18n.I.tr
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.collections.transformation.SortedList
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter

class ServerContextMenu(
    private val joinDialogProvider: () -> JoinDialog,
    private val controller: MainContract.Controller
) : ContextMenu() {
    init {
        items.addAll(
            MenuItem(tr("Join Channel")).apply {
                setOnAction {
                    joinDialogProvider().show()
                }
                disableProperty().bind(controller.selectedWindow.isNull)
            },
            MenuItem(tr("Disconnect")).apply {
                setOnAction {
                    controller.selectedWindow.value?.connection?.disconnect()
                }
            }
        )
    }
}

class ChannelContextMenu(
    private val controller: MainContract.Controller
) : ContextMenu() {
    init {
        items.add(
            MenuItem(tr("Close")).apply {
                setOnAction {
                    controller.leaveChannel(controller.selectedWindow.value.name.value)
                }
            }
        )
    }
}

class NodeListCellFactory(
    private val list: ListView<WindowModel>,
    private val joinDialogProvider: () -> JoinDialog,
    private val controller: MainContract.Controller
) : Callback<ListView<WindowModel>, ListCell<WindowModel>> {
    override fun call(param: ListView<WindowModel>?): ListCell<WindowModel> {
        return NodeListCell(list, joinDialogProvider, controller)
    }
}

class NodeListCell(
    list: ListView<WindowModel>,
    private val joinDialogProvider: () -> JoinDialog,
    private val controller: MainContract.Controller
) : ListCell<WindowModel>() {
    init {
        prefWidthProperty().bind(list.widthProperty())
        maxWidth = Control.USE_PREF_SIZE
    }

    override fun updateItem(node: WindowModel?, empty: Boolean) {
        super.updateItem(node, empty)
        if (node != null && !empty) {
            graphic = BorderPane().apply {
                contextMenu = ServerContextMenu(joinDialogProvider, controller)
                styleClass.add("node-${node.type.name.toLowerCase()}")
                if (node.hasUnreadMessages.value) {
                    styleClass.add("node-unread")
                }
                if (node.type == WindowType.SERVER) {
                    right = Label().apply {
                        styleClass.add("node-cog")
                        graphic = FontAwesomeIconView(FontAwesomeIcon.COG)
                        contextMenu = ServerContextMenu(joinDialogProvider, controller)
                        //TODO: This needs to work cross platform as expected
                        onMouseClicked = EventHandler {
                            if (it.button == MouseButton.PRIMARY) {
                                contextMenu.show(graphic, it.screenX, it.screenY)
                            }
                        }

                    }
                } else {
                    contextMenu = ChannelContextMenu(controller)
                }
                left = Label(node.title.value)
            }
            tooltip = Tooltip(node.title.value)
        }
        if (empty) {
            graphic = null
        }
    }
}

class MainView(
    private val controller: MainContract.Controller,
    val config: ClientConfig,
    private val joinDialogProvider: () -> JoinDialog,
    val settingsDialogProvider: () -> SettingsDialog,
    private val primaryStage: Stage,
    titleProperty: StringProperty
) : BorderPane() {
    private val selectedWindow = SimpleObjectProperty<Node>()

    init {
        top = MenuBar().apply {
            menus.addAll(
                Menu(tr("IRC")).apply {
                    items.addAll(
                        MenuItem(tr("Server List")).apply {
                            setOnAction {
                                ServerListController(controller, primaryStage, config).create()
                            }
                        }
                    )
                },
                Menu(tr("Settings")).apply {
                    items.add(
                        MenuItem(tr("Settings")).apply {
                            setOnAction {
                                settingsDialogProvider().show()
                            }
                        }
                    )
                }
            )
        }
        left = ListView(SortedList(controller.windows, compareBy { it.sortKey })).apply {
            styleClass.add("tree-view")
            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                controller.selectedWindow.value = newValue
            }
            cellFactory = NodeListCellFactory(this, joinDialogProvider, controller)
        }
        centerProperty().bindBidirectional(selectedWindow)
        primaryStage.icons.add(Image(MainView::class.java.getResourceAsStream("/logo.png")))
        titleProperty.bindBidirectional(controller.selectedWindow, TitleStringConverter())
        controller.selectedWindow.addListener { _, _, newValue ->
            selectedWindow.value = newValue?.let {
                it.connection?.children?.get(it.name.value)?.ui
            } ?: VBox()
        }
    }
}

class TitleStringConverter : StringConverter<WindowModel>() {

    override fun fromString(string: String?) = TODO("not implemented")

    override fun toString(window: WindowModel?): String = when {
        window == null -> tr("DMDirc")
        window.isConnection -> tr("DMDirc: %s").format(window.connection?.networkName ?: "")
        else -> tr("DMDirc: %s | %s").format(window.name.value, window.connection?.networkName ?: "")
    }

}
