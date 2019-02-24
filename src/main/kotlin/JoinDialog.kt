package com.dmdirc

import javafx.scene.control.ButtonBar
import tornadofx.*

data class JoinDetails(val channel: String)

class JoinDialogController(private val controller: MainController) {
    private var dialog: JoinDialog? = null
    fun create() {
        dialog = JoinDialog(JoinDetailsModel(this))
        dialog?.openModal()
    }
    fun join(channel: String) {
        controller.joinChannel(channel)
        dialog?.close()
    }
}

class JoinDetailsModel(private val controller: JoinDialogController) : ItemViewModel<JoinDetails>() {
    val channel = bind(JoinDetails::channel)
    override fun onCommit() {
        if (isValid) {
            controller.join(channel.value)
        }
    }
}

class JoinDialog(model: JoinDetailsModel) : Fragment() {
    override val root = form {
        fieldset {
            field("Channel Name") {
                textfield(model.channel).apply {
                    action {
                        model.commit()
                    }
                }.required()
            }
        }
        buttonbar {
            button("Join", ButtonBar.ButtonData.OK_DONE) {
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
}