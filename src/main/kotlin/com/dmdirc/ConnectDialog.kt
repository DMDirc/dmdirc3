package com.dmdirc

import com.jukusoft.i18n.I.tr
import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonBar.setButtonData
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Spinner
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.util.Callback

object ServerListDialogContract {
    interface Controller {
        fun connect(server: ConnectionDetailsEditable)
        fun save(servers: ObservableList<ConnectionDetailsEditable>)
    }

    interface ViewModel : ValidatingModel {
        fun connectPressed()
        fun addPressed()
        fun cancelPressed()
        fun savePressed()
        fun deletePressed()
        fun closeDialog()
        fun show()

        val open: BooleanProperty
        val servers: ObservableList<ConnectionDetailsEditable>
        val selected: ObjectProperty<ConnectionDetailsEditable>
        val hostname: StringProperty
        val password: StringProperty
        val editEnabled: BooleanProperty
        val port: IntegerProperty
        val tls: BooleanProperty
        val autoconnect: BooleanProperty
    }
}

class ConnectionDetailsEditable(
        var hostname: String,
        var password: String = "",
        var port: Int,
        var tls: Boolean = true,
        var autoconnect: Boolean = false
)

class ServerListController(
        private val controller: MainContract.Controller,
        private val config: ClientConfig
) : ServerListDialogContract.Controller {

    override fun connect(server: ConnectionDetailsEditable) {
        controller.connect(getConnectionDetails(server))
    }

    override fun save(servers: ObservableList<ConnectionDetailsEditable>) {
        config[ClientSpec.servers] = servers.map {
            getConnectionDetails(it)
        }
        config.save()
    }

    internal fun getConnectionDetails(server: ConnectionDetailsEditable) =
            ConnectionDetails(
                    server.hostname,
                    server.password,
                    server.port,
                    server.tls,
                    server.autoconnect
            )
}

class ServerListModel(
        private val controller: ServerListDialogContract.Controller,
        private val config: ClientConfig
) : ServerListDialogContract.ViewModel {
    override val valid = ValidatorChain()
    override val open = SimpleBooleanProperty(true)
    override val servers = emptyList<ConnectionDetailsEditable>().toMutableList().observable()
    override val selected = SimpleObjectProperty<ConnectionDetailsEditable>()
    override val hostname = SimpleStringProperty()
    override val password = SimpleStringProperty()
    override val port = SimpleIntegerProperty()
    override val tls = SimpleBooleanProperty()
    override val autoconnect = SimpleBooleanProperty()
    override val editEnabled = SimpleBooleanProperty()

    init {
        selected.addListener { _, oldValue, newValue ->
            if (oldValue != null) {
                oldValue.autoconnect = autoconnect.value ?: true
                oldValue.hostname = hostname.value ?: ""
                oldValue.password = password.value ?: ""
                oldValue.port = port.value ?: 6667
                oldValue.tls = tls.value ?: true
            }
            if (newValue != null) {
                autoconnect.value = newValue.autoconnect
                hostname.value = newValue.hostname
                password.value = newValue.password
                port.value = newValue.port
                tls.value = newValue.tls
            }
            editEnabled.value = (newValue != null)
        }
    }

    override fun show() {
        servers.addAll(config[ClientSpec.servers]
                .map { ConnectionDetailsEditable(it.hostname, it.password, it.port, it.tls, it.autoconnect) }
                .toMutableList().observable())
        if (servers.isNotEmpty()) {
            selected.value = servers.first()
        }
    }

    override fun closeDialog() {
        open.value = false
    }

    override fun addPressed() {
        servers.add(ConnectionDetailsEditable(
                hostname = "New Server",
                port = 6697,
                tls = true,
                autoconnect = false
        ).apply {
            selected.value = this
        })
    }

    override fun connectPressed() {
        if (selected.value != null) {
            controller.connect(selected.value)
        }
    }

    override fun deletePressed() {
        if (selected.value != null) {
            servers.remove(selected.value)
        }
    }

    override fun savePressed() {
        controller.save(servers)
        closeDialog()
    }

    override fun cancelPressed() {
        closeDialog()
    }
}

class ConnectionDetailsListCellFactory :
        Callback<ListView<ConnectionDetailsEditable>, ListCell<ConnectionDetailsEditable>> {
    override fun call(param: ListView<ConnectionDetailsEditable>?): ListCell<ConnectionDetailsEditable> {
        return ConnectionDetailsListCell()
    }
}

class ConnectionDetailsListCell : ListCell<ConnectionDetailsEditable>() {
    override fun updateItem(connectionDetails: ConnectionDetailsEditable?, empty: Boolean) {
        super.updateItem(connectionDetails, empty)
        text = connectionDetails?.hostname ?: ""
    }
}

class ServerlistDialog(
        private val model: ServerListDialogContract.ViewModel,
        private val parent: ObjectProperty<Node>
) : VBox() {
    fun show() {
        parent.value = this
        model.show()
    }

    init {
        model.open.addListener { _, _, newValue ->
            if (newValue == false) {
                parent.value = null
            }
        }
        styleClass.add("serverlist-dialog")
        children.addAll(
                VBox().apply {
                    styleClass.add("dialog-background")
                    children.addAll(
                            BorderPane().apply {
                                styleClass.add("serverlist-dialog")
                                top = Label(tr("Server List")).apply {
                                    styleClass.add("dialog-header")
                                }
                                left = ListView(model.servers).apply {
                                    cellFactory = ConnectionDetailsListCellFactory()
                                    model.selected.addListener { _, _, newValue ->
                                        selectionModel.select(model.servers.find { connectionDetailsEditable ->
                                            connectionDetailsEditable == newValue
                                        })
                                    }
                                    selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                                        model.selected.value = newValue
                                    }
                                }
                                center = BorderPane().apply {
                                    center = GridPane().apply {
                                        add(Label(tr("Server Details")).apply {
                                            styleClass.add("dialog-header")
                                        }, 0, 0, 2, 1)
                                        add(Label(tr("Server Name: ")), 0, 1)
                                        add(TextField().apply {
                                            bindRequiredTextControl(this, model.hostname, model)
                                            disableProperty().bind(model.editEnabled.not())
                                        }, 1, 1)
                                        add(Label(tr("Port: ")), 0, 2)
                                        add(Spinner<Number>(1, 65535, model.port.value).apply {
                                            model.port.bindBidirectional(valueFactory.valueProperty())
                                            disableProperty().bind(model.editEnabled.not())
                                        }, 1, 2)
                                        add(Label(tr("Password: ")), 0, 3)
                                        add(TextField().apply {
                                            model.password.bindBidirectional(this.textProperty())
                                            disableProperty().bind(model.editEnabled.not())
                                        }, 1, 3)
                                        add(Label(tr("TLS: ")), 0, 4)
                                        add(CheckBox().apply {
                                            model.tls.bindBidirectional(this.selectedProperty())
                                            disableProperty().bind(model.editEnabled.not())
                                        }, 1, 4)
                                        add(Label(tr("AutoConnect: ")), 0, 5)
                                        add(CheckBox().apply {
                                            model.autoconnect.bindBidirectional(this.selectedProperty())
                                            disableProperty().bind(model.editEnabled.not())
                                        }, 1, 5)
                                    }
                                    bottom = ButtonBar().apply {
                                        buttons.addAll(
                                                Button(tr("Connect")).apply {
                                                    setButtonData(this, ButtonBar.ButtonData.OK_DONE)
                                                    disableProperty().bind(model.editEnabled.not())
                                                    setOnAction {
                                                        model.connectPressed()
                                                    }
                                                }, Button(tr("Delete")).apply {
                                            setButtonData(this, ButtonBar.ButtonData.CANCEL_CLOSE)
                                            disableProperty().bind(model.editEnabled.not())
                                            setOnAction {
                                                model.deletePressed()
                                            }
                                        }
                                        )
                                    }
                                }
                                bottom = ButtonBar().apply {
                                    styleClass.add("serverlist-dialog-controls")
                                    buttons.addAll(
                                            Button(tr("Add")).apply {
                                                setButtonData(this, ButtonBar.ButtonData.LEFT)
                                                setOnAction {
                                                    model.addPressed()
                                                }
                                            },
                                            Button(tr("Save")).apply {
                                                setButtonData(this, ButtonBar.ButtonData.OK_DONE)
                                                setOnAction {
                                                    model.savePressed()
                                                }
                                            },
                                            Button(tr("Cancel")).apply {
                                                setButtonData(this, ButtonBar.ButtonData.CANCEL_CLOSE)
                                                setOnAction {
                                                    model.cancelPressed()
                                                }
                                            }
                                    )
                                }
                            }
                    )
                }
        )
    }
}