package com.dmdirc

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import org.kodein.di.generic.instance
import tornadofx.*

class ConnectionDetailsEditable(
    hostname: String,
    password: String,
    port: Int,
    tls: Boolean = true,
    autoconnect: Boolean = false
) {
    var hostname: String by property(hostname)
    fun hostnameProperty() = getProperty(ConnectionDetailsEditable::hostname)
    var password: String by property(password)
    fun passwordProperty() = getProperty(ConnectionDetailsEditable::password)
    var port: Int by property(port)
    fun portProperty() = getProperty(ConnectionDetailsEditable::port)
    var tls: Boolean by property(tls)
    fun tlsProperty() = getProperty(ConnectionDetailsEditable::tls)
    var autoconnect: Boolean by property(autoconnect)
    fun autoconnectProperty() = getProperty(ConnectionDetailsEditable::autoconnect)
}

class ServerListController(private val controller: MainController) {
    private val config1 by kodein.instance<ClientConfig>()
    private var model: ServerListModel? = null
    fun create() {
        val model = ServerListModel(this)
        this.model = model
        ServerlistDialog(model).openModal()
        model.servers.addAll(config1[ClientSpec.servers]
            .map { ConnectionDetailsEditable(it.hostname, it.password, it.port, it.tls, it.autoconnect) }
            .toMutableList().observable())
    }

    fun connect(server: ConnectionDetailsEditable) {
        controller.connect(getConnectionDetails(server))
    }

    fun save(servers: List<ConnectionDetailsEditable>) {
        config1[ClientSpec.servers] = servers.map {
            getConnectionDetails(it)
        }
        config1.save()
        model?.closeDialog()
    }

    private fun getConnectionDetails(server: ConnectionDetailsEditable) =
        ConnectionDetails(server.hostname, server.password, server.port, server.tls, server.autoconnect)
}

class ServerListModel(private val controller: ServerListController) : ItemViewModel<ConnectionDetailsEditable>() {
    val open = SimpleBooleanProperty(true)
    val servers = emptyList<ConnectionDetailsEditable>().toMutableList().observable()
    val hostname = bind(autocommit = true) { item?.hostnameProperty() }
    val password = bind(autocommit = true) { item?.passwordProperty() }
    val port = bind(autocommit = true) { item?.portProperty() }
    val tls = bind(autocommit = true) { item?.tlsProperty() }
    val autoconnect = bind(autocommit = true) { item?.autoconnectProperty() }

    override fun onCommit() {
        controller.save(servers)
    }

    fun join(server: ConnectionDetailsEditable?) {
        if (server == null) return
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
                        button("Connect", ButtonBar.ButtonData.OK_DONE) {
                            action {
                                model.join(model.item)
                            }
                        }
                    }
                }
                bottom = buttonbar {
                    button("Add", ButtonBar.ButtonData.LEFT) {
                        action {
                            model.servers.add(
                                ConnectionDetailsEditable(
                                    hostname = "",
                                    password = "",
                                    port = 6667,
                                    tls = false,
                                    autoconnect = false
                                )
                            )
                        }
                    }
                    button("Delete", ButtonBar.ButtonData.LEFT) {
                        action {
                            model.servers.remove(model.item)
                        }
                    }
                    button("Save", ButtonBar.ButtonData.OK_DONE) {
                        enableWhen(model.valid)
                        action {
                            model.commit()
                        }
                    }
                    button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                        action {
                            close()
                        }
                    }
                }
            }
            model.open.addListener(ChangeListener { _, _, newValue ->
                if (!newValue) {
                    close()
                }
            })
        }
    }
}