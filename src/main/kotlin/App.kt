package com.dmdirc

import com.jukusoft.i18n.I
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import tornadofx.App
import tornadofx.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.logging.LogManager


internal lateinit var kodein: Kodein

class MainApp : App(MainView::class) {
    override fun start(stage: Stage) {
        kodein = createKodein(stage)
        val config by kodein.instance<ClientConfig>()
        initInternationalisation(Path.of("translations"), config[ClientSpec.language])
        with(stage) {
            minWidth = 800.0
            minHeight = 600.0
            super.start(this)
        }
        GlobalScope.launch {
            installStyles(stage.scene, Paths.get("stylesheet.css"))
        }
    }
}

private fun createKodein(stage: Stage) = Kodein {
    bind<ClientConfig>() with singleton { ClientConfig.loadFrom(Paths.get("config.yml")) }
    bind<Stage>() with instance(stage)
    bind<MainController>() with singleton { MainController(instance()) }
    bind<MainContract.Controller>() with singleton { instance<MainController>() }

    bind<JoinDialogContract.Controller>() with provider { JoinDialogController(instance()) }
    bind<JoinDialogContract.ViewModel>() with provider { JoinDialogModel(instance()) }
    bind<JoinDialog>() with provider { JoinDialog(instance(), instance()) }

    bind<SettingsDialogContract.Controller>() with provider { SettingsDialogController(instance()) }
    bind<SettingsDialogContract.ViewModel>() with provider { SettingsDialogModel(instance(), instance()) }
    bind<SettingsDialog>() with provider { SettingsDialog(instance()) }
}

private fun initInternationalisation(path: Path, locale: String?) {
    if (!Files.exists(path)) {
        Files.createDirectory(path)
    }

    I.init(path.toFile(), Locale.ENGLISH, "messages")
    I.setLanguage(Locale.forLanguageTag(locale))
}

fun main(args: Array<String>) {
    LogManager.getLogManager().readConfiguration(MainApp::class.java.getResourceAsStream("/logs.properties"))
    launch<MainApp>(args)
}
