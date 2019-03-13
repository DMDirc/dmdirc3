package com.dmdirc

import javafx.beans.property.ObjectProperty
import javafx.scene.Node
import javafx.stage.Stage

class FocusManager(
    private val stage: Stage,
    private val controller: MainContract.Controller,
    private val dialogPane: ObjectProperty<Node>
) {
    fun start() {
        stage.scene.focusOwnerProperty().addListener { _, _, _ ->
            val selectedWindowValue = controller.selectedWindow.value
            val uiNode = selectedWindowValue?.connection?.children?.get(selectedWindowValue.name.value)?.ui
            if (uiNode is WindowUI && dialogPane.value?.visibleProperty()?.value != true) {
                uiNode.inputField.requestFocus()
                uiNode.inputField.focusedProperty().addListener { _, _, newValue ->
                    if (newValue == false && dialogPane.value?.visibleProperty()?.value != true) {
                        uiNode.inputField.requestFocus()
                    }
                }
            }
        }
    }
}
