package com.dmdirc

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import org.kodein.di.generic.instance
import tornadofx.*

class ServerListController(private val controller: MainController) {
    private var model: ServerListModel? = null
    fun create() {
        val model = ServerListModel(this)
        this.model = model
        ServerlistDialog(model).openModal()
    }

    fun connect(server: ConnectionDetails) {
        controller.connect(server)
    }
}

class ServerListModel(private val controller: ServerListController) : ItemViewModel<ConnectionDetails>() {
    val open = SimpleBooleanProperty(true)
    private val config1 by kodein.instance<ClientConfig>()

    val servers = config1[ClientSpec.servers].toMutableList().observable()
    val hostname = bind(ConnectionDetails::hostname)
    val password = bind(ConnectionDetails::password)
    val port = bind(ConnectionDetails::port)
    val tls = bind(ConnectionDetails::tls)
    val autoconnect = bind(ConnectionDetails::autoconnect)

    override fun onCommit() {
        config1[ClientSpec.servers] = servers
        config1.save()
    }

    fun join(server: ConnectionDetails) {
        controller.connect(server)
    }

    fun closeDialog() {
        open.value = false
    }
}

class ServerlistDialog(private val model: ServerListModel) : Fragment() {
    override val root = form {
        borderpane {
            minWidth = 600.toDouble()
            minHeight = 400.toDouble()
            left = listview(model.servers) {
                bindSelected(model)
                if (model.servers.isNotEmpty()) {
                    selectionModel.select(model.servers[0])
                }
                cellFormat {
                    text = if (it.hostname.isEmpty()) {
                        "[Empty]"
                    } else {
                        it.hostname
                    }
                }
            }
            center = form {
                fieldset {
                    field("Server Name") {
                        textfield(model.hostname) {
                            enableWhen(!model.empty)
                            required()
                        }
                    }
                    field("Port") {
                        spinner(editable = true, property = model.port, min = 1, max = 65535) {
                            enableWhen(!model.empty)
                        }
                    }
                    field("Password") {
                        textfield(model.password) {
                            enableWhen(!model.empty)
                        }
                    }
                    field("TLS") {
                        checkbox(property = model.tls) {
                            enableWhen(!model.empty)
                        }
                    }
                    field("Auto Connect") {
                        checkbox(property = model.autoconnect) {
                            enableWhen(!model.empty)
                        }
                    }
                }
                hbox {
                    buttonbar {
                        alignment = Pos.BASELINE_RIGHT
                        button("Connect", ButtonBar.ButtonData.OK_DONE)
                        button("Reset", ButtonBar.ButtonData.CANCEL_CLOSE)
                    }
                }
            }
            bottom = buttonbar {
                button("Add", ButtonBar.ButtonData.LEFT) {
                    action {
                        model.servers.add(ConnectionDetails("", "", 6667, tls = false, autoconnect = false))
                    }
                }
                button("Delete", ButtonBar.ButtonData.LEFT) {
                    action {
                        model.servers.remove(model.item)
                    }
                }
                button("Save", ButtonBar.ButtonData.OK_DONE) {
                    enableWhen(model.dirty)
                    action {
                        model.commit()
                        close()
                    }
                }
                button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                    action {
                        model.rollback()
                        close()
                    }
                }
            }
        }
        model.open.addListener(ChangeListener { _, _, newValue -> if (!newValue) { close() }})
    }
}