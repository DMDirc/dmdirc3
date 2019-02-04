package com.dmdirc

import tornadofx.*

class ConnectDialog: Fragment() {
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
        }
        buttonbar {
            button("Connect") {
                isDefaultButton = true
                enableWhen {
                    controller.connectHostname.isNotEmpty
                }
                action {
                    controller.connect(
                            host=controller.connectHostname.value ?: "",
                            port=controller.connectPort.value ?: 6667,
                            password=controller.connectPassword.value
                    )
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