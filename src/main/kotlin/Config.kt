package com.dmdirc

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.Item
import com.uchuhimo.konf.source.yaml.toYaml
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger

data class ConnectionDetails(
    var hostname: String,
    var password: String,
    var port: Int,
    var tls: Boolean = true,
    var autoconnect: Boolean = false
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

