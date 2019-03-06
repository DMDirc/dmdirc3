
import com.install4j.gradle.Install4jTask
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.dmdirc"
version = "0.1-SNAPSHOT"
val mainClass = "com.dmdirc.AppKt"

plugins {
    application
    jacoco
    kotlin("jvm").version("1.3.21")
    id("org.openjfx.javafxplugin").version("0.0.7")
    id("name.remal.check-updates") version "1.0.113"
    id("com.install4j.gradle") version "7.0.9"
}

install4j {
    if (OperatingSystem.current().isLinux) {
        installDir = File("/opt/install4j7/")
    } else if (OperatingSystem.current().isWindows) {
        installDir = File("C:\\Program Files\\install4j7")
    }
    license = System.getenv("i4jlicense")
}

jacoco {
    toolVersion = "0.8.3"
}

repositories {
    mavenCentral()
    jcenter()
    mavenLocal()
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

javafx {
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

application {
    mainClassName = mainClass
}

dependencies {
    implementation("org.controlsfx:controlsfx:9.0.0")
    implementation("org.fxmisc.richtext:richtextfx:0.9.3")
    implementation("com.dmdirc:ktirc:0.10.3")
    implementation("com.uchuhimo:konf:0.13.1")
    implementation("org.kodein.di:kodein-di-generic-jvm:6.1.0")
    implementation("com.jukusoft:easy-i18n-gettext:1.2.0")
    implementation("de.jensd:fontawesomefx-fontawesome:4.7.0-5")
    implementation("de.jensd:fontawesomefx-commons:11.0")
    implementation("de.jensd:fontawesomefx-controls:11.0")

    runtime("org.openjfx:javafx-graphics:$javafx.version:win")
    runtime("org.openjfx:javafx-graphics:$javafx.version:linux")
    runtime("org.openjfx:javafx-graphics:$javafx.version:mac")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.0")
    testImplementation("io.mockk:mockk:1.9.1")
    testImplementation("com.google.jimfs:jimfs:1.1")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.4.0")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.3.21")
        }
    }
}

tasks {

    withType<Install4jTask> {
        dependsOn("jar")
        projectFile = "dmdirc.install4j"
        debug = true
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Wrapper> {
        gradleVersion = "5.2.1"
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<Jar> {
        manifest.attributes.apply {
            put("Main-Class", mainClass)
        }
        from(configurations.runtimeClasspath.get().map {
            if (it.isDirectory) {
                it
            } else {
                zipTree(it).matching {
                    exclude("META-INF/*.SF")
                    exclude("META-INF/*.DSA")
                    exclude("META-INF/*.RSA")
                }
            }
        })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<JacocoReport> {
        executionData(fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec"))

        sourceSets(sourceSets["main"])

        reports {
            xml.isEnabled = true
            xml.destination = File("$buildDir/reports/jacoco/report.xml")
            html.isEnabled = true
            csv.isEnabled = false
        }

        dependsOn("test")
    }

}
