package vn.loi.learning.desktop.runtime

import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

data class DesktopRuntimeConfiguration(
    val logLevel: DesktopLogLevel = DesktopLogLevel.INFO,
    val retainedLogFiles: Int = DEFAULT_RETAINED_LOG_FILES
) {
    init {
        require(retainedLogFiles in 1..MAX_RETAINED_LOG_FILES) {
            "Retained log files must be between 1 and $MAX_RETAINED_LOG_FILES."
        }
    }

    companion object {
        const val SCHEMA_VERSION: Int = 1
        const val DEFAULT_RETAINED_LOG_FILES: Int = 10
        const val MAX_RETAINED_LOG_FILES: Int = 100
        const val FILE_NAME: String = "runtime.properties"
    }
}

enum class DesktopLogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG
}

class InvalidDesktopConfigurationException(
    val filePath: Path,
    val propertyName: String?,
    cause: Throwable
) : IllegalStateException(
    buildString {
        append("Invalid Desktop runtime configuration: ")
        append(filePath.toAbsolutePath().normalize())
        propertyName?.let { name ->
            append(" (property: ")
            append(name)
            append(')')
        }
    },
    cause
)

object DesktopRuntimeConfigurationLoader {
    fun load(filePath: Path): DesktopRuntimeConfiguration {
        if (Files.notExists(filePath)) {
            return DesktopRuntimeConfiguration()
        }

        val content = Files.readString(filePath, StandardCharsets.UTF_8)
        if (content.isBlank()) {
            throw invalid(
                filePath = filePath,
                propertyName = null,
                cause = IllegalArgumentException("Configuration file is blank.")
            )
        }

        val properties =
            try {
                Properties().apply {
                    load(StringReader(content))
                }
            } catch (failure: IllegalArgumentException) {
                throw invalid(filePath, null, failure)
            }

        val schemaVersion =
            properties.required(filePath, "schema.version").toIntOrNull()
                ?: throw invalid(
                    filePath,
                    "schema.version",
                    IllegalArgumentException("Schema version must be an integer.")
                )

        if (schemaVersion != DesktopRuntimeConfiguration.SCHEMA_VERSION) {
            throw invalid(
                filePath,
                "schema.version",
                IllegalArgumentException(
                    "Unsupported Desktop runtime configuration schema version: $schemaVersion"
                )
            )
        }

        val logLevelValue = properties.required(filePath, "log.level")
        val logLevel =
            try {
                DesktopLogLevel.valueOf(logLevelValue.uppercase())
            } catch (failure: IllegalArgumentException) {
                throw invalid(filePath, "log.level", failure)
            }

        val retainedValue = properties.required(filePath, "log.retained.files")
        val retainedLogFiles =
            retainedValue.toIntOrNull()
                ?: throw invalid(
                    filePath,
                    "log.retained.files",
                    IllegalArgumentException("Log retention must be an integer.")
                )

        return try {
            DesktopRuntimeConfiguration(
                logLevel = logLevel,
                retainedLogFiles = retainedLogFiles
            )
        } catch (failure: IllegalArgumentException) {
            throw invalid(filePath, "log.retained.files", failure)
        }
    }

    private fun Properties.required(filePath: Path, key: String): String =
        getProperty(key)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: throw invalid(
                filePath,
                key,
                IllegalArgumentException("Required configuration property is missing.")
            )

    private fun invalid(
        filePath: Path,
        propertyName: String?,
        cause: Throwable
    ): InvalidDesktopConfigurationException =
        InvalidDesktopConfigurationException(
            filePath = filePath,
            propertyName = propertyName,
            cause = cause
        )
}
