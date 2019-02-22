package com.dmdirc

import javafx.beans.property.*
import javafx.scene.control.ButtonBar
import tornadofx.*

data class ConnectionDetails(val hostname: String, val password: String, val port: Int, val tls: Boolean)

interface ConnectionViewModel {
    val hostname: Property<String>
    val password: Property<String>
    val port: Property<Number>
    val tls: Property<Boolean>
    val remember: Property<Boolean>
    val valid: ReadOnlyBooleanProperty
    val closed: Property<Boolean>

    fun connectPressed()
    fun cancelPressed()
}

class ConnectionDetailsViewModel : ItemViewModel<ConnectionDetails>(), ConnectionViewModel {

    private val controller: MainController by inject()

    private val keyHostname = "hostname"
    private val keyPort = "port"
    private val keyPassword = "password"
    private val keyRemember = "remember"
    private val keyTLS = "tls"

    override val hostname = bind { SimpleStringProperty(item?.hostname, null, config.string(keyHostname)) }
    override val password = bind { SimpleStringProperty(item?.password, null, config.string(keyPassword)) }
    override val port = bind { SimpleIntegerProperty(item?.port, null, config.int(keyPort, 6667)) }
    override val tls = bind { SimpleBooleanProperty(item?.tls, null, config.boolean(keyTLS, false)) }
    override val remember = bind { SimpleBooleanProperty(config.boolean(keyRemember) ?: false) }
    override val closed = bind { SimpleBooleanProperty(false) }

    override fun onCommit() {
        if (remember.value) {
            with(config) {
                set(keyHostname to hostname.value)
                set(keyPort to port.value)
                if (password.value != null && password.value.isNotEmpty()) {
                    set(keyPassword to password.value)
                }
                set(keyTLS to tls.value)
                save()
            }
        }
    }

    override fun connectPressed() {
        controller.connect(
            host = hostname.value,
            port = port.value.toInt(),
            password = password.value,
            tls = tls.value
        )
        closed.value = true
    }

    override fun cancelPressed() {
        rollback()
        closed.value = true
    }

}

class ConnectDialog(model: ConnectionViewModel = ConnectionDetailsViewModel()) : Fragment() {
    override val root = form {
        fieldset {
            field("Server Name") {
                textfield(model.hostname).required()
            }
            field("Port") {
                spinner(editable = true, property = model.port, min = 1, max = 65535)
            }
            field("Password") {
                textfield(model.password)
            }
            field("TLS") {
                checkbox(property = model.tls)
            }
            field("Save Defaults") {
                checkbox(property = model.remember)
            }
        }
        buttonbar {

            button("Connect", ButtonBar.ButtonData.OK_DONE) {
                enableWhen(model.valid)
                action(model::connectPressed)
            }
            button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                action(model::cancelPressed)
            }
        }
    }

    init {
        model.closed.addListener { _, _, new -> if (new) { close() } }
    }
}