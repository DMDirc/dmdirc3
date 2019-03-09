package com.dmdirc

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import javafx.scene.control.TextFormatter.Change
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ServerListControllerTest {
    private val mainController = mockk<MainContract.Controller>()
    private val config = mockk<ClientConfig>()
    private val controller = ServerListController(mainController, config)

    @Test
    fun `test connect called`() {
        val connectionEditable = ConnectionDetailsEditable(
            hostname = "hostname", password = "password", port = "1", tls = false, autoconnect = true
        )
        val connection = ConnectionDetails(
            hostname = "hostname", password = "password", port = 1, tls = false, autoconnect = true
        )
        controller.connect(connectionEditable)
        verify {
            mainController.connect(connection)
        }
    }

    @Test
    fun `test converting`() {
        val connectionEditable = ConnectionDetailsEditable(
            hostname = "hostname", password = "password", port = "1", tls = false, autoconnect = true
        )
        val connection = ConnectionDetails(
            hostname = "hostname", password = "password", port = 1, tls = false, autoconnect = true
        )
        assertEquals(connection, controller.getConnectionDetails(connectionEditable))
    }

    @Test
    fun `test saving serverlist`() {
        val serversEditable = listOf(
            ConnectionDetailsEditable(
                hostname = "hostname1", password = "password", port = "1", tls = false, autoconnect = true
            ), ConnectionDetailsEditable(
                hostname = "hostname2", password = "password", port = "2", tls = true, autoconnect = false
            )
        ).observable()
        val servers = listOf(
            ConnectionDetails(
                hostname = "hostname1", password = "password", port = 1, tls = false, autoconnect = true
            ), ConnectionDetails(
                hostname = "hostname2", password = "password", port = 2, tls = true, autoconnect = false
            )
        )
        controller.save(serversEditable)
        verify {
            config[ClientSpec.servers] = servers
            config.save()
        }
    }
}

internal class ServerListModelTest {
    private val controller = mockk<ServerListController>()
    private val config = mockk<ClientConfig>()
    private val model = ServerListModel(controller, config)

    @Test
    fun `test connect pressed when null`() {
        model.connectPressed()
        verify(exactly = 0) {
            controller.connect(any())
        }
    }

    @Test
    fun `test connect pressed when not null`() {
        model.selected.value = ConnectionDetailsEditable(
            hostname = "hostname", password = "password", port = "1", tls = false, autoconnect = true
        )
        model.connectPressed()
        verify {
            controller.connect(any())
        }
    }

    @Test
    fun `test delete pressed when null`() {
        val serversEditable = listOf(
            ConnectionDetailsEditable(
                hostname = "hostname1", password = "password", port = "1", tls = false, autoconnect = true
            ), ConnectionDetailsEditable(
                hostname = "hostname2", password = "password", port = "2", tls = true, autoconnect = false
            )
        )
        model.servers.addAll(serversEditable)
        model.selected.value = null
        model.deletePressed()
        assertEquals(serversEditable, model.servers)
    }

    @Test
    fun `test delete pressed when not null`() {
        val serversEditable = listOf(
            ConnectionDetailsEditable(
                hostname = "hostname1", password = "password", port = "1", tls = false, autoconnect = true
            ), ConnectionDetailsEditable(
                hostname = "hostname2", password = "password", port = "2", tls = true, autoconnect = false
            )
        )
        model.servers.addAll(serversEditable)
        model.selected.value = serversEditable.first()
        model.deletePressed()
        assertEquals(serversEditable.subList(1, 2), model.servers)
    }

    @Test
    fun `test save pressed`() {
        assertEquals(true, model.open.value)
        model.savePressed()
        assertEquals(false, model.open.value)
        verify {
            controller.save(any())
        }
    }

    @Test
    fun `test save pressed unsaved`() {
        val server = ConnectionDetailsEditable(
            hostname = "New Server", port = "6697", tls = true, autoconnect = false
        )
        val newServer = ConnectionDetailsEditable(
            hostname = "test", port = "6667", tls = false, autoconnect = true
        )
        model.servers.addAll(
            ConnectionDetailsEditable(
                hostname = "New Server", port = "6697", tls = true, autoconnect = false
            )
        )
        model.selected.value = server
        assertEquals(true, model.open.value)
        assertEquals(server, model.selected.value)
        assertEquals("New Server", model.hostname.value)
        assertEquals("", model.password.value)
        assertEquals("6697", model.port.value)
        assertEquals(false, model.autoconnect.value)
        assertEquals(true, model.tls.value)
        model.hostname.value = "test"
        model.port.value = "6667"
        model.tls.value = false
        model.autoconnect.value = true
        model.savePressed()
        assertEquals(false, model.open.value)
        assertEquals(null, model.selected.value)
        assertEquals("test", model.hostname.value)
        assertEquals("", model.password.value)
        assertEquals("6667", model.port.value)
        assertEquals(true, model.autoconnect.value)
        assertEquals(false, model.tls.value)
        verify {
            controller.save(any())
        }
    }

    @Test
    fun `test close pressed`() {
        assertEquals(true, model.open.value)
        model.closeDialog()
        assertEquals(false, model.open.value)
    }

    @Test
    fun `test add pressed when empty`() {
        val server = ConnectionDetailsEditable(
            hostname = "New Server", port = "6697", tls = true, autoconnect = false
        )
        assertTrue(model.servers.isEmpty())
        model.addPressed()
        assertTrue(model.servers.size == 1)
        assertEquals(server, model.servers[0])
        assertEquals(server, model.selected.value)
    }

    @Test
    fun `test add pressed when not empty`() {
        val server = ConnectionDetailsEditable(
            hostname = "New Server", port = "6697", tls = true, autoconnect = false
        )
        model.servers.addAll(
            ConnectionDetailsEditable(
                hostname = "hostname", password = "password", port = "1", tls = false, autoconnect = true
            )
        )
        assertTrue(model.servers.size == 1)
        model.addPressed()
        assertTrue(model.servers.size == 2)
        assertEquals(server, model.servers[1])
        assertEquals(server, model.selected.value)
    }

    @Test
    fun `test cancel pressed`() {
        assertEquals(true, model.open.value)
        model.cancelPressed()
        assertEquals(false, model.open.value)
    }

    @Test
    fun `test show with servers`() {
        val servers = listOf(
            ConnectionDetails(
                hostname = "hostname1", password = "password", port = 1, tls = false, autoconnect = true
            ), ConnectionDetails(
                hostname = "hostname2", password = "password", port = 2, tls = true, autoconnect = false
            )
        )
        val serversEditable = listOf(
            ConnectionDetailsEditable(
                hostname = "hostname1", password = "password", port = "1", tls = false, autoconnect = true
            ), ConnectionDetailsEditable(
                hostname = "hostname2", password = "password", port = "2", tls = true, autoconnect = false
            )
        )
        every { config[ClientSpec.servers] } returns servers
        model.show()
        assertEquals(serversEditable, model.servers)
        assertEquals(serversEditable[0], model.selected.value)
    }

    @Test
    fun `test show without servers`() {
        val servers = emptyList<ConnectionDetails>()
        every { config[ClientSpec.servers] } returns servers
        model.show()
        assertEquals(servers, model.servers)
        assertEquals(null, model.selected.value)
    }
}

internal class PortIntegerStringConvertTest {

    private val converter = PortIntegerStringConvert()

    @Test
    fun `test toString with int`() {
        assertEquals("6667", converter.toString(6667))
    }

    @Test
    fun `test toString with null`() {
        assertEquals("6667", converter.toString(null))
    }

    @Test
    fun `test fromString with int`() {
        assertEquals(6669, converter.fromString("6669"))
    }

    @Test
    fun `test fromString with null`() {
        assertEquals(6667, converter.fromString(null))
    }

    @Test
    fun `test fromString with int over`() {
        assertEquals(65535, converter.fromString("66536"))
    }

    @Test
    fun `test fromString with zero`() {
        assertEquals(1, converter.fromString("0"))
    }

    @Test
    fun `test fromString with less than zero`() {
        assertEquals(1, converter.fromString("-5"))
    }
}

internal class IntegerFilterTest {
    private val filter = IntegerFilter()

    @Test
    fun `test filter with letters`() {
        val change = mockk<Change>()
        every { change.text } returns "test"
        assertEquals(null, filter.apply(change))
    }

    @Test
    fun `test filter with mixed letters`() {
        val change = mockk<Change>()
        every { change.text } returns "6667test"
        assertEquals(null, filter.apply(change))
    }

    @Test
    fun `test filter with numbers`() {
        val change = mockk<Change>()
        every { change.text } returns "6667"
        assertEquals(change, filter.apply(change))
    }

    @Test
    fun `test filter with negative number`() {
        val change = mockk<Change>()
        every { change.text } returns "-5"
        assertEquals(null, filter.apply(change))
    }
}
