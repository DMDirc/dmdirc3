package com.dmdirc

import com.dmdirc.ktirc.io.CaseMapping
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MainControllerTest {

    private val factory = mockk<(ConnectionDetails) -> ConnectionContract.Controller>()

    private val mockServer1 = mockk<ConnectionContract.Controller> {
        every { children } returns Connection.WindowMap { CaseMapping.Rfc }
    }
    private val mockServer2 = mockk<ConnectionContract.Controller> {
        every { children } returns Connection.WindowMap { CaseMapping.Rfc }
    }

    private val emptyConfig = mockk<ClientConfig> {
        every { get(ClientSpec.servers) } returns emptyList()
    }

    @BeforeEach
    fun setup() {
        runLaterProvider = { it.run() }
    }

    @Test
    fun `autoconnects on init`() {
        val serverList = listOf(
            ConnectionDetails("host1", "pass1", 123, tls = true, autoconnect = true),
            ConnectionDetails("host2", "", 1234, tls = false, autoconnect = false),
            ConnectionDetails("host3", "pass3", 1235, tls = true, autoconnect = false),
            ConnectionDetails("host4", "", 999, tls = false, autoconnect = true)
        )

        val config = mockk<ClientConfig> {
            every { get(ClientSpec.servers) } returns serverList
        }

        every { factory.invoke(serverList[0]) } returns mockServer1
        every { factory.invoke(serverList[3]) } returns mockServer2

        MainController(config, factory)

        verify {
            factory.invoke(serverList[0])
            factory.invoke(serverList[3])
            mockServer1.connect()
            mockServer2.connect()
        }
    }

    @Test
    fun `connects to new servers`() {
        val controller = MainController(emptyConfig, factory)
        val details = ConnectionDetails("host3", "pass3", 1235, tls = true, autoconnect = false)

        every { factory.invoke(details) } returns mockServer1

        controller.connect(details)

        verify {
            factory.invoke(details)
            mockServer1.connect()
        }
    }

    @Test
    fun `adds existing windows from a server`() {
        val controller = MainController(emptyConfig, factory)
        val details = ConnectionDetails("host3", "pass3", 1235, tls = true, autoconnect = false)

        every { factory.invoke(details) } returns mockServer1
        val model = WindowModel("name", WindowType.CHANNEL, null, mockk(), null)
        mockServer1.children += Connection.Child(model, mockk())

        controller.connect(details)
        assertEquals(1, controller.windows.size)
        assertEquals(model, controller.windows[0])
    }

    @Test
    fun `adds new windows from a server`() {
        val controller = MainController(emptyConfig, factory)
        val details = ConnectionDetails("host3", "pass3", 1235, tls = true, autoconnect = false)

        every { factory.invoke(details) } returns mockServer1

        controller.connect(details)

        assumeTrue(controller.windows.isEmpty())

        val model = WindowModel("name", WindowType.CHANNEL, null, mockk(), null)
        mockServer1.children += Connection.Child(model, mockk())

        assertEquals(1, controller.windows.size)
        assertEquals(model, controller.windows[0])
    }

    @Test
    fun `removes windows deleted from a server`() {
        val controller = MainController(emptyConfig, factory)
        val details = ConnectionDetails("host3", "pass3", 1235, tls = true, autoconnect = false)

        every { factory.invoke(details) } returns mockServer1

        controller.connect(details)

        val model = WindowModel("name", WindowType.CHANNEL, null, mockk(), null)
        mockServer1.children += Connection.Child(model, mockk())

        assumeTrue(1 == controller.windows.size)

        mockServer1.children -= "name"

        assertTrue(controller.windows.isEmpty())
    }

}