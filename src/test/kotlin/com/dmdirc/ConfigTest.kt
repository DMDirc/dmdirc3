package com.dmdirc

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

private class ConfigTest {

    private val fs = Jimfs.newFileSystem(Configuration.unix())

    @Test
    fun `returns default config if file is not found`() {
        val config = ClientConfig.loadFrom(fs.getPath("/blah.yml"))
        assertNotNull(config)
        assertTrue(config[ClientSpec.servers].isEmpty())
    }

    @Test
    fun `returns default config if file throws io exception`() {
        Files.createDirectory(fs.getPath("/dir"))
        val config = ClientConfig.loadFrom(fs.getPath("/dir"))
        assertNotNull(config)
        assertTrue(config[ClientSpec.servers].isEmpty())
    }

    @Test
    fun `writes config to specified path`() {
        val config = ClientConfig.loadFrom(fs.getPath("/whatever"))
        config.save(fs.getPath("/out.yml"))
        assertTrue(Files.exists(fs.getPath("/out.yml")))
    }

    @Test
    fun `writes config to original path`() {
        val config = ClientConfig.loadFrom(fs.getPath("/config.yml"))
        config.save()
        assertTrue(Files.exists(fs.getPath("/config.yml")))
    }

    @Test
    fun `loads previously written config file`() {
        val config = ClientConfig.loadFrom(fs.getPath("/whatever"))
        config[ClientSpec.servers] = listOf(ConnectionDetails("host", "pass", 1234, true))

        config.save(fs.getPath("/out.yml"))
        val newConfig = ClientConfig.loadFrom(fs.getPath("/out.yml"))
        val servers = newConfig[ClientSpec.servers]
        assertEquals(1, servers.size)
        assertEquals(ConnectionDetails("host", "pass", 1234, true), servers[0])
    }

    @Test
    fun `gets config directory if DMDIRC_HOME is set`() {
        val path = fs.getConfigDirectory("Windows 10", fs.getPath("/users/burn"), mapOf("DMDIRC_HOME" to "/etc/dmdirc"))
        assertEquals("/etc/dmdirc", path.toString())
    }

    @Test
    fun `gets config directory on Mac`() {
        val path = fs.getConfigDirectory("Mac OS X", fs.getPath("/users/burn"), emptyMap())
        assertEquals("/users/burn/Library/Preferences/dmdirc3", path.toString())
    }

    @Test
    fun `gets config directory on Windows with AppData set`() {
        val path = fs.getConfigDirectory("Windows 10", fs.getPath("/users/burn"), mapOf("APPDATA" to "/appdata"))
        assertEquals("/appdata/dmdirc3", path.toString())
    }

    @Test
    fun `gets config directory on Windows with AppData not set`() {
        val path = fs.getConfigDirectory("Windows 10", fs.getPath("/users/burn"), emptyMap())
        assertEquals("/users/burn/dmdirc3", path.toString())
    }

    @Test
    fun `gets config directory on other OSes with XDG_CONFIG_HOME not set`() {
        val path = fs.getConfigDirectory("Linux", fs.getPath("/users/burn"), emptyMap())
        assertEquals("/users/burn/.dmdirc3", path.toString())
    }

    @Test
    fun `gets config directory on other OSes with XDG_CONFIG_HOME set`() {
        val path =
            fs.getConfigDirectory("Linux", fs.getPath("/users/burn"), mapOf("XDG_CONFIG_HOME" to "/users/burn/.config"))
        assertEquals("/users/burn/.config/dmdirc3", path.toString())
    }
}
