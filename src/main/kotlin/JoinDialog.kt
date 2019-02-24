package com.dmdirc

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.ButtonBar
import tornadofx.*

data class JoinDetails(val channel: String)

class JoinDialogController(private val controller: MainController) {
    private var model: JoinDetailsModel? = null
    fun create() {
        val model = JoinDetailsModel(this)
        this.model = model
        JoinDialog(model).openModal()
    }
    fun join(channel: String) {
        controller.joinChannel(channel)
        model?.closeDialog()
    }
}

class JoinDetailsModel(private val controller: JoinDialogController) : ItemViewModel<JoinDetails>() {
    val channel = bind(JoinDetails::channel)
    val open = SimpleBooleanProperty(true)
    override fun onCommit() {
        if (isValid) {
            controller.join(channel.value)
        }
    }
    fun closeDialog() {
        open.value = false
    }
}

class JoinDialog(private val model: JoinDetailsModel) : Fragment() {
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
        model.open.addListener(ChangeListener { _, _, newValue -> if (!newValue) { close() }})
    }
}