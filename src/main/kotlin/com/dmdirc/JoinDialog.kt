package com.dmdirc

import com.jukusoft.i18n.I
import com.jukusoft.i18n.I.tr
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.Region
import javafx.scene.layout.VBox

object JoinDialogContract {
    interface Controller {
        fun join(channel: String)
    }

    interface ViewModel : ValidatingModel {
        val open: BooleanProperty
        val channel: StringProperty
        fun onTextAction()
        fun onJoinPressed()
        fun onCancelPressed()
    }
}

class JoinDialogController(private val controller: MainContract.Controller) : JoinDialogContract.Controller {
    override fun join(channel: String) {
        controller.joinChannel(channel)
    }
}

class JoinDialogModel(private val controller: JoinDialogContract.Controller) : JoinDialogContract.ViewModel {

    override val open = SimpleBooleanProperty(true)
    override val channel = SimpleStringProperty()
    override val valid = ValidatorChain()

    private fun commit() {
        if (!valid.value) {
            return
        }
        controller.join(channel.value)
        close()
    }

    private fun close() = open.set(false)

    override fun onTextAction() = commit()
    override fun onJoinPressed() = commit()
    override fun onCancelPressed() = close()
}

class JoinDialog(model: JoinDialogContract.ViewModel, private val parent: ObjectProperty<Node>) : VBox() {
    private val textfield = TextField()
    fun show() {
        parent.value = this
        runLater {
            textfield.requestFocus()
        }
    }

    init {
        model.open.addListener { _, _, newValue ->
            if (newValue == false) {
                parent.value = null
            }
        }
        styleClass.add("join-dialog")
        children.addAll(VBox().apply {
            children.addAll(VBox().apply {
                styleClass.add("dialog-background")
                children.addAll(Label(tr("Join Channel")).apply {
                    styleClass.add("dialog-header")
                }, Label(tr("Enter channel name: ")), textfield.apply {
                    bindRequiredTextControl(this, model.channel, model)
                    setOnAction { model.onTextAction() }
                }, ButtonBar().apply {
                    buttons.addAll(Button(I.tr("Join")).apply {
                        ButtonBar.setButtonData(this, ButtonBar.ButtonData.OK_DONE)
                        disableProperty().bind(model.valid.not())
                        setOnAction { model.onJoinPressed() }
                    }, Button(I.tr("Cancel")).apply {
                        ButtonBar.setButtonData(this, ButtonBar.ButtonData.CANCEL_CLOSE)
                        setOnAction { model.onCancelPressed() }
                    })
                })
                setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE)
            })
            alignment = Pos.TOP_CENTER
        })
    }
}
