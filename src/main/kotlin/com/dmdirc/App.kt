package com.dmdirc

import com.jukusoft.i18n.I
import javafx.application.Application
import javafx.application.HostServices
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.bindings.subTypes
import org.kodein.di.generic.*
import org.kodein.di.jvmType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.logging.LogManager


internal lateinit var kodein: Kodein

class MainApp : Application() {
    override fun start(stage: Stage) {
        kodein = createKodein(stage, hostServices)
        val config by kodein.instance<ClientConfig>()
        initInternationalisation(Path.of("translations"), config[ClientSpec.language])
        val controller: MainContract.Controller by kodein.instance()
        val joinDialogProvider: () -> JoinDialog by kodein.provider()
        val settingsDialogProvider: () -> SettingsDialog by kodein.provider()
        with(stage) {
            minWidth = 800.0
            minHeight = 600.0
            scene = Scene(MainView(controller, config, joinDialogProvider, settingsDialogProvider, stage, titleProperty()))
            show()
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

    bind<ConnectionContract.Controller>() with factory { connectionDetails: ConnectionDetails -> Connection(connectionDetails, instance(), instance()) }
    bind<JoinDialogContract.Controller>() with provider { JoinDialogController(instance()) }
    bind<JoinDialogContract.ViewModel>() with provider { JoinDialogModel(instance()) }

    bind<SettingsDialogContract.Controller>() with provider { SettingsDialogController(instance()) }
    bind<SettingsDialogContract.ViewModel>() with provider { SettingsDialogModel(instance(), instance()) }
}

fun main(args: Array<String>) {
    LogManager.getLogManager().readConfiguration(MainApp::class.java.getResourceAsStream("/logs.properties"))
    Application.launch(MainApp::class.java)
}

fun <T> List<T>.observable(): ObservableList<T> = FXCollections.observableList(this)
fun <T> Set<T>.observable(): ObservableSet<T> = FXCollections.observableSet(this)
fun <T> ObservableSet<T>.synchronized(): ObservableSet<T> = FXCollections.synchronizedObservableSet(this)
fun <T> ObservableSet<T>.readOnly(): ObservableSet<T> = FXCollections.unmodifiableObservableSet(this)
fun <K, V> Map<K, V>.observable(): ObservableMap<K, V> = FXCollections.observableMap(this)

// For testing purposes: we can swap out the Platform call to something we control
internal var runLaterProvider: (Runnable) -> Unit = Platform::runLater
fun runLater(block: () -> Unit) = runLaterProvider(Runnable(block))
