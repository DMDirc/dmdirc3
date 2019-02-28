package com.dmdirc

import com.jukusoft.i18n.I.tr
import javafx.application.Platform.runLater
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonBar.setButtonData
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle

class JoinDialogController(private val controller: MainController) {
    fun create() {
        runLater {
            JoinDialog(JoinDetailsModel(this)).show()
        }
    }

    fun join(value: String) {
        controller.joinChannel(value)
    }
}

class JoinDetailsModel(private val controller: JoinDialogController): ValidatingModel {
    val open = SimpleBooleanProperty(true)
    val channel = SimpleStringProperty()
    private var validated: BooleanExpression = SimpleBooleanProperty(false)

    fun commit() {
        if (isValid().value.not()) {
            return
        }
        controller.join(channel.value)
        closeDialog()
    }

    fun closeDialog() {
        open.value = false
    }

    override fun addValidator(validator: BooleanExpression) {
        validated = validated.or(validator).not()
    }

    fun isValid() = validated
}

class JoinDialog(model: JoinDetailsModel): Stage() {
    init {
        model.open.addListener { _, _, newValue -> if (newValue == false) { close() } }
        initStyle(StageStyle.DECORATED)
        initModality(Modality.APPLICATION_MODAL)
        scene = Scene(VBox().apply {
            children.addAll(
                TextField().apply {
                    bindRequiredTextControl(this, model.channel, model)
                    setOnAction {
                        model.commit()
                    }
                }
                ,
                ButtonBar().apply {
                    buttons.addAll(
                        Button(tr("Join")).apply {
                            setButtonData(this, ButtonBar.ButtonData.OK_DONE)
                            disableProperty().bind(model.isValid().not())
                            setOnAction {
                                model.commit()
                            }
                        }
                        ,
                        Button(tr("Cancel")).apply {
                            setButtonData(this, ButtonBar.ButtonData.CANCEL_CLOSE)
                            setOnAction {
                                model.closeDialog()
                            }
                        }
                    )
                }
            )
        })
    }
}
