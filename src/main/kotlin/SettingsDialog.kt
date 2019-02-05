package com.dmdirc

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonBar
import tornadofx.*

data class Settings(val nickname: String, val realname: String, val username: String)

class SettingsModel : ItemViewModel<Settings>() {
    private val keyNickname = "nickname"
    private val keyRealname = "realname"
    private val keyUsername = "username"

    val nickname = bind { SimpleStringProperty(item?.nickname, null, app.config.string(keyNickname)) }
    val realname = bind { SimpleStringProperty(item?.realname, null, app.config.string(keyRealname)) }
    val username = bind { SimpleStringProperty(item?.username, null, app.config.string(keyUsername)) }

    override fun onCommit() {
        with(app.config) {
            set(keyNickname to nickname.value)
            set(keyRealname to realname.value)
            set(keyUsername to username.value)
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