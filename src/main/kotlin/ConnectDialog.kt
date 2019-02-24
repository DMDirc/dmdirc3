package com.dmdirc

import com.jukusoft.i18n.I.tr
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
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
    val hostname = SimpleStringProperty(hostname)
    val password = SimpleStringProperty(password)
    val port = SimpleIntegerProperty(port)
    val tls = SimpleBooleanProperty(tls)
    val autoconnect = SimpleBooleanProperty(autoconnect)
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
        ConnectionDetails(
            server.hostname.value,
            server.password.value,
            server.port.value,
            server.tls.value,
            server.autoconnect.value
        )
}

class ServerListModel(private val controller: ServerListController) : ItemViewModel<ConnectionDetailsEditable>() {
    val open = SimpleBooleanProperty(true)
    val servers = emptyList<ConnectionDetailsEditable>().toMutableList().observable()
    val hostname = bind(autocommit = true, defaultValue = "") { item?.hostname }
    val password = bind(autocommit = true, defaultValue = "") { item?.password }
    val port = bind(autocommit = true, defaultValue = 6697) { item?.port }
    val tls = bind(autocommit = true, defaultValue = true) { item?.tls }
    val autoconnect = bind(autocommit = true, defaultValue = false) { item?.autoconnect }

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
                    text = if (it.hostname.value.isEmpty()) {
                        tr("[Empty]")
                    } else {
                        it.hostname.value
                    }
                }
            }
            center = form {
                fieldset {
                    field(tr("Server Name")) {
                        textfield(model.hostname) {
                            enableWhen(!model.empty)
                            required()
                        }
                    }
                    field(tr("Port")) {
                        spinner(editable = true, property = model.port, min = 1, max = 65535) {
                            enableWhen(!model.empty)
                        }
                    }
                    field(tr("Password")) {
                        textfield(model.password) {
                            enableWhen(!model.empty)
                        }
                    }
                    field(tr("TLS")) {
                        checkbox(property = model.tls) {
                            enableWhen(!model.empty)
                        }
                    }
                    field(tr("Auto Connect")) {
                        checkbox(property = model.autoconnect) {
                            enableWhen(!model.empty)
                        }
                    }
                }
                hbox {
                    buttonbar {
                        alignment = Pos.BASELINE_RIGHT
                        button(tr("Connect"), ButtonBar.ButtonData.OK_DONE) {
                            action {
                                model.join(model.item)
                            }
                        }
                    }
                }
                bottom = buttonbar {
                    button(tr("Add"), ButtonBar.ButtonData.LEFT) {
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
                    button(tr("Delete"), ButtonBar.ButtonData.LEFT) {
                        action {
                            model.servers.remove(model.item)
                        }
                    }
                    button(tr("Save"), ButtonBar.ButtonData.OK_DONE) {
                        enableWhen(model.valid)
                        action {
                            model.commit()
                        }
                    }
                    button(tr("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE) {
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