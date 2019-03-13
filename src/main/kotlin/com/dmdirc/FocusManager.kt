package com.dmdirc

import javafx.beans.property.ObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.TextField
import javafx.stage.Stage

class FocusListener(
    private val uiNode: WindowUI,
    private val dialogPane: ObjectProperty<Node>
) : ChangeListener<Any> {
    override fun changed(observable: ObservableValue<out Any>?, oldValue: Any?, newValue: Any?) {
        if (newValue == false && dialogPane.value?.visibleProperty()?.value != true) {
            uiNode.inputField.requestFocus()
        }
    }
}

class FocusManager(
    private val stage: Stage,
    private val controller: MainContract.Controller,
    private val dialogPane: ObjectProperty<Node>
) {
    private var inputField: TextField? = null
    private var inputFieldListener: ChangeListener<Any>? = null

    fun start() {
        stage.scene.focusOwnerProperty().addListener { _, _, _ ->
            val selectedWindowValue = controller.selectedWindow.value
            val uiNode = selectedWindowValue?.connection?.children?.get(selectedWindowValue.name.value)?.ui
            if (uiNode is WindowUI && dialogPane.value?.visibleProperty()?.value != true) {
                if (inputFieldListener != null) {
                    inputField?.focusedProperty()?.removeListener(inputFieldListener)
                }
                inputField = uiNode.inputField
                uiNode.inputField.requestFocus()
                inputFieldListener = FocusListener(uiNode, dialogPane)
                uiNode.inputField.focusedProperty().addListener(inputFieldListener)
            }
        }
    }
}
