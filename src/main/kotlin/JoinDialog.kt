package com.dmdirc

import tornadofx.*

class JoinDialog: Fragment() {
    private val controller: MainController by inject()
    override val root = form {
        fieldset {
            field("Channel Name") {
                textfield(controller.channelName)
            }
        }
        buttonbar {
            button("Connect") {
                isDefaultButton = true
                enableWhen {
                    controller.isConnected.and(controller.channelName.isNotEmpty)
                }
                action {
                    controller.joinChannel(
                            name=controller.channelName.value
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