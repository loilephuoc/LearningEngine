plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("app.cash.sqldelight") version "2.0.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    id("com.android.application") version "9.1.1" apply false
    application
}

group = "vn.loi.learning"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(21)
}

sqldelight {
    databases {
        create("LearningEngineDatabase") {
            packageName.set("vn.loi.learning.infrastructure.persistence.sqlite")
        }
    }
}

dependencies {
    implementation(
        "org.jetbrains.kotlinx:" +
                "kotlinx-serialization-json:1.11.0"
    )
    implementation("app.cash.sqldelight:runtime:2.0.2")
    implementation("app.cash.sqldelight:sqlite-driver:2.0.2")

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

tasks.register<JavaExec>("reconcileIntermediatePublicTransportOrphan") {
    group = "application"
    description = "Runs the exact-ID audited Intermediate public-transport orphan repair."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("vn.loi.learning.adapter.jvm.ReconcileIntermediatePublicTransportOrphanMainKt")
}

tasks.register<JavaExec>("ipaPrefixCleanup") {
    group = "application"
    description = "Dry-runs or applies the one-time package-scoped IPA POS-prefix cleanup."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("vn.loi.learning.adapter.jvm.IpaPrefixCleanupMainKt")
}

tasks.withType<JavaExec>()
    .configureEach {
        jvmArgs(
            "-Dfile.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8"
        )
    }
