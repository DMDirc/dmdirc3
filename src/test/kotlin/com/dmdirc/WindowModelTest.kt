package com.dmdirc

import com.dmdirc.MessageFlag.Message
import com.dmdirc.ktirc.IrcClient
import com.dmdirc.ktirc.events.ActionReceived
import com.dmdirc.ktirc.events.ChannelJoined
import com.dmdirc.ktirc.events.ChannelNickChanged
import com.dmdirc.ktirc.events.ChannelParted
import com.dmdirc.ktirc.events.ChannelQuit
import com.dmdirc.ktirc.events.ChannelTopicChanged
import com.dmdirc.ktirc.events.ChannelTopicDiscovered
import com.dmdirc.ktirc.events.ChannelTopicMetadataDiscovered
import com.dmdirc.ktirc.events.EventMetadata
import com.dmdirc.ktirc.events.MessageReceived
import com.dmdirc.ktirc.events.NoticeReceived
import com.dmdirc.ktirc.events.ServerConnected
import com.dmdirc.ktirc.events.ServerConnectionError
import com.dmdirc.ktirc.events.ServerDisconnected
import com.dmdirc.ktirc.model.ConnectionError
import com.dmdirc.ktirc.model.User
import com.jukusoft.i18n.I
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Locale

internal class WindowModelTest {

    private val mockConnection = mockk<ConnectionContract.Controller>()
    private val mockConfig = mockk<ClientConfig>()
    private val metaData = mockk<EventMetadata>()
    private val mockClient = mockk<IrcClient> {
        every { isChannel(match { it.startsWith("#") }) } returns true
        every { isChannel(match { !it.startsWith("#") }) } returns false
        every { isLocalUser(any<User>()) } returns false
        every { localUser } returns User("localNick")
    }
    private val eventMapper = IrcEventMapper(mockClient)

    @BeforeEach
    fun setup() {
        I.init(File("translations"), Locale.ENGLISH, "messages")
        I.setLanguage(Locale.forLanguageTag("en-GB"))
        every { metaData.time } returns TestConstants.time
        PlatformWrappers.runLaterProvider = { it.run() }
        PlatformWrappers.fxThreadTester = { true }
    }

    @Test
    fun `uses connectionId in sort key if provided`() {
        val model = WindowModel("name", WindowType.ROOT, null, eventMapper, mockk(), "cid123")
        assertEquals("cid123 name", model.sortKey)
    }

    @Test
    fun `doesn't use connectionId in sort key if null`() {
        val model = WindowModel("name", WindowType.ROOT, null, eventMapper, mockk(), null)
        assertEquals(" name", model.sortKey)
    }

    @Test
    fun `doesn't use name in sort key if server`() {
        val model = WindowModel("name", WindowType.SERVER, null, eventMapper, mockk(), "cid123")
        assertEquals("cid123 ", model.sortKey)
    }

    @Test
    fun `uses blank sort key if server without connection id`() {
        val model = WindowModel("name", WindowType.SERVER, null, eventMapper, mockk(), null)
        assertEquals(" ", model.sortKey)
    }

    @Test
    fun `does nothing when handling input if field is blank`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, eventMapper, mockk(), null)
        model.handleInput()
        verify(inverse = true) {
            mockConnection.sendMessage(any(), any())
        }
    }

    @Test
    fun `sends message to controller when input is not empty`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, eventMapper, mockk(), null)
        model.inputField.value = "Mess with the best"
        model.handleInput()
        verify {
            mockConnection.sendMessage("name", "Mess with the best")
        }
    }

    @Test
    fun `sends actions to controller when input is prefixed with me`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, eventMapper, mockk(), null)
        model.inputField.value = "/me hacks the planet"
        model.handleInput()
        verify {
            mockConnection.sendAction("name", "hacks the planet")
        }
    }

    @Test
    fun `blanks input field after sending message`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, eventMapper, mockk(), null)
        model.inputField.value = "Mess with the best"
        model.handleInput()
        assertEquals("", model.inputField.value)
    }

    @Test
    fun `formats and adds lines with timestamp`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.message] } returns "nick=%s message=%s"
        model.addLine("12:34:56", setOf(Message), arrayOf("n123", "m456"))

        assertEquals(1, model.lines.size)
        assertArrayEquals(
            arrayOf(
                StyledSpan("12:34:56", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-Message"))),
                StyledSpan(" nick=n123 message=m456", setOf(Style.CustomStyle("messagetype-Message")))
            ), model.lines[0]
        )
    }

    @Test
    fun `parses control codes when adding lines`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.message] } returns "nick=%s message=%s"
        model.addLine("12:34:56", setOf(Message), arrayOf("n123", "\u0002m456"))

        assertEquals(1, model.lines.size)
        assertArrayEquals(
            arrayOf(
                StyledSpan("12:34:56", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-Message"))),
                StyledSpan(" nick=n123 message=", setOf(Style.CustomStyle("messagetype-Message"))),
                StyledSpan("m456", setOf(Style.BoldStyle, Style.CustomStyle("messagetype-Message")))
            ), model.lines[0]
        )
    }

    @Test
    fun `detects links when adding lines`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.message] } returns "nick=%s message=%s"
        model.addLine("12:34:56", setOf(Message), arrayOf("n123", "https://www.dmdirc.com/"))

        assertEquals(1, model.lines.size)
        assertArrayEquals(
            arrayOf(
                StyledSpan("12:34:56", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-Message"))),
                StyledSpan(" nick=n123 message=", setOf(Style.CustomStyle("messagetype-Message"))),
                StyledSpan(
                    "https://www.dmdirc.com/",
                    setOf(Style.Link("https://www.dmdirc.com/"), Style.CustomStyle("messagetype-Message"))
                )
            ), model.lines[0]
        )
    }

    @Test
    fun `displays join events`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelJoined(metaData, User("acidBurn"), "#channel"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" -- ", setOf(Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" joined", setOf(Style.CustomStyle("messagetype-ChannelEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays part events without reasons`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelParted(metaData, User("acidBurn"), "#channel"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" -- ", setOf(Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" left", setOf(Style.CustomStyle("messagetype-ChannelEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays part events with reasons`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(
            ChannelParted(
                metaData, User("acidBurn"), "#channel", "Mess with the best"
            )
        )
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" -- ", setOf(Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" left (Mess with the best)", setOf(Style.CustomStyle("messagetype-ChannelEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays message events`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.message] } returns "<%s> %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(
            MessageReceived(
                metaData, User("acidBurn"), "#channel", "Mess with the best"
            )
        )
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Message"))),
                StyledSpan(" <", setOf(Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Message"))),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"), Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Message"))),
                StyledSpan("> Mess with the best", setOf(Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Message")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays notice events`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.notice] } returns "-%s- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(
            NoticeReceived(
                metaData, User("acidBurn"), "#channel", "Mess with the best"
            )
        )
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Notice"))),
                StyledSpan(" -", setOf(Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Notice"))),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"), Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Notice"))),
                StyledSpan("- Mess with the best", setOf(Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Notice")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays action events`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.action] } returns "* %s %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ActionReceived(metaData, User("acidBurn"), "#channel", "hacks"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Action"))),
                StyledSpan(" * ", setOf(Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Action"))),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"), Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Action"))),
                StyledSpan(" hacks", setOf(Style.CustomStyle("messagetype-ChannelEvent"), Style.CustomStyle("messagetype-Action")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays nick changes`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelNickChanged(metaData, User("zeroCool"), "#channel", "crashOverride"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" -- ", setOf(Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan("zeroCool", setOf(Style.Nickname("zeroCool"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" is now known as ", setOf(Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan("crashOverride", setOf(Style.Nickname("crashOverride"), Style.CustomStyle("messagetype-ChannelEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays topic changes`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelTopicChanged(metaData, User("acidBurn"), "#channel", "Mess with the best"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" -- ", setOf(Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" has changed the topic to: Mess with the best", setOf(Style.CustomStyle("messagetype-ChannelEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays empty topic discovered`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelTopicDiscovered(metaData, "#channel", null))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" -- there is no topic set", setOf(Style.CustomStyle("messagetype-ChannelEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays topic discovered`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelTopicDiscovered(metaData, "#channel", "Mess with the best"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" -- the topic is: Mess with the best", setOf(Style.CustomStyle("messagetype-ChannelEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays topic metadata`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelTopicMetadataDiscovered(metaData, "#channel", User("acidBurn"), TestConstants.time))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" -- topic was set at 09:00:00 on Friday, 15 September 1995 by ", setOf(Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"), Style.CustomStyle("messagetype-ChannelEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays quit events without reasons`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelQuit(metaData, User("acidBurn"), "#channel"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" -- ", setOf(Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" quit", setOf(Style.CustomStyle("messagetype-ChannelEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays quit events with reasons`() {
        val model = WindowModel("#channel", WindowType.CHANNEL, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(
            ChannelQuit(
                metaData, User("acidBurn"), "#channel", "Mess with the best"
            )
        )
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" -- ", setOf(Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"), Style.CustomStyle("messagetype-ChannelEvent"))),
                StyledSpan(" quit (Mess with the best)", setOf(Style.CustomStyle("messagetype-ChannelEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays connected events`() {
        val model = WindowModel("server", WindowType.ROOT, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.serverEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ServerConnected(metaData))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ServerEvent"))),
                StyledSpan(" -- Connected", setOf(Style.CustomStyle("messagetype-ServerEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays disconnected events`() {
        val model = WindowModel("server", WindowType.ROOT, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.serverEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ServerDisconnected(metaData))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ServerEvent"))),
                StyledSpan(" -- Disconnected", setOf(Style.CustomStyle("messagetype-ServerEvent")))
            ), model.lines[0]
        )
    }

    @Test
    fun `displays connection errors with details`() {
        val model = WindowModel("server", WindowType.ROOT, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.serverEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ServerConnectionError(metaData, ConnectionError.BadTlsCertificate, "details"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"), Style.CustomStyle("messagetype-ServerEvent"))),
                StyledSpan(" -- Error: the server's certificate was not valid - details", setOf(Style.CustomStyle("messagetype-ServerEvent")))
            ), model.lines[0]
        )
    }
}
