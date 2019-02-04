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
    implementation("no.tornado:tornadofx:1.7.17")
    implementation("no.tornado:tornadofx-controlsfx:0.1")
    implementation("org.fxmisc.richtext:richtextfx:0.9.2")
    implementation("com.dmdirc:ktirc:+")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Wrapper> {
    gradleVersion = "5.1.1"
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

tasks.withType<Jar> {
    manifest.attributes.apply {
        put("Main-Class", mainClass)
    }
    configurations.compile.get().toList().forEach { println(it)}
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}