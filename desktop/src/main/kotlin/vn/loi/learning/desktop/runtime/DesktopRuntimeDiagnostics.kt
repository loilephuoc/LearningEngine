package vn.loi.learning.desktop.runtime

import java.nio.file.Path

data class DesktopRuntimeDiagnostics(
    val applicationId: String,
    val applicationName: String,
    val version: String,
    val buildChannel: String,
    val buildRevision: String,
    val buildNumber: String,
    val operatingSystem: String,
    val architecture: String,
    val javaRuntime: String,
    val dataDirectory: String,
    val configDirectory: String,
    val logsDirectory: String,
    val logFile: String,
    val legacyDataInUse: Boolean
) {
    fun supportSummary(): String =
        listOf(
            "Application: $applicationName",
            "Application ID: $applicationId",
            "Version: $version",
            "Build channel: $buildChannel",
            "Build revision: $buildRevision",
            "Build number: $buildNumber",
            "Operating system: $operatingSystem",
            "Architecture: $architecture",
            "Java runtime: $javaRuntime",
            "Data directory: $dataDirectory",
            "Config directory: $configDirectory",
            "Logs directory: $logsDirectory",
            "Current log: $logFile",
            "Legacy data in use: $legacyDataInUse"
        ).joinToString("\n")
}

object DesktopRuntimeDiagnosticsFactory {
    fun create(
        directories: DesktopRuntimeDirectories,
        buildMetadata: DesktopBuildMetadata,
        logFile: Path
    ): DesktopRuntimeDiagnostics =
        create(
            directories = directories,
            buildMetadata = buildMetadata,
            logFile = logFile,
            userHome = Path.of(System.getProperty("user.home", ".")).toAbsolutePath(),
            osName = System.getProperty("os.name", "unknown"),
            osVersion = System.getProperty("os.version", "unknown"),
            architecture = System.getProperty("os.arch", "unknown"),
            javaRuntime = System.getProperty("java.runtime.version", "unknown")
        )

    internal fun create(
        directories: DesktopRuntimeDirectories,
        buildMetadata: DesktopBuildMetadata,
        logFile: Path,
        userHome: Path,
        osName: String,
        osVersion: String,
        architecture: String,
        javaRuntime: String
    ): DesktopRuntimeDiagnostics =
        DesktopRuntimeDiagnostics(
            applicationId = DesktopApplicationIdentity.APPLICATION_ID,
            applicationName = DesktopApplicationIdentity.DISPLAY_NAME,
            version = buildMetadata.applicationVersion,
            buildChannel = buildMetadata.buildChannel,
            buildRevision = buildMetadata.buildRevision,
            buildNumber = buildMetadata.buildNumber,
            operatingSystem = "$osName $osVersion".trim(),
            architecture = architecture,
            javaRuntime = javaRuntime,
            dataDirectory = redact(directories.data, userHome),
            configDirectory = redact(directories.config, userHome),
            logsDirectory = redact(directories.logs, userHome),
            logFile = redact(logFile, userHome),
            legacyDataInUse = directories.legacyDataInUse
        )

    internal fun redact(path: Path, userHome: Path): String {
        val normalizedPath = path.toAbsolutePath().normalize()
        val normalizedHome = userHome.toAbsolutePath().normalize()

        return if (normalizedPath.startsWith(normalizedHome)) {
            val relative = normalizedHome.relativize(normalizedPath).toString()
            if (relative.isBlank()) {
                "<user-home>"
            } else {
                "<user-home>${normalizedPath.fileSystem.separator}$relative"
            }
        } else {
            normalizedPath.toString()
        }
    }
}
