package com.dmdirc

import com.dmdirc.ktirc.model.ConnectionError
import com.jukusoft.i18n.I
import com.jukusoft.i18n.I.tr
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

fun initInternationalisation(path: Path, locale: String?) {
    if (!Files.exists(path)) {
        Files.createDirectory(path)
    }

    I.init(path.toFile(), Locale.ENGLISH, "messages")
    I.setLanguage(Locale.forLanguageTag(locale))
}

fun ConnectionError.translated() = when (this) {
    ConnectionError.Unknown -> tr("unknown error")
    ConnectionError.UnresolvableAddress -> tr("the address could not be resolved")
    ConnectionError.ConnectionRefused -> tr("the connection was refused")
    ConnectionError.BadTlsCertificate -> tr("the server's certificate was not valid")
}
