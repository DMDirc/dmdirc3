package com.dmdirc.ui.nicklist

class NickListBinder {
    fun bind(model: NickListModel, view: NickListView) {
        view.items = model.nicks
    }
}
