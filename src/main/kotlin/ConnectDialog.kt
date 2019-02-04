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
                spinner(editable = true, property = controller.connectPort, max = Integer.MAX_VALUE)
            }
            field("Password") {
                textfield(controller.connectPassword)
            }
        }
        buttonbar {
            button("Connect") {
                isDefaultButton = true
                action {
                    controller.connect(
                            host=controller.connectHostname.value,
                            port=controller.connectPort.value,
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