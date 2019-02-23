package com.dmdirc

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.yaml.toYaml
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger

private val logger = Logger.getLogger(ClientSpec::class.simpleName)

data class ConnectionDetails(val hostname: String, val password: String, val port: Int, val tls: Boolean, val autoconnect: Boolean)

object ClientSpec : ConfigSpec("") {
    val servers by optional(listOf<ConnectionDetails>())
}

/**
 * Attempts to load the client config from the given [path].
 *
 * If the [path] doesn't exist or cannot be read, a default config is returned.
 */
fun loadConfig(path: Path): Config =
    with(Config { addSpec(ClientSpec) }) {
        try {
            return Files.newInputStream(path).use { from.yaml.inputStream(it) }
        } catch (ex: IOException) {
            logger.log(Level.WARNING, ex) { "Unable to load config file" }
        }
        return this
    }

/**
 * Attempts to save the client config to the given [path].
 */
fun Config.save(path: Path) =
    try {
        Files.newOutputStream(path).use { toYaml.toOutputStream(it) }
    } catch (ex: IOException) {
        logger.log(Level.WARNING, ex) { "Unable to load config file" }
    }
