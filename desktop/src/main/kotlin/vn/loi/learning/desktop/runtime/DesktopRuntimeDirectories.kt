package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import java.nio.file.Path

data class DesktopRuntimeDirectories(
    val data: Path,
    val config: Path,
    val cache: Path,
    val logs: Path,
    val temp: Path,
    val legacyDataInUse: Boolean
) {
    init {
        listOf(data, config, cache, logs, temp).forEach { path ->
            require(path.isAbsolute) {
                "Desktop runtime directory must be absolute: $path"
            }
        }
    }
}

enum class DesktopOperatingSystem {
    WINDOWS,
    MACOS,
    LINUX
}

object DesktopRuntimeDirectoryResolver {
    fun resolve(): DesktopRuntimeDirectories {
        val userHome =
            Path.of(
                requireNotNull(System.getProperty("user.home")) {
                    "Missing system property: user.home"
                }
            )

        val temporaryRoot =
            Path.of(
                requireNotNull(System.getProperty("java.io.tmpdir")) {
                    "Missing system property: java.io.tmpdir"
                }
            )

        val legacyData = userHome.resolve(".learning-engine").resolve("data")

        return resolve(
            operatingSystem = detectOperatingSystem(System.getProperty("os.name", "")),
            userHome = userHome,
            temporaryRoot = temporaryRoot,
            environment = System.getenv(),
            legacyDataExists = Files.isDirectory(legacyData)
        )
    }

    internal fun resolve(
        operatingSystem: DesktopOperatingSystem,
        userHome: Path,
        temporaryRoot: Path,
        environment: Map<String, String>,
        legacyDataExists: Boolean
    ): DesktopRuntimeDirectories {
        require(userHome.isAbsolute) { "User home must be absolute: $userHome" }
        require(temporaryRoot.isAbsolute) {
            "Temporary root must be absolute: $temporaryRoot"
        }

        val directoryName = DesktopApplicationIdentity.DIRECTORY_NAME
        val legacyData = userHome.resolve(".learning-engine").resolve("data")

        val resolved =
            when (operatingSystem) {
                DesktopOperatingSystem.WINDOWS -> {
                    val root =
                        environment.absolutePath("LOCALAPPDATA")
                            ?: userHome.resolve("AppData").resolve("Local")
                    val applicationRoot = root.resolve(directoryName)

                    DesktopRuntimeDirectories(
                        data = applicationRoot.resolve("data"),
                        config = applicationRoot.resolve("config"),
                        cache = applicationRoot.resolve("cache"),
                        logs = applicationRoot.resolve("logs"),
                        temp = temporaryRoot.resolve(directoryName),
                        legacyDataInUse = false
                    )
                }

                DesktopOperatingSystem.MACOS -> {
                    val supportRoot =
                        userHome.resolve("Library").resolve("Application Support")
                            .resolve(directoryName)

                    DesktopRuntimeDirectories(
                        data = supportRoot.resolve("data"),
                        config = supportRoot.resolve("config"),
                        cache = userHome.resolve("Library").resolve("Caches")
                            .resolve(directoryName),
                        logs = userHome.resolve("Library").resolve("Logs")
                            .resolve(directoryName),
                        temp = temporaryRoot.resolve(directoryName),
                        legacyDataInUse = false
                    )
                }

                DesktopOperatingSystem.LINUX -> {
                    val dataRoot =
                        environment.absolutePath("XDG_DATA_HOME")
                            ?: userHome.resolve(".local").resolve("share")
                    val configRoot =
                        environment.absolutePath("XDG_CONFIG_HOME")
                            ?: userHome.resolve(".config")
                    val cacheRoot =
                        environment.absolutePath("XDG_CACHE_HOME")
                            ?: userHome.resolve(".cache")
                    val stateRoot =
                        environment.absolutePath("XDG_STATE_HOME")
                            ?: userHome.resolve(".local").resolve("state")

                    DesktopRuntimeDirectories(
                        data = dataRoot.resolve(directoryName),
                        config = configRoot.resolve(directoryName),
                        cache = cacheRoot.resolve(directoryName),
                        logs = stateRoot.resolve(directoryName).resolve("logs"),
                        temp = temporaryRoot.resolve(directoryName),
                        legacyDataInUse = false
                    )
                }
            }

        return if (legacyDataExists) {
            resolved.copy(
                data = legacyData,
                legacyDataInUse = true
            )
        } else {
            resolved
        }
    }

    internal fun detectOperatingSystem(osName: String): DesktopOperatingSystem =
        when {
            osName.startsWith("Windows", ignoreCase = true) ->
                DesktopOperatingSystem.WINDOWS

            osName.startsWith("Mac", ignoreCase = true) ||
                osName.startsWith("Darwin", ignoreCase = true) ->
                DesktopOperatingSystem.MACOS

            else ->
                DesktopOperatingSystem.LINUX
        }

    private fun Map<String, String>.absolutePath(key: String): Path? =
        get(key)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let(Path::of)
            ?.takeIf(Path::isAbsolute)
}
