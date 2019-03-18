package com.dmdirc

import com.bugsnag.Bugsnag
import javafx.application.Application
import javafx.application.HostServices
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.scene.Node
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.logging.LogManager

internal lateinit var kodein: Kodein

class MainApp : Application() {
    override fun start(stage: Stage) {
        kodein = createKodein(stage, hostServices, stage.titleProperty())
        kodein.direct.instance<ErrorReporter>().init()
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

interface ErrorReporter {
    fun init()
    fun notify(throwable: Throwable)
}

class BugsnagErrorReporter(private val version: String) : ErrorReporter {
    private val bugsnag = Bugsnag("972c7b9be25508467fccdded43791bc5")

    override fun init() {
        bugsnag.apply {
            setAppVersion(version)
            setReleaseStage(if (version == "dev") "development" else "production")
            setProjectPackages("com.dmdirc")
            startSession()
            addCallback { it.device.remove("hostname") }
        }
    }

    override fun notify(throwable: Throwable) {
        bugsnag.notify(throwable)
    }
}

class StderrErrorReporter : ErrorReporter {
    override fun init() {}
    override fun notify(throwable: Throwable) {
        throwable.printStackTrace(System.err)
    }
}

private fun createKodein(
    stage: Stage,
    hostServices: HostServices,
    titleProperty: StringProperty
) = Kodein {
    bind<ErrorReporter>() with eagerSingleton {
        if (instance<ClientConfig>()[ClientSpec.sendBugReports]) {
            BugsnagErrorReporter(instance("version"))
        } else {
            StderrErrorReporter()
        }
    }
    bind<Property<WindowModel>>() with singleton { SimpleObjectProperty<WindowModel>().threadAsserting() as Property<WindowModel> }
    bind<String>("version") with singleton { getVersion() }
    bind<Path>() with singleton { getConfigDirectory() }
    bind<ClientConfig>() with singleton { ClientConfig.loadFrom(instance<Path>().resolve("config.yml")) }
    bind<HostServices>() with instance(hostServices)
    bind<MainContract.Controller>() with singleton { MainController(instance(), instance(), factory()) }
    bind<StringProperty>("mainViewTitle") with singleton { titleProperty }
    bind<ObjectProperty<Node>>("dialogPane") with singleton { SimpleObjectProperty<Node>() }
    bind<MainView>() with singleton {
        MainView(
            instance(),
            instance(),
            factory(),
            factory(),
            factory(),
            instance(),
            instance("mainViewTitle"),
            instance("dialogPane"),
            provider()
        )
    }
    bind<FocusManager>() with singleton { FocusManager(stage, instance(), instance("dialogPane")) }
    bind<JoinDialog>() with factory { mainview: MainView ->
        JoinDialog(instance(), mainview)
    }
    bind<SettingsDialog>() with factory { mainview: MainView ->
        SettingsDialog(instance(), mainview)
    }
    bind<ServerlistDialog>() with factory { mainview: MainView ->
        ServerlistDialog(instance(), mainview)
    }
    bind<WelcomePane>() with provider {
        WelcomePane(instance(), instance("version"))
    }
    bind<Stage>() with instance(stage)
    bind<NotificationManager>() with singleton { NotificationManager(instance(), instance()) }

    bind<ConnectionContract.Controller>() with factory { connectionDetails: ConnectionDetails ->
        Connection(connectionDetails, instance(), instance(), factory())
    }
    bind<JoinDialogContract.Controller>() with provider { JoinDialogController(instance()) }
    bind<JoinDialogContract.ViewModel>() with provider { JoinDialogModel(instance()) }

    bind<SettingsDialogContract.Controller>() with provider { SettingsDialogController(instance()) }
    bind<SettingsDialogContract.ViewModel>() with provider { SettingsDialogModel(instance(), instance()) }

    bind<ServerListDialogContract.Controller>() with provider { ServerListController(instance(), instance()) }
    bind<ServerListDialogContract.ViewModel>() with provider { ServerListModel(instance(), instance()) }

    bind<OkHttpClient>() with singleton {
        OkHttpClient().newBuilder()
            .callTimeout(Duration.ofSeconds(2))
            .build()
    }
    bind<Request>() with factory { url: String ->
        Request.Builder().url(url).build()
    }
    bind<Request>() with factory { url: URL ->
        Request.Builder().url(url).build()
    }
    bind<ImageLoader>() with factory { url: String ->
        ImageLoader(url, instance(), factory())
    }
    bind<WindowUI>() with factory { model: WindowModel ->
        WindowUI(model, instance(), factory())
    }
}

fun main() {
    LogManager.getLogManager().readConfiguration(MainApp::class.java.getResourceAsStream("/logs.properties"))
    Application.launch(MainApp::class.java)
}
