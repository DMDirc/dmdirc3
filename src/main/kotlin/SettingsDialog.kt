package com.dmdirc

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonBar
import tornadofx.*

data class Settings(val nickname: String, val realname: String, val username: String)

class SettingsModel : ItemViewModel<Settings>() {
    val KEY_NICKNAME = "nickname"
    val KEY_REALNAME = "realname"
    val KEY_USERNAME = "username"

    val nickname = bind { SimpleStringProperty(item?.nickname, null, app.config.string(KEY_NICKNAME)) }
    val realname = bind { SimpleStringProperty(item?.realname, null, app.config.string(KEY_REALNAME)) }
    val username = bind { SimpleStringProperty(item?.username, null, app.config.string(KEY_USERNAME)) }

    override fun onCommit() {
        with(app.config) {
            set(KEY_NICKNAME to nickname.value)
            set(KEY_REALNAME to realname.value)
            set(KEY_USERNAME to username.value)
            save()
        }
    }
}

class SettingsDialog : Fragment() {
    private val model = SettingsModel()
    override val root = form {
        fieldset("Profile") {
            field("Nickname: ") {
                textfield(model.nickname).required()
            }
            field("Realname: ") {
                textfield(model.realname).required()
            }
            field("Username: ") {
                textfield(model.username).required()
            }
        }
        buttonbar {
            button("Close", ButtonBar.ButtonData.CANCEL_CLOSE).action {
                model.rollback()
                close()
            }
            button("Reset", ButtonBar.ButtonData.OTHER).action {
                model.rollback()
            }
            button("Save", ButtonBar.ButtonData.OK_DONE) {
                enableWhen(model.valid.and(model.dirty))
                action {
                    model.commit()
                    close()
                }
            }
        }
        model.validate(decorateErrors = true)
    }
}