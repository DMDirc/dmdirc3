package com.dmdirc

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.ButtonBar.setButtonData
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Callback

class ConnectionDetailsEditable(
    var hostname: String,
    var password: String,
    var port: Int,
    var tls: Boolean = true,
    var autoconnect: Boolean = false
)

class ServerListController(private val controller: MainContract.Controller, private val stage: Stage, private val config: ClientConfig) {
    private var model: ServerListModel? = null
    fun create() {
        val model = ServerListModel(this)
        this.model = model
        ServerlistDialog(model, stage).show()
        model.servers.addAll(config[ClientSpec.servers]
            .map { ConnectionDetailsEditable(it.hostname, it.password, it.port, it.tls, it.autoconnect) }
            .toMutableList().observable())
        if (model.servers.isNotEmpty()) {
            model.selected.value = model.servers.first()
        }
    }

    fun connect(server: ConnectionDetailsEditable) {
        controller.connect(getConnectionDetails(server))
    }

    fun save(servers: List<ConnectionDetailsEditable>) {
        config[ClientSpec.servers] = servers.map {
            getConnectionDetails(it)
        }
        config.save()
        model?.closeDialog()
    }

    private fun getConnectionDetails(server: ConnectionDetailsEditable) =
        ConnectionDetails(
            server.hostname,
            server.password,
            server.port,
            server.tls,
            server.autoconnect
        )
}

class ServerListModel(private val controller: ServerListController) : ValidatingModel {
    override val valid = ValidatorChain()
    val open = SimpleBooleanProperty(true)
    val servers = emptyList<ConnectionDetailsEditable>().toMutableList().observable()
    val selected = SimpleObjectProperty<ConnectionDetailsEditable>()
    val hostname = SimpleStringProperty()
    val password = SimpleStringProperty()
    val port = SimpleIntegerProperty()
    val tls = SimpleBooleanProperty()
    val autoconnect = SimpleBooleanProperty()

    init {
        selected.addListener { _, oldValue, newValue ->
            if (oldValue != null) {
                oldValue.autoconnect = autoconnect.value
                oldValue.hostname = hostname.value
                oldValue.password = password.value
                oldValue.port = port.value
                oldValue.tls = tls.value
            }
            if (newValue != null) {
                autoconnect.value = newValue.autoconnect
                hostname.value = newValue.hostname
                password.value = newValue.password
                port.value = newValue.port
                tls.value = newValue.tls
            }
        }
    }

    fun closeDialog() {
        open.value = false
    }

    fun connectPressed() {
        controller.connect(selected.value)
    }

    fun deletePressed() {
        servers.remove(selected.value)
    }

    fun savePressed() {
        controller.save(servers)
        closeDialog()
    }

    fun cancelPressed() {
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

class ServerlistDialog(private val model: ServerListModel, primaryStage: Stage) : Stage() {
    init {
        model.open.addListener { _, _, newValue ->
            if (newValue == false) {
                close()
            }
        }
        initOwner(primaryStage)
        initStyle(StageStyle.DECORATED)
        initModality(Modality.APPLICATION_MODAL)
        scene = Scene(BorderPane().apply {
            left = ListView(model.servers).apply {
                cellFactory = ConnectionDetailsListCellFactory()
                selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                    model.selected.value = newValue
                }
                model.selected.addListener { _, _, newValue ->
                    selectionModel.select(model.servers.find {
                            connectionDetailsEditable -> connectionDetailsEditable == newValue
                    })
                }
            }
            center = BorderPane().apply {
                center = GridPane().apply {
                    add(Label("Server Name: "), 0, 0)
                    add(TextField().apply {
                        bindRequiredTextControl(this, model.hostname, model)
                    }, 1, 0)
                    add(Label("Port: "), 0, 1)
                    add(Spinner<Number>(1, 65535, model.port.value).apply {
                        model.port.bindBidirectional(valueFactory.valueProperty())
                    }, 1, 1)
                    add(Label("Password: "), 0, 2)
                    add(TextField().apply {
                        model.password.bindBidirectional(this.textProperty())
                    }, 1, 2)
                    add(Label("TLS: "), 0, 3)
                    add(CheckBox().apply {
                        model.tls.bindBidirectional(this.selectedProperty())
                    }, 1, 3)
                    add(Label("AutoConnect: "), 0, 4)
                    add(CheckBox().apply {
                        model.autoconnect.bindBidirectional(this.selectedProperty())
                    }, 1, 4)
                }
                bottom = ButtonBar().apply {
                    buttons.addAll(
                        Button("Connect").apply {
                            setButtonData(this, ButtonBar.ButtonData.OK_DONE)
                            setOnAction {
                                model.connectPressed()
                            }
                        }, Button("Delete").apply {
                            setButtonData(this, ButtonBar.ButtonData.CANCEL_CLOSE)
                            setOnAction {
                                model.deletePressed()
                            }
                        }
                    )
                }
            }
            bottom = ButtonBar().apply {
                buttons.addAll(
                    Button("Save").apply {
                        setButtonData(this, ButtonBar.ButtonData.OK_DONE)
                        setOnAction {
                            model.savePressed()
                        }
                    },
                    Button("Cancel").apply {
                        setButtonData(this, ButtonBar.ButtonData.CANCEL_CLOSE)
                        setOnAction {
                            model.cancelPressed()
                        }
                    }
                )
            }
        })
    }
}