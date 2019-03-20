package com.dmdirc.ui.nicklist

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.ListView

class NickListController(private val model: NickListContract.Model) : NickListContract.Controller {

    @FXML
    private lateinit var nicklist: ListView<String>

    override fun createUi(): Node {
        val loader = FXMLLoader(javaClass.getResource("/fxml/nicklist.fxml"))
        loader.setController(this)
        return loader.load<Node>().also {
            nicklist.items = model.users
        }
    }
}
