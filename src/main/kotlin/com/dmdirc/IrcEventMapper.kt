package com.dmdirc

import com.dmdirc.MessageFlag.Action
import com.dmdirc.MessageFlag.ChannelEvent
import com.dmdirc.MessageFlag.Highlight
import com.dmdirc.MessageFlag.Message
import com.dmdirc.MessageFlag.Notice
import com.dmdirc.MessageFlag.Self
import com.dmdirc.MessageFlag.ServerEvent
import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.ActionReceived
import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.events.ChannelNickChanged
import com.dmdirc.ktirc.events.ChannelParted
import com.dmdirc.ktirc.events.ChannelQuit
import com.dmdirc.ktirc.events.ChannelTopicChanged
import com.dmdirc.ktirc.events.ChannelTopicDiscovered
import com.dmdirc.ktirc.events.ChannelTopicMetadataDiscovered
import com.dmdirc.ktirc.events.IrcEvent
import com.dmdirc.ktirc.events.MessageReceived
import com.dmdirc.ktirc.events.NoticeReceived
import com.dmdirc.ktirc.events.ServerConnected
import com.dmdirc.ktirc.events.ServerConnectionError
import com.dmdirc.ktirc.events.ServerDisconnected
import com.dmdirc.ktirc.events.SourcedEvent
import com.dmdirc.ktirc.events.TargetedEvent
import com.dmdirc.ktirc.model.User
import com.jukusoft.i18n.I.tr
import java.time.format.DateTimeFormatter

class IrcEventMapper(private val client: IrcClient) {

    fun flags(event: IrcEvent) = sequence {
        yield(if (event.channelEvent) ChannelEvent else ServerEvent)

        if (event is SourcedEvent && client.isLocalUser(event.user)) {
            yield(Self)
        } else if (event.isHighlight) {
            yield(Highlight)
        }

        when (event) {
            is MessageReceived -> yield(Message)
            is ActionReceived -> yield(Action)
            is NoticeReceived -> yield(Notice)
        }
    }.toSet()

    fun displayableText(event: IrcEvent): Array<String>? = event.translate()

    private fun IrcEvent.translate(): Array<String>? = when (this) {
        // Things that don't need translating:
        is MessageReceived -> arrayOf(formattedNickname, message)
        is NoticeReceived -> arrayOf(formattedNickname, message)
        is ActionReceived -> arrayOf(formattedNickname, action)
        else -> (translateSimpleText() ?: translateConditionalText())?.let { arrayOf(it) }
    }

    private fun IrcEvent.translateSimpleText(): String? = when (this) {
        // Things that have a constant translation for their events
        is ServerConnected -> tr("Connected")
        is ServerDisconnected -> tr("Disconnected")
        is ServerConnectionError -> tr("Error: %s - %s").format(error.translated(), details ?: "")
        is ChannelJoined -> tr("%s joined").format(formattedNickname)
        is ChannelNickChanged -> tr("%s is now known as %s").format(formattedNickname, newNick.formattedNickname)
        is ChannelTopicChanged -> tr("%s has changed the topic to: %s").format(formattedNickname, topic)
        is ChannelTopicMetadataDiscovered -> tr("topic was set at %s on %s by %s").format(
            setTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            setTime.format(DateTimeFormatter.ofPattern("cccc, d LLLL yyyy")),
            user.formattedNickname
        )
        else -> null
    }

    private fun IrcEvent.translateConditionalText(): String? = when {
        // Things that have different transactions based on event properties
        this is ChannelParted && reason.isEmpty() -> tr("%s left").format(formattedNickname)
        this is ChannelParted -> tr("%s left (%s)").format(formattedNickname, reason)

        this is ChannelQuit && reason.isEmpty() -> tr("%s quit").format(formattedNickname)
        this is ChannelQuit -> tr("%s quit (%s)").format(formattedNickname, reason)

        this is ChannelTopicDiscovered && topic.isNullOrEmpty() -> tr("there is no topic set")
        this is ChannelTopicDiscovered -> tr("the topic is: %s").format(topic)

        else -> null
    }

    private val IrcEvent.isHighlight: Boolean
    get() = when (this) {
        is MessageReceived -> message.contains(client.localUser.nickname, true)
        is NoticeReceived -> message.contains(client.localUser.nickname, true)
        is ActionReceived -> action.contains(client.localUser.nickname, true)
        else -> false
    }

    private val IrcEvent.channelEvent: Boolean
        get() = this is TargetedEvent && client.isChannel(this.target)

    private val SourcedEvent.formattedNickname: String
        get() = user.formattedNickname

    private val User.formattedNickname: String
        get() = nickname.formattedNickname

    private val String.formattedNickname: String
        get() = "${ControlCode.InternalNicknames}$this${ControlCode.InternalNicknames}"
}
