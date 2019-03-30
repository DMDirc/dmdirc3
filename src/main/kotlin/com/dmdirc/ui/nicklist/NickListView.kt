package com.dmdirc.ui.nicklist

import javafx.scene.control.ListView

class NickListView : ListView<String>() {
    init {
        styleClass.add("nick-list")
        isFocusTraversable = false
        prefWidth = 148.0
    }
}
