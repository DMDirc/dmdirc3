package com.dmdirc

import com.jukusoft.i18n.I.tr
import javafx.application.Platform.runLater
import javafx.beans.property.ReadOnlyBooleanProperty
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
import org.controlsfx.validation.ValidationSupport
import org.controlsfx.validation.Validator
import tornadofx.enableWhen

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

class JoinDetailsModel(private val controller: JoinDialogController) {
    val channel = SimpleStringProperty()
    val open = SimpleBooleanProperty(true)
    val channelValidator = Validator.createEmptyValidator<TextField>("Required")
    val validated = emptyList<ReadOnlyBooleanProperty>().toMutableList()
    val validated1 = SimpleBooleanProperty(false)

    init {
        validated1.addListener { observable, oldValue, newValue ->
            println("New Value: $newValue")
        }
    }

    fun commit() {
        if (validated.any {
                it.value == true
            }) {
            return
        }
        controller.join(channel.value)
        closeDialog()
    }

    fun closeDialog() {
        open.value = false
    }

    fun isValid() = validated1
}

class JoinDialog(model: JoinDetailsModel): Stage() {
    init {
        model.open.addListener { _, _, newValue -> if (newValue == false) { close() } }
        initStyle(StageStyle.DECORATED)
        initModality(Modality.APPLICATION_MODAL)
        scene = Scene(VBox().apply {
            children.addAll(
                TextField().apply {
                    val validationSupport = ValidationSupport()
                    validationSupport.registerValidator(this, model.channelValidator)
                    model.validated.add((validationSupport.invalidProperty()))
                    validationSupport.invalidProperty()
                    model.validated1.or(validationSupport.invalidProperty().not())
                    model.channel.bindBidirectional(textProperty())
                    setOnAction {
                        model.commit()
                    }
                }
                ,
                ButtonBar().apply {
                    buttons.addAll(
                        Button(tr("Join")).apply {
                            setButtonData(this, ButtonBar.ButtonData.OK_DONE)
                            enableWhen { model.isValid() }
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