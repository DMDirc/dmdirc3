package com.dmdirc

import com.dmdirc.MessageFlags.Message
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
    fun `formats and adds lines with timestamp`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, eventMapper, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.message] } returns "nick=%s message=%s"
        model.addLine("12:34:56", setOf(Message), arrayOf("n123", "m456"))

        assertEquals(1, model.lines.size)
        assertArrayEquals(
            arrayOf(
                StyledSpan("12:34:56", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" nick=n123 message=m456", emptySet())
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
                StyledSpan("12:34:56", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" nick=n123 message=", emptySet()),
                StyledSpan("m456", setOf(Style.BoldStyle))
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
                StyledSpan("12:34:56", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" nick=n123 message=", emptySet()),
                StyledSpan("https://www.dmdirc.com/", setOf(Style.Link("https://www.dmdirc.com/")))
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"))),
                StyledSpan(" joined", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"))),
                StyledSpan(" left", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"))),
                StyledSpan(" left (Mess with the best)", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" <", emptySet()),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"))),
                StyledSpan("> Mess with the best", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -", emptySet()),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"))),
                StyledSpan("- Mess with the best", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" * ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"))),
                StyledSpan(" hacks", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("zeroCool", setOf(Style.Nickname("zeroCool"))),
                StyledSpan(" is now known as ", emptySet()),
                StyledSpan("crashOverride", setOf(Style.Nickname("crashOverride")))
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"))),
                StyledSpan(" has changed the topic to: Mess with the best", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- there is no topic set", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- the topic is: Mess with the best", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- topic was set at 09:00:00 on Friday, 15 September 1995 by ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn")))
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"))),
                StyledSpan(" quit", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.Nickname("acidBurn"))),
                StyledSpan(" quit (Mess with the best)", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))), StyledSpan(" -- Connected", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- Disconnected", emptySet())
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
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- Error: the server's certificate was not valid - details", emptySet())
            ), model.lines[0]
        )
    }
}
