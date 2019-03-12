package com.dmdirc

import com.dmdirc.MessageFlags.Action
import com.dmdirc.MessageFlags.ChannelEvent
import com.dmdirc.MessageFlags.Message
import com.dmdirc.MessageFlags.Notice
import com.dmdirc.MessageFlags.ServerEvent
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
import com.jukusoft.i18n.I
import java.time.format.DateTimeFormatter

class IrcEventMapper(private val client: IrcClient) {

    fun flags(event: IrcEvent) = sequence {
        yield(if (event.channelEvent) ChannelEvent else ServerEvent)

        when (event) {
            is MessageReceived -> yield(Message)
            is ActionReceived -> yield(Action)
            is NoticeReceived -> yield(Notice)
        }
    }.toSet()

    fun displayableText(event: IrcEvent): Array<String>? = with(event) {
        when (this) {
            is ServerConnected -> arrayOf(I.tr("Connected"))
            is ServerDisconnected -> arrayOf(I.tr("Disconnected"))
            is ServerConnectionError -> arrayOf(I.tr("Error: %s - %s").format(error.translated(), details ?: ""))
            is ChannelJoined -> arrayOf(I.tr("%s joined").format(formattedNickname))
            is ChannelParted -> if (reason.isEmpty()) arrayOf(I.tr("%s left").format(formattedNickname))
            else arrayOf(I.tr("%s left (%s)").format(formattedNickname, reason))
            is ChannelQuit -> if (reason.isEmpty()) arrayOf(I.tr("%s quit").format(formattedNickname))
            else arrayOf(I.tr("%s quit (%s)").format(formattedNickname, reason))
            is ChannelNickChanged -> arrayOf(
                I.tr("%s is now known as %s").format(formattedNickname, newNick.formattedNickname)
            )
            is ChannelTopicChanged -> arrayOf(I.tr("%s has changed the topic to: %s").format(formattedNickname, topic))
            is ChannelTopicDiscovered -> if (topic.isNullOrEmpty()) {
                arrayOf(I.tr("there is no topic set"))
            } else {
                arrayOf(I.tr("the topic is: %s").format(topic))
            }
            is ChannelTopicMetadataDiscovered -> arrayOf(
                I.tr("topic was set at %s on %s by %s").format(
                    setTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    setTime.format(DateTimeFormatter.ofPattern("cccc, d LLLL yyyy")),
                    user.formattedNickname
                )
            )
            is MessageReceived -> arrayOf(user.formattedNickname, message)
            is NoticeReceived -> arrayOf(user.formattedNickname, message)
            is ActionReceived -> arrayOf(user.formattedNickname, action)
            else -> null
        }
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
