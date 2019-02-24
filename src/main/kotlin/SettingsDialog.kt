package com.dmdirc

import com.jukusoft.i18n.I.tr
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonBar
import org.kodein.di.generic.instance
import tornadofx.*

data class Settings(val nickname: String, val realname: String, val username: String)

class SettingsModel : ItemViewModel<Settings>() {
    private val config1 by kodein.instance<ClientConfig>()

    val nickname = bind { SimpleStringProperty(item?.nickname, null, config1[ClientSpec.DefaultProfile.nickname]) }
    val realname = bind { SimpleStringProperty(item?.realname, null, config1[ClientSpec.DefaultProfile.realname]) }
    val username = bind { SimpleStringProperty(item?.username, null, config1[ClientSpec.DefaultProfile.username]) }

    override fun onCommit() {
        config1[ClientSpec.DefaultProfile.nickname] = nickname.value
        config1[ClientSpec.DefaultProfile.realname] = realname.value
        config1[ClientSpec.DefaultProfile.username] = username.value
        config1.save()
    }
}

class SettingsDialog : Fragment() {
    private val model = SettingsModel()
    override val root = form {
        fieldset(tr("Profile")) {
            field(tr("Nickname: ")) {
                textfield(model.nickname).required()
            }
            field(tr("Realname: ")) {
                textfield(model.realname).required()
            }
            field(tr("Username: ")) {
                textfield(model.username).required()
            }
        }
        buttonbar {
            button(tr("Close"), ButtonBar.ButtonData.CANCEL_CLOSE).action {
                model.rollback()
                close()
            }
            button(tr("Reset"), ButtonBar.ButtonData.OTHER).action {
                model.rollback()
            }
            button(tr("Save"), ButtonBar.ButtonData.OK_DONE) {
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