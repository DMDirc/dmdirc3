package com.dmdirc

import com.dmdirc.ktirc.events.*
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
import java.util.*

internal class WindowModelTest {

    private val mockConnection = mockk<ConnectionContract.Controller>()
    private val mockConfig = mockk<ClientConfig>()

    @BeforeEach
    fun setup() {
        I.init(File("translations"), Locale.ENGLISH, "messages")
        I.setLanguage(Locale.forLanguageTag("en-GB"))
    }

    @Test
    fun `uses connectionId in sort key if provided`() {
        val model = WindowModel("name", WindowType.ROOT, null, mockk(), "cid123")
        assertEquals("cid123 name", model.sortKey)
    }

    @Test
    fun `doesn't use connectionId in sort key if null`() {
        val model = WindowModel("name", WindowType.ROOT, null, mockk(), null)
        assertEquals(" name", model.sortKey)
    }

    @Test
    fun `doesn't use name in sort key if server`() {
        val model = WindowModel("name", WindowType.SERVER, null, mockk(), "cid123")
        assertEquals("cid123 ", model.sortKey)
    }

    @Test
    fun `uses blank sort key if server without connection id`() {
        val model = WindowModel("name", WindowType.SERVER, null, mockk(), null)
        assertEquals(" ", model.sortKey)
    }

    @Test
    fun `does nothing when handling input if field is blank`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, mockk(), null)
        model.handleInput()
        verify(inverse = true) {
            mockConnection.sendMessage(any(), any())
        }
    }

    @Test
    fun `sends message to controller when input is not empty`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, mockk(), null)
        model.inputField.value = "Mess with the best"
        model.handleInput()
        verify {
            mockConnection.sendMessage("name", "Mess with the best")
        }
    }

    @Test
    fun `blanks input field after sending message`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, mockk(), null)
        model.inputField.value = "Mess with the best"
        model.handleInput()
        assertEquals("", model.inputField.value)
    }

    @Test
    fun `formats and adds lines with timestamp`() {
        val model = WindowModel("name", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.message] } returns "nick=%s message=%s"
        model.addLine("12:34:56", ClientSpec.Formatting.message, "n123", "m456")

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
        val model = WindowModel("name", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.message] } returns "nick=%s message=%s"
        model.addLine("12:34:56", ClientSpec.Formatting.message, "n123", "\u0002m456")

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
        val model = WindowModel("name", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.message] } returns "nick=%s message=%s"
        model.addLine("12:34:56", ClientSpec.Formatting.message, "n123", "https://www.dmdirc.com/")

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
        val model = WindowModel("#channel", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelJoined(EventMetadata(TestConstants.time), User("acidBurn"), "#channel"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.CustomStyle("irc-nickname"))),
                StyledSpan(" joined", emptySet())
            ), model.lines[0]
        )
    }

    @Test
    fun `displays part events without reasons`() {
        val model = WindowModel("#channel", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelParted(EventMetadata(TestConstants.time), User("acidBurn"), "#channel"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.CustomStyle("irc-nickname"))),
                StyledSpan(" left", emptySet())
            ), model.lines[0]
        )
    }

    @Test
    fun `displays part events with reasons`() {
        val model = WindowModel("#channel", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(
            ChannelParted(
                EventMetadata(TestConstants.time),
                User("acidBurn"),
                "#channel",
                "Mess with the best"
            )
        )
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.CustomStyle("irc-nickname"))),
                StyledSpan(" left (Mess with the best)", emptySet())
            ), model.lines[0]
        )
    }

    @Test
    fun `displays message events`() {
        val model = WindowModel("#channel", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.message] } returns "<%s> %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(
            MessageReceived(
                EventMetadata(TestConstants.time),
                User("acidBurn"),
                "#channel",
                "Mess with the best"
            )
        )
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" <", emptySet()),
                StyledSpan("acidBurn", setOf(Style.CustomStyle("irc-nickname"))),
                StyledSpan("> Mess with the best", emptySet())
            ), model.lines[0]
        )
    }

    @Test
    fun `displays action events`() {
        val model = WindowModel("#channel", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.action] } returns "* %s %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ActionReceived(EventMetadata(TestConstants.time), User("acidBurn"), "#channel", "hacks"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" * ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.CustomStyle("irc-nickname"))),
                StyledSpan(" hacks", emptySet())
            ), model.lines[0]
        )
    }

    @Test
    fun `displays quit events without reasons`() {
        val model = WindowModel("#channel", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ChannelQuit(EventMetadata(TestConstants.time), User("acidBurn"), "#channel"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.CustomStyle("irc-nickname"))),
                StyledSpan(" quit", emptySet())
            ), model.lines[0]
        )
    }

    @Test
    fun `displays quit events with reasons`() {
        val model = WindowModel("#channel", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.channelEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(
            ChannelQuit(
                EventMetadata(TestConstants.time),
                User("acidBurn"),
                "#channel",
                "Mess with the best"
            )
        )
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- ", emptySet()),
                StyledSpan("acidBurn", setOf(Style.CustomStyle("irc-nickname"))),
                StyledSpan(" quit (Mess with the best)", emptySet())
            ), model.lines[0]
        )
    }

    @Test
    fun `displays connected events`() {
        val model = WindowModel("server", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.serverEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ServerConnected(EventMetadata(TestConstants.time)))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- Connected", emptySet())
            ), model.lines[0]
        )
    }

    @Test
    fun `displays disconnected events`() {
        val model = WindowModel("server", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.serverEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ServerDisconnected(EventMetadata(TestConstants.time)))
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
        val model = WindowModel("server", WindowType.ROOT, mockConnection, mockConfig, null)
        every { mockConfig[ClientSpec.Formatting.serverEvent] } returns "-- %s"
        every { mockConfig[ClientSpec.Formatting.timestamp] } returns "HH:mm:ss"
        model.handleEvent(ServerConnectionError(EventMetadata(TestConstants.time), ConnectionError.BadTlsCertificate, "details"))
        assertEquals(1, model.lines.size)

        assertArrayEquals(
            arrayOf(
                StyledSpan("09:00:00", setOf(Style.CustomStyle("timestamp"))),
                StyledSpan(" -- Error: the server's certificate was not valid - details", emptySet())
            ), model.lines[0]
        )
    }

}