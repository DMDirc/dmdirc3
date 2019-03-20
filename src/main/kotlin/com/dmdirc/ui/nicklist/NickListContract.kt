package com.dmdirc.ui.nicklist

import javafx.collections.ObservableList
import javafx.scene.Node

object NickListContract {

    interface Model {
        val users: ObservableList<String>
    }

    interface Controller {
        fun createUi(): Node
    }
}
