package com.dmdirc

import com.dmdirc.ktirc.events.*

class NickListModel(private val controller: ConnectionContract.Controller?) {

    val users = mutableListOf<String>().observable()

    fun handleEvent(event: IrcEvent) {
        when (event) {
            is ChannelJoined -> users.add(event.user.nickname)
            is ChannelParted -> users.remove(event.user.nickname)
            is ChannelQuit -> users.remove(event.user.nickname)
            is ChannelUserKicked -> users.remove(event.victim)
            is ChannelNickChanged -> {
                users.remove(event.user.nickname)
                users.add(event.newNick)
            }
            is ChannelNamesFinished -> {
                users.clear()
                controller?.let {
                    users.addAll(it.getUsers(event.target).map { user -> user.nickname })
                }
            }
        }
    }

}
