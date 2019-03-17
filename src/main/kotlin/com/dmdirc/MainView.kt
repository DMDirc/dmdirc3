package com.dmdirc

import com.jukusoft.i18n.I.tr
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.animation.ScaleTransition
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ContextMenu
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.Tooltip
import javafx.scene.effect.GaussianBlur
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.Duration
import javafx.util.StringConverter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServerContextMenu(
    private val mainView: MainView,
    private val joinDialogProvider: (MainView) -> JoinDialog,
    private val connection: ConnectionContract.Controller?
) : ContextMenu() {
    private val joinChannel = MenuItem(tr("Join Channel"))
    private val disconnect = MenuItem(tr("Disconnect"))
    private val reconnected = MenuItem()

    override fun show() {
        val connected = connection?.connected?.value ?: false
        disconnect.visibleProperty().value = connected
        joinChannel.visibleProperty().value = connected
        reconnected.text = if (connected) tr("Reconnect") else tr("Connect")
        super.show()
    }

    init {
        items.addAll(joinChannel.apply {
            setOnAction {
                joinDialogProvider(mainView).show()
            }
        }, disconnect.apply {
            setOnAction {
                if (connection?.connected?.value == true) {
                    connection.disconnect()
                }
            }
        }, reconnected.apply {
            setOnAction {
                GlobalScope.launch {
                    if (connection?.connected?.value == true) {
                        connection.disconnect()
                    }
                    delay(500)
                    if (connection?.connected?.value == false) {
                        connection.connect()
                    }
                }
            }
        }, MenuItem(tr("Close")).apply {
            setOnAction {
                if (connection?.connected?.value == true) {
                    connection.disconnect()
                }
                connection?.children?.clear()
            }
        })
    }
}

class ChannelContextMenu(
    private val controller: MainContract.Controller
) : ContextMenu() {
    init {
        items.add(MenuItem(tr("Close")).apply {
            setOnAction {
                controller.leaveChannel(controller.selectedWindow.value.name.value)
            }
        })
    }
}

class NodeListCellFactory(
    private val mainView: MainView,
    private val list: ListView<WindowModel>,
    private val joinDialogProvider: (MainView) -> JoinDialog,
    private val controller: MainContract.Controller
) : Callback<ListView<WindowModel>, ListCell<WindowModel>> {
    override fun call(param: ListView<WindowModel>?): ListCell<WindowModel> {
        return NodeListCell(mainView, list, joinDialogProvider, controller)
    }
}

class NodeListCell(
    private val mainView: MainView,
    list: ListView<WindowModel>,
    private val joinDialogProvider: (MainView) -> JoinDialog,
    private val controller: MainContract.Controller
) : ListCell<WindowModel>() {
    init {
        prefWidthProperty().bind(list.widthProperty().subtract(20))
        maxWidth = Control.USE_PREF_SIZE
    }

    override fun updateItem(node: WindowModel?, empty: Boolean) {
        super.updateItem(node, empty)
        if (node != null && !empty) {
            graphic = StackPane().apply {
                contextMenu = ServerContextMenu(mainView, joinDialogProvider, item.connection)
                styleClass.add("node-${node.type.name.toLowerCase()}")
                if (node.connection?.connected?.value == false) {
                    styleClass.add("node-disconnected")
                }
                node.unreadStatus.value?.let {
                    styleClass.add("node-unread")
                    styleClass.add("node-unread-${it.name}")
                }
                if (node.type == WindowType.SERVER) {
                    children.add(Label().apply {
                        StackPane.setAlignment(this, CENTER_RIGHT)
                        styleClass.add("node-cog")
                        graphic = FontAwesomeIconView(FontAwesomeIcon.COG)
                        contextMenu = ServerContextMenu(mainView, joinDialogProvider, item.connection)
                        onMouseClicked = EventHandler {
                            if (!it.isPopupTrigger) {
                                contextMenu.show(graphic, it.screenX, it.screenY)
                            }
                        }
                    })
                } else {
                    contextMenu = ChannelContextMenu(controller)
                }
                children.add(Label(node.title.value).apply {
                    StackPane.setAlignment(this, CENTER_LEFT)
                })
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
    private val joinDialogProvider: (MainView) -> JoinDialog,
    val settingsDialogProvider: (MainView) -> SettingsDialog,
    val serverlistDialogProvider: (MainView) -> ServerlistDialog,
    private val primaryStage: Stage,
    titleProperty: StringProperty,
    private val dialogPane: ObjectProperty<Node>,
    welcomePaneProvider: () -> WelcomePane
) : StackPane() {
    private val selectedWindow = SimpleObjectProperty<Node>()
    private val fadeIn = ScaleTransition(Duration.millis(100.0)).apply {
        fromX = 0.0
        toX = 1.0
        fromY = 0.0
        toY = 1.0
        cycleCount = 1
    }
    private val fadeOut = ScaleTransition(Duration.millis(100.0)).apply {
        fromX = 1.0
        toX = 0.0
        fromY = 1.0
        toY = 0.0
        cycleCount = 1
    }

    init {
        val ui = BorderPane()
        children.addAll(ui.apply {
            centerProperty().bindBidirectional(selectedWindow)
            top = MenuBar().apply {
                menus.addAll(Menu(tr("IRC")).apply {
                    items.addAll(MenuItem(tr("Server List")).apply {
                        setOnAction {
                            serverlistDialogProvider(this@MainView).show()
                        }
                    })
                }, Menu(tr("Settings")).apply {
                    items.add(MenuItem(tr("Settings")).apply {
                        setOnAction {
                            settingsDialogProvider(this@MainView).show()
                        }
                    })
                })
            }
            left = ListView(controller.windows).apply {
                isFocusTraversable = false
                styleClass.add("tree-view")
                selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                    controller.selectedWindow.value = newValue
                }
                controller.selectedWindow.addListener { _, _, newValue ->
                    selectionModel.select(newValue)
                }
                selectionModel.select(controller.selectedWindow.value)
                cellFactory = NodeListCellFactory(this@MainView, this, joinDialogProvider, controller)
            }
        }, BorderPane().apply {
            top = VBox().apply { minHeightProperty().bind(primaryStage.heightProperty().multiply(0.1)) }
            bottom = VBox().apply { minHeightProperty().bind(primaryStage.heightProperty().multiply(0.1)) }
            left = VBox().apply { minWidthProperty().bind(primaryStage.widthProperty().multiply(0.1)) }
            right = VBox().apply { minWidthProperty().bind(primaryStage.widthProperty().multiply(0.1)) }
            centerProperty().bindBidirectional(dialogPane)
            isVisible = false
            centerProperty().addListener { _, _, newValue ->
                isVisible = newValue != null
            }
            visibleProperty().bindTransform(ui.effectProperty()) { _, b ->
                if (b == true) {
                    GaussianBlur(5.0)
                } else {
                    null
                }
            }
        })
        primaryStage.icons.add(Image(MainView::class.java.getResourceAsStream("/logo.png")))
        titleProperty.bindBidirectional(controller.selectedWindow, TitleStringConverter())
        selectedWindow.value = controller.selectedWindow.value?.let {
            it.connection?.children?.get(it.name.value)?.ui
        } ?: welcomePaneProvider.invoke()
        controller.selectedWindow.addListener { _, oldValue, newValue ->
            runLater {
                oldValue?.unreadStatus?.value = null
                newValue?.unreadStatus?.value = null
                selectedWindow.value = newValue?.let {
                    it.connection?.children?.get(it.name.value)?.ui
                } ?: VBox()
            }
        }
    }

    fun showDialog(dialog: Node) {
        dialogPane.value = dialog
        fadeIn.node = dialog
        fadeIn.playFromStart()
    }

    fun hideDialog() {
        fadeOut.node = dialogPane.value
        fadeOut.playFromStart()
        fadeOut.onFinishedProperty().value = EventHandler {
            dialogPane.value = null
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

class WelcomePane(
    controller: MainContract.Controller,
    settingsDialogProvider: () -> SettingsDialog,
    serverlistDialogProvider: () -> ServerlistDialog,
    version: String
) : VBox() {
    init {
        styleClass.add("welcome")
        children.addAll(Label(tr("Welcome to DMDirc %s").format(version)).apply {
            styleClass.add("welcome-header")
        }, Label(
            tr(
                "To get started you'll need to set your nickname and other settings and add a server or two, you can do this with the buttons below. If you'd rather just dive in, click the \"Chat with us\" button and you'll connect to our development channel with some default settings."
            )
        ).apply {
            styleClass.add("welcome-text")
            isWrapText = true
            prefWidth = 500.0
        }, VBox().apply {
            children.addAll(Button(tr("Profile")).apply {
                maxWidth = Double.MAX_VALUE
                setOnAction {
                    settingsDialogProvider().show()
                }
            }, Button(tr("Server list")).apply {
                maxWidth = Double.MAX_VALUE
                setOnAction {
                    serverlistDialogProvider().show()
                }
            }, Button(tr("Chat with us")).apply {
                maxWidth = Double.MAX_VALUE
                setOnAction {
                    controller.joinDev()
                }
            })
            alignment = Pos.CENTER
            spacing = 5.0
            minWidth = 150.0
            maxWidth = 150.0
        })
        spacing = 5.0
        alignment = Pos.CENTER
    }
}
