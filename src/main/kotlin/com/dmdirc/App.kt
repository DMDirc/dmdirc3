package com.dmdirc

import com.jukusoft.i18n.I
import javafx.application.HostServices
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.bindings.subTypes
import org.kodein.di.generic.*
import org.kodein.di.jvmType
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
        kodein = createKodein(stage, hostServices)
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

private fun createKodein(stage: Stage, hostServices: HostServices) = Kodein {
    bind<ClientConfig>() with singleton { ClientConfig.loadFrom(Paths.get("config.yml")) }
    bind<HostServices>() with instance(hostServices)
    bind<MainContract.Controller>() with singleton { MainController(instance(), factory()) }

    bind<Stage>().subTypes() with {
        when (it.jvmType) {
            JoinDialog::class.java -> provider { JoinDialog(instance(), instance()) }
            SettingsDialog::class.java -> provider { SettingsDialog(instance()) }
            else -> instance(stage)
        }
    }

    bind<Connection>() with factory { connectionDetails: ConnectionDetails -> Connection(connectionDetails, instance(), instance()) }
    bind<JoinDialogContract.Controller>() with provider { JoinDialogController(instance()) }
    bind<JoinDialogContract.ViewModel>() with provider { JoinDialogModel(instance()) }

    bind<SettingsDialogContract.Controller>() with provider { SettingsDialogController(instance()) }
    bind<SettingsDialogContract.ViewModel>() with provider { SettingsDialogModel(instance(), instance()) }
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

fun <T> List<T>.observable(): ObservableList<T> = FXCollections.observableList(this)
fun <T> Set<T>.observable(): ObservableSet<T> = FXCollections.observableSet(this)
fun <T> ObservableSet<T>.synchronized(): ObservableSet<T> = FXCollections.synchronizedObservableSet(this)
fun <T> ObservableSet<T>.readOnly(): ObservableSet<T> = FXCollections.unmodifiableObservableSet(this)
fun <K, V> Map<K, V>.observable(): ObservableMap<K, V> = FXCollections.observableMap(this)