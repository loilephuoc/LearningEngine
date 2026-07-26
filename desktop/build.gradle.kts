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
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "vn.loi.learning.desktop.DesktopMainKt"

        nativeDistributions {
            modules("jdk.accessibility")
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

tasks.register("verifyWindowsLauncher") {
    group = "verification"
    description = "Starts the packaged Windows launcher with an isolated accessibility-enabled profile."
    dependsOn("createDistributable")

    onlyIf {
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    }

    doLast {
        val distribution =
            layout.buildDirectory.dir("compose/binaries/main/app/$desktopPackageName").get().asFile
        val launcher = distribution.resolve("$desktopPackageName.exe")
        check(launcher.isFile) { "Missing packaged Windows launcher: $launcher" }

        val probe = layout.buildDirectory.dir("launcher-smoke").get().asFile
        probe.deleteRecursively()
        probe.mkdirs()
        probe.resolve(".accessibility.properties").writeText(
            "assistive_technologies=com.sun.java.accessibility.AccessBridge\n",
            Charsets.UTF_8
        )
        val localAppData = probe.resolve("local-app-data").apply { mkdirs() }
        val temporary = probe.resolve("temp").apply { mkdirs() }
        val output = probe.resolve("launcher-output.txt")

        val process =
            ProcessBuilder(launcher.absolutePath)
                .directory(distribution)
                .redirectErrorStream(true)
                .redirectOutput(output)
                .apply {
                    environment()["LOCALAPPDATA"] = localAppData.absolutePath
                    environment()["TEMP"] = temporary.absolutePath
                    environment()["TMP"] = temporary.absolutePath
                    environment()["JAVA_TOOL_OPTIONS"] =
                        "-Duser.home=${probe.absolutePath.replace('\\', '/')} " +
                            "-DlearningEngine.startupVerification=true"
                }
                .start()

        val deadline = System.nanoTime() + 30_000_000_000L
        while (process.isAlive && System.nanoTime() < deadline) {
            Thread.sleep(100)
        }
        val exited = !process.isAlive
        if (!exited) {
            process.destroyForcibly()
            process.waitFor()
        }
        val diagnostic = output.takeIf { it.isFile }?.readText(Charsets.UTF_8).orEmpty()
        check(exited) { "Packaged Windows launcher did not finish startup verification.\n$diagnostic" }
        check(process.exitValue() == 0) {
            "Packaged Windows launcher failed startup verification with exit code " +
                "${process.exitValue()}.\n$diagnostic"
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
