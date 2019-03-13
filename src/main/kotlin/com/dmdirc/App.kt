package com.dmdirc

import com.bugsnag.Bugsnag
import javafx.application.Application
import javafx.application.HostServices
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.scene.Node
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.LogManager

internal lateinit var kodein: Kodein

class MainApp : Application() {
    override fun start(stage: Stage) {
        val bugsnag = Bugsnag("972c7b9be25508467fccdded43791bc5")
        kodein = createKodein(stage, hostServices, stage.titleProperty(), bugsnag)
        val config by kodein.instance<ClientConfig>()
        initInternationalisation(Paths.get("translations"), config[ClientSpec.language])
        with(stage) {
            minWidth = 800.0
            minHeight = 600.0
            scene = Scene(kodein.direct.instance<MainView>())
            show()
            kodein.direct.instance<FocusManager>().start()
        }
        GlobalScope.launch {
            installStyles(stage.scene, kodein.direct.instance<Path>().resolve("stylesheet.css"))
        }
    }
}

private fun createKodein(
    stage: Stage,
    hostServices: HostServices,
    titleProperty: StringProperty,
    bugsnag: Bugsnag
) = Kodein {
    bind<Bugsnag>() with singleton { bugsnag.apply { setAppVersion(instance("version")) } }
    bind<String>("version") with singleton { getVersion() }
    bind<Path>() with singleton { getConfigDirectory() }
    bind<ClientConfig>() with singleton { ClientConfig.loadFrom(instance<Path>().resolve("config.yml")) }
    bind<HostServices>() with instance(hostServices)
    bind<MainContract.Controller>() with singleton { MainController(instance(), factory()) }
    bind<StringProperty>("mainViewTitle") with singleton { titleProperty }
    bind<ObjectProperty<Node>>("dialogPane") with singleton { SimpleObjectProperty<Node>() }
    bind<MainView>() with singleton {
        MainView(
            instance(),
            instance(),
            provider(),
            provider(),
            provider(),
            instance(),
            instance("mainViewTitle"),
            instance("dialogPane"),
            provider()
        )
    }
    bind<FocusManager>() with singleton { FocusManager(stage, instance(), instance("dialogPane")) }
    bind<JoinDialog>() with provider {
        JoinDialog(instance(), instance("dialogPane"))
    }
    bind<SettingsDialog>() with provider {
        SettingsDialog(instance(), instance("dialogPane"))
    }
    bind<ServerlistDialog>() with provider {
        ServerlistDialog(instance(), instance("dialogPane"))
    }
    bind<WelcomePane>() with provider {
        WelcomePane(instance(), provider(), provider(), instance("version"))
    }
    bind<Stage>() with instance(stage)

    bind<ConnectionContract.Controller>() with factory { connectionDetails: ConnectionDetails ->
        Connection(connectionDetails, instance(), instance())
    }
    bind<JoinDialogContract.Controller>() with provider { JoinDialogController(instance()) }
    bind<JoinDialogContract.ViewModel>() with provider { JoinDialogModel(instance()) }

    bind<SettingsDialogContract.Controller>() with provider { SettingsDialogController(instance()) }
    bind<SettingsDialogContract.ViewModel>() with provider { SettingsDialogModel(instance(), instance()) }

    bind<ServerListDialogContract.Controller>() with provider { ServerListController(instance(), instance()) }
    bind<ServerListDialogContract.ViewModel>() with provider { ServerListModel(instance(), instance()) }
}

fun main() {
    LogManager.getLogManager().readConfiguration(MainApp::class.java.getResourceAsStream("/logs.properties"))
    Application.launch(MainApp::class.java)
}
