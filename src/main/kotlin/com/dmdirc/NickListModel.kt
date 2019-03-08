package com.dmdirc

import com.dmdirc.ktirc.events.ChannelMembershipAdjustment

class NickListModel {

    val users = mutableListOf<String>().observable()

    fun handleEvent(event: ChannelMembershipAdjustment) {
        event.addedUser?.let {
            users.add(it)
        }

        event.removedUser?.let {
            users.remove(it)
        }

        event.replacedUsers?.let {
            users.clear()
            users.addAll(it)
        }
    }
}
