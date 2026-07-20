plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
    id("org.jetbrains.compose") version "1.11.1"
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "vn.loi.learning.desktop.DesktopMainKt"
    }
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dstdout.encoding=UTF-8",
        "-Dstderr.encoding=UTF-8"
    )
}