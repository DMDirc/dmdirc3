package com.dmdirc

import javafx.beans.property.Property
import javafx.stage.Stage

class NotificationManager(
    private val selectedWindow: Property<WindowModel>,
    private val stage: Stage
) {
    fun notify(window: WindowModel, message: String) {
        if (selectedWindow.value != window || !stage.isFocused) {
            println("Notification! ${window.name.value}: $message")
        }
    }
}
