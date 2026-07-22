plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
    id("org.jetbrains.compose") version "1.11.1"
}

val desktopPackageName = "LearningEngine"
val desktopPackageVersion =
    rootProject.version.toString()
        .substringBefore('-')
        .split('.')
        .let { components ->
            (components + List((3 - components.size).coerceAtLeast(0)) { "0" })
                .take(3)
                .joinToString(".")
        }

val generatedBuildMetadataDirectory =
    layout.buildDirectory.dir("generated/resources/build-metadata")

val generateDesktopBuildMetadata =
    tasks.register("generateDesktopBuildMetadata") {
        val outputFile =
            generatedBuildMetadataDirectory.map { directory ->
                directory.file("learning-engine-build.properties")
            }
        val applicationVersion = rootProject.version.toString()
        val buildChannel =
            providers.gradleProperty("learningEngineBuildChannel").orElse("development")
        val buildRevision =
            providers.gradleProperty("learningEngineBuildRevision").orElse("local")
        val buildNumber =
            providers.gradleProperty("learningEngineBuildNumber").orElse("0")

        inputs.property("applicationVersion", applicationVersion)
        inputs.property("buildChannel", buildChannel)
        inputs.property("buildRevision", buildRevision)
        inputs.property("buildNumber", buildNumber)
        outputs.file(outputFile)

        doLast {
            val target = outputFile.get().asFile
            target.parentFile.mkdirs()
            target.writeText(
                "application.version=$applicationVersion\n" +
                    "build.channel=${buildChannel.get()}\n" +
                    "build.revision=${buildRevision.get()}\n" +
                    "build.number=${buildNumber.get()}\n" +
                    "distribution.package.name=$desktopPackageName\n" +
                    "distribution.package.version=$desktopPackageVersion\n" +
                    "distribution.windows.formats=msi,exe\n",
                Charsets.UTF_8
            )
        }
    }

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(21)
}

sourceSets.main {
    resources.srcDir(generatedBuildMetadataDirectory)
}

tasks.processResources {
    dependsOn(generateDesktopBuildMetadata)
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

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = desktopPackageName
            packageVersion = desktopPackageVersion
            description = "Desktop learning application"
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dstdout.encoding=UTF-8",
        "-Dstderr.encoding=UTF-8"
    )
}
