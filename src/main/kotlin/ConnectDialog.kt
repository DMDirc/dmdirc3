package com.dmdirc

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonBar
import tornadofx.*

class ConnectionDetailsModel : ItemViewModel<ConnectionDetails>() {
    private val keyHostname = "hostname"
    private val keyPort = "port"
    private val keyPassword = "password"
    private val keyRemember = "remember"
    private val keyTLS = "tls"

    val hostname = bind { SimpleStringProperty(item?.hostname, null, config.string(keyHostname)) }
    val password = bind { SimpleStringProperty(item?.password, null, config.string(keyPassword)) }
    val port = bind { SimpleIntegerProperty(item?.port, null, config.int(keyPort, 6667)) }
    val tls = bind { SimpleBooleanProperty(item?.tls, null, config.boolean(keyTLS, false)) }
    val remember = SimpleBooleanProperty(config.boolean(keyRemember) ?: false)

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
}

class ConnectDialog : Fragment() {
    private val controller: MainController by inject()
    private val model = ConnectionDetailsModel()
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
                action {
                    model.commit()
                    controller.connect(
                        host = model.hostname.value,
                        port = model.port.value.toInt(),
                        password = model.password.value,
                        tls = model.tls.value
                    )
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
}