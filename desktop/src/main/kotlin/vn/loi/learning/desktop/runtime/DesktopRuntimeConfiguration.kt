package vn.loi.learning.desktop.runtime

import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption
import java.util.Properties

data class DesktopRuntimeConfiguration(
    val logLevel: DesktopLogLevel = DesktopLogLevel.INFO,
    val retainedLogFiles: Int = DEFAULT_RETAINED_LOG_FILES,
    val theme: DesktopThemePreference = DesktopThemePreference.SYSTEM,
    val locale: DesktopLocale = DesktopLocale.ENGLISH,
    val audioLoopDelaySeconds: Double = DEFAULT_AUDIO_LOOP_DELAY_SECONDS,
    val newItemsPerSession: Int = DEFAULT_NEW_ITEMS_PER_SESSION,
    val reviewItemsPerSession: Int = DEFAULT_REVIEW_ITEMS_PER_SESSION,
    val studyTypography: StudyTypographyPreferences = StudyTypographyPreferences()
) {
    init {
        require(retainedLogFiles in 1..MAX_RETAINED_LOG_FILES) {
            "Retained log files must be between 1 and $MAX_RETAINED_LOG_FILES."
        }
        require(audioLoopDelaySeconds in MIN_AUDIO_LOOP_DELAY_SECONDS..MAX_AUDIO_LOOP_DELAY_SECONDS) {
            "Audio loop delay must be between $MIN_AUDIO_LOOP_DELAY_SECONDS and $MAX_AUDIO_LOOP_DELAY_SECONDS seconds."
        }
        require(newItemsPerSession in MIN_NEW_ITEMS_PER_SESSION..MAX_NEW_ITEMS_PER_SESSION) {
            "newItemsPerSession must be between $MIN_NEW_ITEMS_PER_SESSION and $MAX_NEW_ITEMS_PER_SESSION."
        }
        require(reviewItemsPerSession in MIN_REVIEW_ITEMS_PER_SESSION..MAX_REVIEW_ITEMS_PER_SESSION) {
            "reviewItemsPerSession must be between $MIN_REVIEW_ITEMS_PER_SESSION and $MAX_REVIEW_ITEMS_PER_SESSION."
        }
        require(newItemsPerSession > 0 || reviewItemsPerSession > 0) {
            "At least one session item limit must be greater than zero."
        }
    }

    companion object {
        const val SCHEMA_VERSION: Int = 1
        const val DEFAULT_RETAINED_LOG_FILES: Int = 10
        const val MAX_RETAINED_LOG_FILES: Int = 100
        const val DEFAULT_AUDIO_LOOP_DELAY_SECONDS: Double = 0.35
        const val MIN_AUDIO_LOOP_DELAY_SECONDS: Double = 0.0
        const val MAX_AUDIO_LOOP_DELAY_SECONDS: Double = 10.0
        const val DEFAULT_NEW_ITEMS_PER_SESSION = 20
        const val DEFAULT_REVIEW_ITEMS_PER_SESSION = 100
        const val MIN_NEW_ITEMS_PER_SESSION = 0
        const val MAX_NEW_ITEMS_PER_SESSION = 100
        const val MIN_REVIEW_ITEMS_PER_SESSION = 0
        const val MAX_REVIEW_ITEMS_PER_SESSION = 500
        const val FILE_NAME: String = "runtime.properties"
    }
}

data class StudyTypographyPreferences(
    val exampleEnglishFontSize: Int = DEFAULT_EXAMPLE_ENGLISH_FONT_SIZE,
    val exampleVietnameseFontSize: Int = DEFAULT_EXAMPLE_VIETNAMESE_FONT_SIZE
) {
    init {
        require(exampleEnglishFontSize in MIN_EXAMPLE_ENGLISH_FONT_SIZE..MAX_EXAMPLE_ENGLISH_FONT_SIZE) {
            "exampleEnglishFontSize must be between $MIN_EXAMPLE_ENGLISH_FONT_SIZE and $MAX_EXAMPLE_ENGLISH_FONT_SIZE."
        }
        require(exampleVietnameseFontSize in MIN_EXAMPLE_VIETNAMESE_FONT_SIZE..MAX_EXAMPLE_VIETNAMESE_FONT_SIZE) {
            "exampleVietnameseFontSize must be between $MIN_EXAMPLE_VIETNAMESE_FONT_SIZE and $MAX_EXAMPLE_VIETNAMESE_FONT_SIZE."
        }
    }

    companion object {
        const val DEFAULT_EXAMPLE_ENGLISH_FONT_SIZE = 20
        const val MIN_EXAMPLE_ENGLISH_FONT_SIZE = 16
        const val MAX_EXAMPLE_ENGLISH_FONT_SIZE = 30
        const val DEFAULT_EXAMPLE_VIETNAMESE_FONT_SIZE = 16
        const val MIN_EXAMPLE_VIETNAMESE_FONT_SIZE = 14
        const val MAX_EXAMPLE_VIETNAMESE_FONT_SIZE = 26
    }
}

enum class DesktopLogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG
}

enum class DesktopThemePreference {
    LIGHT,
    DARK,
    SYSTEM
}

enum class DesktopLocale {
    ENGLISH,
    VIETNAMESE
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

        val theme =
            properties.getProperty("theme")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { value ->
                    try {
                        DesktopThemePreference.valueOf(value.uppercase())
                    } catch (failure: IllegalArgumentException) {
                        throw invalid(filePath, "theme", failure)
                    }
                }
                ?: DesktopThemePreference.SYSTEM

        val locale =
            properties.getProperty("locale")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { value ->
                    try {
                        DesktopLocale.valueOf(value.uppercase())
                    } catch (failure: IllegalArgumentException) {
                        throw invalid(filePath, "locale", failure)
                    }
                }
                ?: DesktopLocale.ENGLISH

        val audioLoopDelaySeconds =
            properties.getProperty("audio.loop.delay.seconds")
                ?.trim()
                ?.toDoubleOrNull()
                ?.coerceIn(
                    DesktopRuntimeConfiguration.MIN_AUDIO_LOOP_DELAY_SECONDS,
                    DesktopRuntimeConfiguration.MAX_AUDIO_LOOP_DELAY_SECONDS
                )
                ?: DesktopRuntimeConfiguration.DEFAULT_AUDIO_LOOP_DELAY_SECONDS

        val newItemsPerSession = properties.optionalInt(
            filePath, "study.new.items.per.session",
            DesktopRuntimeConfiguration.DEFAULT_NEW_ITEMS_PER_SESSION
        )
        val reviewItemsPerSession = properties.optionalInt(
            filePath, "study.review.items.per.session",
            DesktopRuntimeConfiguration.DEFAULT_REVIEW_ITEMS_PER_SESSION
        )
        val exampleEnglishFontSize = properties.optionalInt(
            filePath,
            "study.typography.example.english.font.size",
            StudyTypographyPreferences.DEFAULT_EXAMPLE_ENGLISH_FONT_SIZE
        )
        val exampleVietnameseFontSize = properties.optionalInt(
            filePath,
            "study.typography.example.vietnamese.font.size",
            StudyTypographyPreferences.DEFAULT_EXAMPLE_VIETNAMESE_FONT_SIZE
        )

        return try {
            DesktopRuntimeConfiguration(
                logLevel = logLevel,
                retainedLogFiles = retainedLogFiles,
                theme = theme,
                locale = locale,
                audioLoopDelaySeconds = audioLoopDelaySeconds,
                newItemsPerSession = newItemsPerSession,
                reviewItemsPerSession = reviewItemsPerSession,
                studyTypography = StudyTypographyPreferences(
                    exampleEnglishFontSize = exampleEnglishFontSize,
                    exampleVietnameseFontSize = exampleVietnameseFontSize
                )
            )
        } catch (failure: IllegalArgumentException) {
            val property = when {
                failure.message?.contains("Retained log files") == true -> "log.retained.files"
                failure.message?.contains("newItemsPerSession") == true -> "study.new.items.per.session"
                failure.message?.contains("reviewItemsPerSession") == true -> "study.review.items.per.session"
                failure.message?.contains("session item limit") == true -> "study.new.items.per.session"
                failure.message?.contains("exampleEnglishFontSize") == true ->
                    "study.typography.example.english.font.size"
                failure.message?.contains("exampleVietnameseFontSize") == true ->
                    "study.typography.example.vietnamese.font.size"
                else -> "audio.loop.delay.seconds"
            }
            throw invalid(filePath, property, failure)
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

    private fun Properties.optionalInt(filePath: Path, key: String, defaultValue: Int): Int {
        val value = getProperty(key)?.trim()?.takeIf(String::isNotEmpty) ?: return defaultValue
        return value.toIntOrNull()
            ?: throw invalid(filePath, key, IllegalArgumentException("$key must be an integer."))
    }

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

object DesktopRuntimeConfigurationStore {
    fun save(filePath: Path, configuration: DesktopRuntimeConfiguration) {
        val target = filePath.toAbsolutePath().normalize()
        val parent = requireNotNull(target.parent) {
            "Desktop runtime configuration path requires a parent."
        }
        Files.createDirectories(parent)
        val temporary = Files.createTempFile(parent, "runtime-config.", ".tmp")

        try {
            Files.writeString(
                temporary,
                buildString {
                    appendLine("schema.version=${DesktopRuntimeConfiguration.SCHEMA_VERSION}")
                    appendLine("log.level=${configuration.logLevel.name.lowercase()}")
                    appendLine("log.retained.files=${configuration.retainedLogFiles}")
                    appendLine("theme=${configuration.theme.name.lowercase()}")
                    appendLine("locale=${configuration.locale.name.lowercase()}")
                    appendLine("audio.loop.delay.seconds=${configuration.audioLoopDelaySeconds}")
                    appendLine("study.new.items.per.session=${configuration.newItemsPerSession}")
                    appendLine("study.review.items.per.session=${configuration.reviewItemsPerSession}")
                    appendLine(
                        "study.typography.example.english.font.size=" +
                            configuration.studyTypography.exampleEnglishFontSize
                    )
                    appendLine(
                        "study.typography.example.vietnamese.font.size=" +
                            configuration.studyTypography.exampleVietnameseFontSize
                    )
                },
                StandardCharsets.UTF_8
            )

            try {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (unsupported: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
