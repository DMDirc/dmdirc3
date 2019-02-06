package com.dmdirc

import javafx.scene.control.ButtonBar
import tornadofx.*

data class JoinDetails(val channel: String)

class JoinDetailsModel : ItemViewModel<JoinDetails>() {
    private val controller: MainController by inject()
    val channel = bind(JoinDetails::channel)
    override fun onCommit() {
        controller.joinChannel(channel.value)
    }
}

class JoinDialog: Fragment() {
    private val model = JoinDetailsModel()
    override val root = form {
        fieldset {
            field("Channel Name") {
                textfield(model.channel).apply {
                    action {
                        if (model.isValid) {
                            model.commit()
                            close()
                        }
                    }
                }.required()
            }
        }
        buttonbar {
            button("Join", ButtonBar.ButtonData.OK_DONE) {
                enableWhen(model.valid)
                action {
                    model.commit()
                    close()
                }
            }
            button("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE) {
                action {
                    close()
                }
            }
        }
    }
}