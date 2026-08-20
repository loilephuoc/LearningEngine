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
        val popupLocation = DesktopVocabularyReminderPopupLocation(
            monitorId = properties.value(POPUP_MONITOR_ID),
            normalizedX = properties.normalizedCoordinate(POPUP_POSITION_X_NORMALIZED),
            normalizedY = properties.normalizedCoordinate(POPUP_POSITION_Y_NORMALIZED),
            customPosition = properties.booleanOrDefault(POPUP_POSITION_CUSTOM, defaults.popupLocation.customPosition)
        )
        return DesktopVocabularyReminderSettings(
            enabled = properties.booleanOrDefault(ENABLED, defaults.enabled),
            selectedPackageId = properties.optionalPackageId(INSTALLED_PACKAGE_ID),
            selectionMode = properties.enumOrDefault(SELECTION_MODE, defaults.selectionMode),
            intervalMillis = properties.intervalMillisOrDefault(defaults.intervalMillis),
            activeStart = properties.timeOrDefault(ACTIVE_START, defaults.activeStart),
            activeEnd = properties.timeOrDefault(ACTIVE_END, defaults.activeEnd),
            displayDurationMillis = properties.durationMillisOrLegacyDefault(
                defaults.displayDurationMillis
            ),
            autoPlayPronunciation = properties.booleanOrDefault(
                AUDIO_AUTOPLAY_PRONUNCIATION,
                defaults.autoPlayPronunciation
            ),
            pausedUntil = properties.optionalInstant(PAUSED_UNTIL_EPOCH_MILLIS),
            popupLocation = popupLocation,
            popupLayout = properties.enumOrDefault(POPUP_LAYOUT, defaults.popupLayout),
            playVietnameseAudio = properties.booleanOrDefault(
                AUDIO_VIETNAMESE_ENABLED,
                defaults.playVietnameseAudio
            ),
            vietnameseAudioDelayMillis = properties.vietnameseDelayMillisOrDefault(
                defaults.vietnameseAudioDelayMillis
            ),
            englishTextFontSizeSp = properties.floatOrDefault(
                TEXT_ENGLISH_FONT_SIZE_SP,
                defaults.englishTextFontSizeSp,
                DesktopVocabularyReminderSettings.MIN_ENGLISH_FONT_SIZE_SP..
                    DesktopVocabularyReminderSettings.MAX_ENGLISH_FONT_SIZE_SP
            ),
            showPopupWhileAppForeground = properties.booleanOrDefault(
                POPUP_SHOW_WHILE_APP_FOREGROUND,
                defaults.showPopupWhileAppForeground
            )
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
        appendLine("interval.millis=${settings.intervalMillis}")
        appendLine("active.start=${settings.activeStart}")
        appendLine("active.end=${settings.activeEnd}")
        appendLine("display.duration.millis=${settings.displayDurationMillis}")
        appendLine("audio.autoplay.pronunciation=${settings.autoPlayPronunciation}")
        appendLine("paused.until.epoch.millis=${settings.pausedUntil?.toEpochMilli()?.toString().orEmpty()}")
        appendLine("popup.monitor.id=${settings.popupLocation.monitorId.orEmpty()}")
        appendLine("popup.position.custom=${settings.popupLocation.customPosition}")
        appendLine("popup.position.x.normalized=${settings.popupLocation.normalizedX?.toString().orEmpty()}")
        appendLine("popup.position.y.normalized=${settings.popupLocation.normalizedY?.toString().orEmpty()}")
        appendLine("popup.layout=${settings.popupLayout.name}")
        appendLine("audio.vietnamese.enabled=${settings.playVietnameseAudio}")
        appendLine("audio.vietnamese.delay.millis=${settings.vietnameseAudioDelayMillis}")
        appendLine("text.english.font.size.sp=${settings.englishTextFontSizeSp}")
        appendLine("popup.show.while.app.foreground=${settings.showPopupWhileAppForeground}")
    }

    companion object {
        const val FILE_NAME = "desktop-vocabulary-reminder.properties"
        private const val ENABLED = "enabled"
        private const val INSTALLED_PACKAGE_ID = "installed.package.id"
        private const val SELECTION_MODE = "selection.mode"
        internal const val INTERVAL_MILLIS = "interval.millis"
        internal const val INTERVAL_MINUTES = "interval.minutes"
        private const val ACTIVE_START = "active.start"
        private const val ACTIVE_END = "active.end"
        private const val AUDIO_AUTOPLAY_PRONUNCIATION = "audio.autoplay.pronunciation"
        private const val PAUSED_UNTIL_EPOCH_MILLIS = "paused.until.epoch.millis"
        private const val POPUP_MONITOR_ID = "popup.monitor.id"
        private const val POPUP_POSITION_CUSTOM = "popup.position.custom"
        private const val POPUP_POSITION_X_NORMALIZED = "popup.position.x.normalized"
        private const val POPUP_POSITION_Y_NORMALIZED = "popup.position.y.normalized"
        internal const val POPUP_LAYOUT = "popup.layout"
        internal const val AUDIO_VIETNAMESE_ENABLED = "audio.vietnamese.enabled"
        internal const val AUDIO_VIETNAMESE_DELAY_MILLIS = "audio.vietnamese.delay.millis"
        internal const val TEXT_ENGLISH_FONT_SIZE_SP = "text.english.font.size.sp"
        internal const val POPUP_SHOW_WHILE_APP_FOREGROUND = "popup.show.while.app.foreground"
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

private fun Properties.intervalMillisOrDefault(default: Long): Long {
    val canonical = value(DesktopVocabularyReminderSettingsStore.INTERVAL_MILLIS)?.toLongOrNull()
    val legacy = value(DesktopVocabularyReminderSettingsStore.INTERVAL_MINUTES)?.toLongOrNull()?.times(60_000L)
    return (canonical ?: legacy)?.takeIf {
        it in DesktopVocabularyReminderSettings.MIN_INTERVAL_MILLIS..
            DesktopVocabularyReminderSettings.MAX_INTERVAL_MILLIS
    } ?: default
}

private fun Properties.normalizedCoordinate(key: String): Double? =
    value(key)?.toDoubleOrNull()?.takeIf { it in 0.0..1.0 }

private fun Properties.durationMillisOrLegacyDefault(default: Long): Long {
    val canonical = value("display.duration.millis")?.toLongOrNull()
    val legacy = value("display.duration.seconds")?.toLongOrNull()?.times(1_000L)
    return (canonical ?: legacy)?.takeIf {
        it in DesktopVocabularyReminderSettings.MIN_DISPLAY_DURATION_MILLIS..
            DesktopVocabularyReminderSettings.MAX_DISPLAY_DURATION_MILLIS
    } ?: default
}

private fun Properties.vietnameseDelayMillisOrDefault(default: Long): Long {
    val canonical = value(DesktopVocabularyReminderSettingsStore.AUDIO_VIETNAMESE_DELAY_MILLIS)?.toLongOrNull()
    val legacy = value("audio.vietnamese.delay.seconds")?.toDoubleOrNull()?.times(1_000.0)?.toLong()
    return (canonical ?: legacy)?.takeIf {
        it in DesktopVocabularyReminderSettings.MIN_VIETNAMESE_AUDIO_DELAY_MILLIS..
            DesktopVocabularyReminderSettings.MAX_VIETNAMESE_AUDIO_DELAY_MILLIS
    } ?: default
}

private fun Properties.timeOrDefault(key: String, default: LocalTime): LocalTime =
    value(key)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: default

private fun Properties.optionalInstant(key: String): Instant? =
    value(key)?.toLongOrNull()?.let { runCatching { Instant.ofEpochMilli(it) }.getOrNull() }

private fun Properties.optionalPackageId(key: String): InstalledPackageId? =
    value(key)?.let { runCatching { InstalledPackageId(it) }.getOrNull() }

private fun Properties.floatOrDefault(key: String, default: Float, range: ClosedFloatingPointRange<Float>): Float =
    value(key)?.toFloatOrNull()?.takeIf { it in range } ?: default
