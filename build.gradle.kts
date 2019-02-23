import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.dmdirc"
version = "0.1-SNAPSHOT"
val mainClass = "com.dmdirc.AppKt"

plugins {
    application
    kotlin("jvm").version("1.3.20")
    id("org.openjfx.javafxplugin").version("0.0.7")
}

repositories {
    mavenCentral()
    jcenter()
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

javafx {
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClassName = mainClass
}

dependencies {
    implementation("no.tornado:tornadofx:1.7.18")
    implementation("no.tornado:tornadofx-controlsfx:0.1")
    implementation("org.fxmisc.richtext:richtextfx:0.9.2")
    implementation("com.dmdirc:ktirc:+")
    implementation("com.uchuhimo:konf:0.13.1")

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
            useVersion("1.3.20")
        }
        resolutionStrategy.cacheChangingModulesFor(2, TimeUnit.MINUTES)
        resolutionStrategy.cacheDynamicVersionsFor(2, TimeUnit.MINUTES)
    }
}

tasks {

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
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }

}
