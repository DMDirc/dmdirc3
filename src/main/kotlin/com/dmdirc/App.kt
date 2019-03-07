package com.dmdirc

import javafx.application.Application
import javafx.application.HostServices
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.scene.Node
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.bindings.subTypes
import org.kodein.di.direct
import org.kodein.di.generic.*
import org.kodein.di.jvmType
import java.nio.file.Paths
import java.util.function.BiFunction
import java.util.logging.LogManager

internal lateinit var kodein: Kodein

class MainApp : Application() {
    override fun start(stage: Stage) {
        kodein = createKodein(stage, hostServices, stage.titleProperty())
        val config by kodein.instance<ClientConfig>()
        initInternationalisation(Paths.get("translations"), config[ClientSpec.language])
        with(stage) {
            minWidth = 800.0
            minHeight = 600.0
            scene = Scene(kodein.direct.instance())
            show()
        }
        GlobalScope.launch {
            installStyles(stage.scene, Paths.get("stylesheet.css"))
        }
    }
}

private fun createKodein(stage: Stage, hostServices: HostServices, titleProperty: StringProperty) = Kodein {
    bind<ClientConfig>() with singleton { ClientConfig.loadFrom(Paths.get("config.yml")) }
    bind<HostServices>() with instance(hostServices)
    bind<MainContract.Controller>() with singleton { MainController(instance(), factory()) }
    bind<StringProperty>("mainViewTitle") with singleton { titleProperty }
    bind<ObjectProperty<Node>>("dialogPane") with singleton { SimpleObjectProperty<Node>() }
    bind<MainView>() with singleton {
        MainView(instance(), instance(), provider(), provider(), instance(), instance("mainViewTitle"), instance("dialogPane"))
    }

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

fun main() {
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

fun <T, Y> Property<T>.bindTransform(other: Property<Y>, biFunction: BiFunction<T, T, Y>) {
    addListener { _, oldValue, newValue ->
        other.value = biFunction.apply(oldValue, newValue)
    }
}