package com.dmdirc

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonBar
import tornadofx.*

data class ConnectionDetails(val hostname: String, val password: String, val port: Int)

class ConnectionDetailsModel : ItemViewModel<ConnectionDetails>() {
    val KEY_HOSTNAME = "hostname"
    val KEY_PORT = "port"
    val KEY_PASSWORD = "password"
    val KEY_REMEMBER = "remember"

    val hostname = bind { SimpleStringProperty(item?.hostname, null, app.config.string(KEY_HOSTNAME)) }
    val password = bind { SimpleStringProperty(item?.password, null, app.config.string(KEY_PASSWORD)) }
    val port = bind { SimpleIntegerProperty(item?.port, null, app.config.int(KEY_PORT, 6667)!!) }
    val remember = SimpleBooleanProperty(config.boolean(KEY_REMEMBER) ?: false)

    override fun onCommit() {
        if (remember.value) {
            with(app.config) {
                set(KEY_HOSTNAME to hostname.value)
                set(KEY_PORT to port.value)
                if (password.value.isNotEmpty()) {
                    set(KEY_PASSWORD to password.value)
                }
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
                        password = model.password.value
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