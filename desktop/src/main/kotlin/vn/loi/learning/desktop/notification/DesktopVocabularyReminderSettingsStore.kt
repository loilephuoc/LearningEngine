package vn.loi.learning.desktop.notification

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalTime
import java.util.Properties
import vn.loi.learning.domain.library.model.InstalledPackageId

interface DesktopVocabularyReminderSettingsRepository {
    fun load(): DesktopVocabularyReminderSettings
    fun save(settings: DesktopVocabularyReminderSettings)
}

class DesktopVocabularyReminderSettingsStore(
    private val filePath: Path
) : DesktopVocabularyReminderSettingsRepository {
    override fun load(): DesktopVocabularyReminderSettings {
        if (!Files.isRegularFile(filePath)) return DesktopVocabularyReminderSettings()
        val properties = Properties()
        runCatching {
            Files.newBufferedReader(filePath, StandardCharsets.UTF_8).use(properties::load)
        }.getOrElse { return DesktopVocabularyReminderSettings() }
        val defaults = DesktopVocabularyReminderSettings()
        return DesktopVocabularyReminderSettings(
            enabled = properties.booleanOrDefault(ENABLED, defaults.enabled),
            selectedPackageId = properties.optionalPackageId(INSTALLED_PACKAGE_ID),
            selectionMode = properties.enumOrDefault(SELECTION_MODE, defaults.selectionMode),
            intervalMinutes = properties.intervalOrDefault(INTERVAL_MINUTES, defaults.intervalMinutes),
            activeStart = properties.timeOrDefault(ACTIVE_START, defaults.activeStart),
            activeEnd = properties.timeOrDefault(ACTIVE_END, defaults.activeEnd),
            displayDurationSeconds = properties.displayDurationOrDefault(
                DISPLAY_DURATION_SECONDS,
                defaults.displayDurationSeconds
            ),
            pausedUntil = properties.optionalInstant(PAUSED_UNTIL_EPOCH_MILLIS)
        )
    }

    override fun save(settings: DesktopVocabularyReminderSettings) {
        val target = filePath.toAbsolutePath().normalize()
        val parent = requireNotNull(target.parent) { "Reminder settings path requires a parent." }
        Files.createDirectories(parent)
        val temporary = Files.createTempFile(parent, "desktop-vocabulary-reminder.", ".tmp")
        try {
            Files.writeString(temporary, serialize(settings), StandardCharsets.UTF_8)
            try {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    internal fun serialize(settings: DesktopVocabularyReminderSettings): String = buildString {
        appendLine("enabled=${settings.enabled}")
        appendLine("installed.package.id=${settings.selectedPackageId?.value.orEmpty()}")
        appendLine("selection.mode=${settings.selectionMode.name}")
        appendLine("interval.minutes=${settings.intervalMinutes}")
        appendLine("active.start=${settings.activeStart}")
        appendLine("active.end=${settings.activeEnd}")
        appendLine("display.duration.seconds=${settings.displayDurationSeconds}")
        appendLine("paused.until.epoch.millis=${settings.pausedUntil?.toEpochMilli()?.toString().orEmpty()}")
    }

    companion object {
        const val FILE_NAME = "desktop-vocabulary-reminder.properties"
        private const val ENABLED = "enabled"
        private const val INSTALLED_PACKAGE_ID = "installed.package.id"
        private const val SELECTION_MODE = "selection.mode"
        private const val INTERVAL_MINUTES = "interval.minutes"
        private const val ACTIVE_START = "active.start"
        private const val ACTIVE_END = "active.end"
        private const val DISPLAY_DURATION_SECONDS = "display.duration.seconds"
        private const val PAUSED_UNTIL_EPOCH_MILLIS = "paused.until.epoch.millis"
    }
}

private fun Properties.value(key: String): String? =
    getProperty(key)?.trim()?.takeIf(String::isNotEmpty)

private fun Properties.booleanOrDefault(key: String, default: Boolean): Boolean =
    when (value(key)?.lowercase()) {
        "true" -> true
        "false" -> false
        else -> default
    }

private inline fun <reified T : Enum<T>> Properties.enumOrDefault(key: String, default: T): T =
    value(key)?.let { runCatching { enumValueOf<T>(it.uppercase()) }.getOrNull() } ?: default

private fun Properties.intervalOrDefault(key: String, default: Int): Int =
    value(key)?.toIntOrNull()?.takeIf { it in DesktopVocabularyReminderSettings.ALLOWED_INTERVAL_MINUTES }
        ?: default

private fun Properties.displayDurationOrDefault(key: String, default: Int): Int =
    value(key)?.toIntOrNull()?.takeIf {
        it in DesktopVocabularyReminderSettings.MIN_DISPLAY_DURATION_SECONDS..
            DesktopVocabularyReminderSettings.MAX_DISPLAY_DURATION_SECONDS
    } ?: default

private fun Properties.timeOrDefault(key: String, default: LocalTime): LocalTime =
    value(key)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: default

private fun Properties.optionalInstant(key: String): Instant? =
    value(key)?.toLongOrNull()?.let { runCatching { Instant.ofEpochMilli(it) }.getOrNull() }

private fun Properties.optionalPackageId(key: String): InstalledPackageId? =
    value(key)?.let { runCatching { InstalledPackageId(it) }.getOrNull() }
