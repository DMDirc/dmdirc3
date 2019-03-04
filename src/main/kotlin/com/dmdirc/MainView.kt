package com.dmdirc

import com.jukusoft.i18n.I.tr
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.collections.transformation.SortedList
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter

class NodeListCellFactory : Callback<ListView<WindowModel>, ListCell<WindowModel>> {
    override fun call(param: ListView<WindowModel>?): ListCell<WindowModel> {
        return NodeListCell()
    }
}

class NodeListCell : ListCell<WindowModel>() {
    override fun updateItem(node: WindowModel?, empty: Boolean) {
        super.updateItem(node, empty)
        if (node != null && !empty) {
            styleClass.removeIf { s ->
                s.startsWith("node-")
            }
            styleClass.add("node-${node.type.name.toLowerCase()}")
            text = when (node.type) {
                WindowType.SERVER -> "${node.name} [${node.connection?.networkName ?: ""}]"
                else -> node.name
            }
        }
        if (empty) {
            text = ""
            graphic = null
            styleClass.removeIf { s ->
                s.startsWith("node-")
            }
        }
    }
}

class MainView(
    private val controller: MainContract.Controller,
    val config: ClientConfig,
    val joinDialogProvider: () -> JoinDialog,
    val settingsDialogProvider: () -> SettingsDialog,
    private val primaryStage: Stage,
    titleProperty: StringProperty
) : BorderPane() {
    private val selectedWindow = SimpleObjectProperty<Node>()

    init {
        top = MenuBar().apply {
            menus.addAll(
                Menu(tr("File")).apply {
                    items.add(
                        MenuItem(tr("Quit")).apply {
                            setOnAction {
                                primaryStage.close()
                            }
                        }
                    )
                },
                Menu(tr("IRC")).apply {
                    items.addAll(
                        MenuItem(tr("Server List")).apply {
                            setOnAction {
                                ServerListController(controller, primaryStage, config).create()
                            }
                        },
                        MenuItem(tr("Join Channel")).apply {
                            setOnAction {
                                joinDialogProvider().show()
                            }
                            disableProperty().bind(controller.selectedWindow.isNull)
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
            cellFactory = NodeListCellFactory()
        }
        centerProperty().bindBidirectional(selectedWindow)
        primaryStage.icons.add(Image(MainView::class.java.getResourceAsStream("/logo.png")))
        titleProperty.bindBidirectional(controller.selectedWindow, TitleStringConverter())
        controller.selectedWindow.addListener { _, _, newValue ->
            selectedWindow.value = newValue?.let {
                it.connection?.children?.get(it.name)?.ui
            } ?: VBox()
        }
    }
}

class TitleStringConverter : StringConverter<WindowModel>() {

    override fun fromString(string: String?) = TODO("not implemented")

    override fun toString(window: WindowModel?): String = when {
        window == null -> tr("DMDirc")
        window.isConnection -> tr("DMDirc: %s").format(window.connection?.networkName ?: "")
        else -> tr("DMDirc: %s | %s").format(window.name, window.connection?.networkName ?: "")
    }

}
