plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    id("com.android.application") version "9.1.1" apply false
    application
}

group = "vn.loi.learning"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(
        "org.jetbrains.kotlinx:" +
                "kotlinx-serialization-json:1.11.0"
    )

    testImplementation(
        kotlin(
            "test"
        )
    )
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set(
        "vn.loi.learning.MainKt"
    )
}

tasks.register<JavaExec>(
    "legacyPackageImport"
) {
    group =
        "application"

    description =
        "Imports legacy JSON and OPD3 PKG packages."

    classpath =
        sourceSets["main"]
            .runtimeClasspath

    mainClass.set(
        "vn.loi.learning.adapter.jvm." +
                "LegacyPackageImportMainKt"
    )
}

tasks.withType<JavaExec>()
    .configureEach {
        jvmArgs(
            "-Dfile.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8"
        )
    }
