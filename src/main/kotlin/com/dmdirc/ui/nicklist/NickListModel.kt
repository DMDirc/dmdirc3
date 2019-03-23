package com.dmdirc.ui.nicklist

import com.dmdirc.WindowModel
import com.dmdirc.ktirc.events.ChannelMembershipAdjustment
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.TargetedEvent
import com.dmdirc.observable
import javafx.beans.property.Property

class NickListModel(selectedWindow: Property<WindowModel>) {

    private var currentChannel: String = ""

    val nicks = mutableListOf<String>().observable()

    init {
        selectedWindow.addListener { _, oldValue, newValue ->
            oldValue?.connection?.removeEventListener(this::handleEvent)
            bindToModel(newValue)
        }
        bindToModel(selectedWindow.value)
    }

    private fun bindToModel(windowModel: WindowModel?) {
        windowModel?.let { wm ->
            val name = wm.name.value
            wm.connection?.let {
                it.addEventListener(this::handleEvent)
                nicks.setAll(it.getUsers(name).map { user -> user.nickname })
                currentChannel = name
            }
        }
    }

    private fun handleEvent(event: IrcEvent) {
        if (event is ChannelMembershipAdjustment && event is TargetedEvent && event.target == currentChannel) {
            event.addedUser?.let { nicks.add(it) }
            event.removedUser?.let { nicks.remove(it) }
            event.replacedUsers?.let { nicks.setAll(it.toList()) }
        }
    }
}
