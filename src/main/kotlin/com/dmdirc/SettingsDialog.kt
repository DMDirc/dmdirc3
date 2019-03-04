package com.dmdirc

import com.jukusoft.i18n.I.tr
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonBar.setButtonData
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.kodein.di.generic.instance

object SettingsDialogContract {
    interface Controller {
        fun save(nickname: String, realname: String, username: String)
    }

    interface ViewModel : ValidatingModel {
        val open: BooleanProperty
        val nickname: StringProperty
        val realname: StringProperty
        val username: StringProperty
        fun onSavePressed()
        fun onCancelPressed()
    }
}

class SettingsDialogController(private val config: ClientConfig) : SettingsDialogContract.Controller {

    override fun save(nickname: String, realname: String, username: String) {
        config[ClientSpec.DefaultProfile.nickname] = nickname
        config[ClientSpec.DefaultProfile.realname] = realname
        config[ClientSpec.DefaultProfile.username] = username
        config.save()
    }

}

class SettingsDialogModel(private val controller: SettingsDialogContract.Controller, config: ClientConfig) : SettingsDialogContract.ViewModel {
    override val valid = ValidatorChain()
    override val open = SimpleBooleanProperty(true)
    override val nickname = SimpleStringProperty(config[ClientSpec.DefaultProfile.nickname])
    override val realname = SimpleStringProperty(config[ClientSpec.DefaultProfile.realname])
    override val username = SimpleStringProperty(config[ClientSpec.DefaultProfile.username])

    override fun onSavePressed() {
        if (!valid.value) {
            return
        }
        controller.save(nickname.value, realname.value, username.value)
        close()
    }

    override fun onCancelPressed()  = close()

    private fun close() = open.set(false)
}

class SettingsDialog(model: SettingsDialogContract.ViewModel) : Stage() {
    init {
        val primaryStage by kodein.instance<Stage>()
        model.open.addListener { _, _, newValue ->
            if (newValue == false) {
                close()
            }
        }
        initOwner(primaryStage)
        initStyle(StageStyle.DECORATED)
        initModality(Modality.APPLICATION_MODAL)
        scene = Scene(GridPane().apply {
            add(Label(tr("Profile")), 0, 0, 2, 1)
            add(Label(tr("Nickname: ")), 0, 1)
            add(TextField().apply {
                bindRequiredTextControl(this, model.nickname, model)
            }, 1, 1)
            add(Label(tr("Realname: ")), 0, 2)
            add(TextField().apply {
                bindRequiredTextControl(this, model.realname, model)
            }, 1, 2)
            add(Label(tr("Username: ")), 0, 3)
            add(TextField().apply {
                bindRequiredTextControl(this, model.username, model)
            }, 1, 3)
            add(ButtonBar().apply {
                buttons.addAll(
                    Button(tr("Save")).apply {
                        setButtonData(this, ButtonBar.ButtonData.OK_DONE)
                        setOnAction {
                            disableProperty().bind(model.valid.not())
                            model.onSavePressed()
                        }
                    }
                    ,
                    Button(tr("Close")).apply {
                        setButtonData(this, ButtonBar.ButtonData.CANCEL_CLOSE)
                        setOnAction {
                            model.onCancelPressed()
                        }
                    }
                )
            }, 0, 4, 2, 1)
        })
    }
}