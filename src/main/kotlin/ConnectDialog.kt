package com.dmdirc

import tornadofx.*

class ConnectDialog : Fragment() {
    private val controller: MainController by inject()
    override val root = form {
        fieldset {
            field("Server Name") {
                textfield(controller.connectHostname)
            }
            field("Port") {
                spinner(editable = true, property = controller.connectPort, min = 1, max = Integer.MAX_VALUE)
            }
            field("Password") {
                textfield(controller.connectPassword)
            }
            field("Save Defaults") {
                checkbox(property = controller.connectSave)
            }
        }
        buttonbar {
            button("Connect") {
                isDefaultButton = true
                enableWhen {
                    controller.connectHostname.isNotEmpty
                }
                action {
                    if (controller.connectSave.value) {
                        preferences("dmdirc3") {
                            put("connecthost", controller.connectHostname.value)
                            putInt("connectport", controller.connectPort.value)
                            put("connectpassword", controller.connectPassword.value)
                        }
                    }
                    controller.connect(
                        host = controller.connectHostname.value ?: "",
                        port = controller.connectPort.value ?: 6667,
                        password = controller.connectPassword.value
                    )
                    close()
                }
            }
            button("Cancel") {
                isCancelButton = true
                action {
                    close()
                }
            }
        }
    }
}