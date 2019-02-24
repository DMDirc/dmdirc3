package com.dmdirc

import javafx.scene.control.ButtonBar
import tornadofx.*

data class JoinDetails(val channel: String)
object CancelEvent : FXEvent()
object CloseEvent : FXEvent()
class JoinEvent(val channelName: String) : FXEvent()

class JoinDialogController(private val controller: MainController) : Component() {
    init {
        subscribe<JoinEvent> {
            controller.joinChannel(it.channelName)
            fire(CloseEvent)
        }
        subscribe<CancelEvent> {
            fire(CloseEvent)
        }
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

class JoinDialog: Fragment() {
    private val mainController: MainController by inject()
    private val controller= JoinDialogController(mainController)
    private val model = JoinDetailsModel()
    override val root = form {
        fieldset {
            field("Channel Name") {
                textfield(model.channel).apply {
                    action {
                        if (model.isValid) {
                            model.commit()
                        }
                    }
                }.required()
            }
        }
        buttonbar {
            button("Join", ButtonBar.ButtonData.OK_DONE) {
                enableWhen(model.valid)
                action {
                    if (model.isValid) {
                        model.commit()
                    }
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
        subscribe<CloseEvent>(times=1) {
            close()
        }
    }
}