plugins {
    kotlin("jvm") version "2.4.0"
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
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("vn.loi.learning.MainKt")
}