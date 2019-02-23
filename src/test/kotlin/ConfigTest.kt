
import com.dmdirc.ClientSpec
import com.dmdirc.ConnectionDetails
import com.dmdirc.loadConfig
import com.dmdirc.save
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.uchuhimo.konf.Config
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

private class ConfigTest {

    private val fs = Jimfs.newFileSystem(Configuration.unix())

    @Test
    fun `returns default config if file is not found`() {
        val config = loadConfig(fs.getPath("/blah.yml"))
        assertNotNull(config)
        assertTrue(config[ClientSpec.servers].isEmpty())
    }

    @Test
    fun `returns default config if file throws io exception`() {
        Files.createDirectory(fs.getPath("/dir"))
        val config = loadConfig(fs.getPath("/dir"))
        assertNotNull(config)
        assertTrue(config[ClientSpec.servers].isEmpty())
    }

    @Test
    fun `writes config to disk`() {
        val config = Config { addSpec(ClientSpec) }
        config.save(fs.getPath("/out.yml"))
        assertTrue(Files.exists(fs.getPath("/out.yml")))
    }

    @Test
    fun `loads previously written config file`() {
        val config = Config { addSpec(ClientSpec) }
        config[ClientSpec.servers] = listOf(ConnectionDetails("host", "pass", 1234, true))

        config.save(fs.getPath("/out.yml"))
        val newConfig = loadConfig(fs.getPath("/out.yml"))
        val servers = newConfig[ClientSpec.servers]
        assertEquals(1, servers.size)
        assertEquals(ConnectionDetails("host", "pass", 1234, true), servers[0])
    }

}