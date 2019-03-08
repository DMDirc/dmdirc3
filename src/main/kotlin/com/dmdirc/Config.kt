package com.dmdirc

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.Item
import com.uchuhimo.konf.source.yaml.toYaml
import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger

data class ConnectionDetails(
    val hostname: String,
    val password: String = "",
    val port: Int,
    val tls: Boolean = true,
    val autoconnect: Boolean = false,
    val autoJoin: List<String> = emptyList()
)

object ClientSpec : ConfigSpec("") {
    val servers by optional(listOf<ConnectionDetails>())
    val language by optional("en-GB")

    object DefaultProfile : ConfigSpec() {
        val nickname by optional("")
        val username by optional("")
        val realname by optional("")
    }

    object Formatting : ConfigSpec() {
        val timestamp by optional("HH:mm:ss")
        val channelEvent by optional("-- %s")
        val serverEvent by optional("** %s")
        val message by optional("<%s> %s")
        val action by optional("* %s %s")
        val notice by optional("-%s- %s")
    }

    object Display : ConfigSpec() {
        val embedImages by optional(true)
    }
}

class ClientConfig private constructor(private val path: Path, private val config: Config) {

    /**
     * Get associated value with specified item.
     *
     * @param item config item (a child of [ClientSpec])
     * @return associated value
     */
    operator fun <T> get(item: Item<T>): T = config[item]

    /**
     * Associate item with specified value.
     *
     * @param item config item (a child of [ClientSpec])
     * @param value associated value
     */
    operator fun <T> set(item: Item<T>, value: T) = config.set(item, value)

    /**
     * Attempts to save the config.
     *
     * If [newPath] is provided the config is saved to that path; otherwise
     * the config is saved to the path it was originally loaded from
     */
    fun save(newPath: Path? = null) =
        try {
            Files.newOutputStream(newPath ?: path).use { config.toYaml.toOutputStream(it) }
        } catch (ex: IOException) {
            logger.log(Level.WARNING, ex) { "Unable to load config file" }
        }

    companion object {

        private val logger = Logger.getLogger(ClientSpec::class.simpleName)

        /**
         * Attempts to load the client config from the given [path].
         *
         * If the [path] doesn't exist or cannot be read, a default config is returned.
         */
        fun loadFrom(path: Path): ClientConfig =
            with(Config { addSpec(ClientSpec) }) {
                try {
                    return ClientConfig(path, Files.newInputStream(path).use { from.yaml.inputStream(it) })
                } catch (ex: Exception) {
                    logger.log(Level.WARNING, ex) { "Unable to load config file" }
                }
                return ClientConfig(path, this)
            }

    }
}

private const val DIRECTORY_NAME = "dmdirc3"

/**
 * Gets the config directory that DMDirc should use.
 */
fun getConfigDirectory(): Path {
    val fs = FileSystems.getDefault()
    val path = fs.getConfigDirectory(
        System.getProperty("os.name"),
        fs.getPath(System.getProperty("user.home")),
        System.getenv()
    )
    if (!Files.isDirectory(path)) {
        Files.createDirectories(path)
    }
    return path
}

/**
 * Gets the config directory that DMDirc should use, given the [osName], [homeDir] and [envVars].
 */
fun FileSystem.getConfigDirectory(osName: String, homeDir: Path, envVars: Map<String, String>): Path =
    when {
        "DMDIRC_HOME" in envVars -> getPath(envVars["DMDIRC_HOME"])
        osName.startsWith("Mac OS") -> resolveMacConfigDirectory(homeDir)
        osName.startsWith("Windows") -> resolveWindowsConfigDirectory(homeDir, envVars["APPDATA"])
        else -> resolveOtherConfigDirectory(homeDir, envVars["XDG_CONFIG_HOME"])
    }.toAbsolutePath()

/**
 * Resolves the pre-defined config directory relative to the given [homeDir] for macs.
 */
private fun resolveMacConfigDirectory(homeDir: Path) =
    homeDir.resolve("Library").resolve("Preferences").resolve(DIRECTORY_NAME)

/**
 * Resolves the config directory for Windows, relative to either the [homeDir] or the [appDataDir] if available.
 */
private fun FileSystem.resolveWindowsConfigDirectory(homeDir: Path, appDataDir: String?) =
    if (appDataDir.isNullOrEmpty()) {
        homeDir.resolve(DIRECTORY_NAME)
    } else {
        getPath(appDataDir, DIRECTORY_NAME)
    }

/**
 * Resolves the config directory relative to the [homeDir] or [xdgConfigHome] if available.
 */
private fun FileSystem.resolveOtherConfigDirectory(homeDir: Path, xdgConfigHome: String?) =
    if (xdgConfigHome.isNullOrEmpty()) {
        homeDir.resolve(".$DIRECTORY_NAME")
    } else {
        getPath(xdgConfigHome, DIRECTORY_NAME)
    }
