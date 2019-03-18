package com.dmdirc

import com.dmdirc.edgar.Edgar
import com.dmdirc.edgar.Edgar.tr
import com.dmdirc.ktirc.model.ConnectionError
import java.nio.file.Files
import java.nio.file.Path

fun initInternationalisation(path: Path, locale: String?) {
    if (!Files.exists(path)) {
        Files.createDirectory(path)
    }

    Edgar.init(path)
    try {
        Edgar.setLanguage(locale?.replace('-', '_') ?: "en_GB")
    } catch (_: IllegalArgumentException) {
        // Language not found
        Edgar.setLanguage("en_GB")
    }
}

fun ConnectionError.translated() = when (this) {
    ConnectionError.Unknown -> tr("unknown error")
    ConnectionError.UnresolvableAddress -> tr("the address could not be resolved")
    ConnectionError.ConnectionRefused -> tr("the connection was refused")
    ConnectionError.BadTlsCertificate -> tr("the server's certificate was not valid")
}
