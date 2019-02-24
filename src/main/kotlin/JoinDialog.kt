package com.dmdirc

import javafx.scene.control.ButtonBar
import tornadofx.*

data class JoinDetails(val channel: String)
object CancelEvent : FXEvent()
object CloseEvent : FXEvent()
class JoinEvent(val channelName: String) : FXEvent()

class JoinDialogController(private val controller: MainController) : Component() {
    fun create() {
        subscribe<JoinEvent>(times = 1) {
            controller.joinChannel(it.channelName)
            fire(CloseEvent)
        }
        subscribe<CancelEvent>(times = 1) {
            fire(CloseEvent)
        }
        find<JoinDialog>().openModal()
    }
}

class JoinDetailsModel : ItemViewModel<JoinDetails>() {
    val channel = bind(JoinDetails::channel)
    override fun onCommit() {
        if (isValid) {
            fire(JoinEvent(channel.value))
        }
    }
}

class JoinDialog : Fragment() {
    private val model = JoinDetailsModel()
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
                    fire(CancelEvent)
                }
            }
        }
    }

    init {
        subscribe<CloseEvent>(times = 1) {
            close()
        }
    }
}