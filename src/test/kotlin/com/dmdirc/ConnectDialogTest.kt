package com.dmdirc

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ServerListControllerTest {
    private val mainController = mockk<MainContract.Controller>()
    private val config = mockk<ClientConfig>()
    private val controller = ServerListController(mainController, config)

    @Test
    fun `test connect called`() {
        val connectionEditable = ConnectionDetailsEditable(
            hostname = "hostname", password = "password", port = 1, tls = false, autoconnect = true
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
            hostname = "hostname", password = "password", port = 1, tls = false, autoconnect = true
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
                hostname = "hostname1", password = "password", port = 1, tls = false, autoconnect = true
            ), ConnectionDetailsEditable(
                hostname = "hostname2", password = "password", port = 2, tls = true, autoconnect = false
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
    val model = ServerListModel(controller, config)

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
            hostname = "hostname", password = "password", port = 1, tls = false, autoconnect = true
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
                hostname = "hostname1", password = "password", port = 1, tls = false, autoconnect = true
            ), ConnectionDetailsEditable(
                hostname = "hostname2", password = "password", port = 2, tls = true, autoconnect = false
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
                hostname = "hostname1", password = "password", port = 1, tls = false, autoconnect = true
            ), ConnectionDetailsEditable(
                hostname = "hostname2", password = "password", port = 2, tls = true, autoconnect = false
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
    fun `test close pressed`() {
        assertEquals(true, model.open.value)
        model.closeDialog()
        assertEquals(false, model.open.value)
    }
}
