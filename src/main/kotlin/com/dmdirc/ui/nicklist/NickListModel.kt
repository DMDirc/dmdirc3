package com.dmdirc.ui.nicklist

import com.dmdirc.ktirc.events.ChannelMembershipAdjustment
import com.dmdirc.observable

class NickListModel : NickListContract.Model {

    override val users = mutableListOf<String>().observable()

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
