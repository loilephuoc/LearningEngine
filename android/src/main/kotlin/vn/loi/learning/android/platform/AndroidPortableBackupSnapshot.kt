package vn.loi.learning.android.platform

import android.content.Context
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vn.loi.learning.android.recording.QuickVoiceRecordingRepository
import vn.loi.learning.infrastructure.recovery.PortableBackupSupplementV2
import vn.loi.learning.infrastructure.recovery.PortableBackupV2SnapshotContributor

internal class AndroidPortableBackupSnapshot(
    private val context: Context,
    private val recordings: QuickVoiceRecordingRepository = QuickVoiceRecordingRepository.getInstance(context)
) : PortableBackupV2SnapshotContributor {
    override fun snapshot(stagingDirectory: Path): List<PortableBackupSupplementV2> {
        val output = mutableListOf<PortableBackupSupplementV2>()
        val preferencesPath = stagingDirectory.resolve(PREFERENCES_PATH)
        Files.createDirectories(requireNotNull(preferencesPath.parent))
        Files.write(preferencesPath, preferenceSnapshot().toString().toByteArray(StandardCharsets.UTF_8))
        output += PortableBackupSupplementV2(PREFERENCES_PATH, "android", "preferences", preferencesPath)

        val recordingFiles = stagingDirectory.resolve("android/recordings/files")
        val completed = recordings.snapshotCompletedForBackup(recordingFiles)
        val recordingIndex = stagingDirectory.resolve(RECORDING_INDEX_PATH)
        Files.createDirectories(requireNotNull(recordingIndex.parent))
        val portableIndex = encodeRecordingIndex(completed)
        Files.write(recordingIndex, portableIndex.toString().toByteArray(StandardCharsets.UTF_8))
        output += PortableBackupSupplementV2(RECORDING_INDEX_PATH, "android", "recording-index", recordingIndex)
        completed.forEach { item ->
            val logical = "android/recordings/files/${item.filename}"
            output += PortableBackupSupplementV2(logical, "android", "recording", stagingDirectory.resolve(logical))
        }

        val background = context.filesDir.toPath().resolve("lockscreen_custom_bg.png")
        if (Files.isRegularFile(background)) {
            val staged = stagingDirectory.resolve(BACKGROUND_PATH)
            Files.createDirectories(requireNotNull(staged.parent))
            Files.copy(background, staged)
            output += PortableBackupSupplementV2(BACKGROUND_PATH, "android", "lockscreen-background", staged)
        }
        return output
    }

    private fun preferenceSnapshot(): JsonElement = AndroidPreferenceSnapshotGate.snapshot {
        encodePreferences(
            DURABLE_KEYS.keys.associateWith { context.getSharedPreferences(it, Context.MODE_PRIVATE).all },
            Files.isRegularFile(context.filesDir.toPath().resolve("lockscreen_custom_bg.png"))
        )
    }

    companion object {
        const val PREFERENCES_PATH = "android/preferences.json"
        const val RECORDING_INDEX_PATH = "android/recordings/index.json"
        const val BACKGROUND_PATH = "android/lockscreen/custom-background.png"

        internal fun encodeRecordingIndex(items: List<vn.loi.learning.android.recording.VoiceRecordingItem>) = buildJsonObject {
            put("schemaVersion", 1)
            put("recordings", buildJsonArray {
                items.sortedBy { it.id }.forEach { item -> add(buildJsonObject {
                    put("id", item.id); put("filename", item.filename); put("recordingFile", "files/${item.filename}")
                    put("createdAt", item.createdAt); put("durationMs", item.durationMs); put("sizeBytes", item.sizeBytes)
                }) }
            })
        }

        internal fun encodePreferences(stores: Map<String, Map<String, *>>, hasBackground: Boolean) = buildJsonObject {
        put("schemaVersion", 1)
        put("stores", buildJsonObject {
            DURABLE_KEYS.toSortedMap().forEach { (storeName, allowlist) ->
                val values = stores[storeName].orEmpty()
                put(storeName, buildJsonObject {
                    values.toSortedMap().forEach { (key, value) ->
                        if (key in allowlist) put(key, value.toPortableJson())
                    }
                })
            }
        })
        put("lockScreenCustomBackground", if (hasBackground) JsonPrimitive(BACKGROUND_PATH) else JsonNull)
    }

        private fun Any?.toPortableJson(): JsonElement = when (this) {
        is Boolean -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is Long -> JsonPrimitive(this)
        is Float -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Set<*> -> buildJsonArray { this@toPortableJson.filterIsInstance<String>().sorted().forEach { add(JsonPrimitive(it)) } }
        else -> error("Unsupported durable preference value type: ${this?.javaClass?.name}")
    }

        private val DURABLE_KEYS = mapOf(
            "learning-engine-presentation" to setOf("theme.mode"),
            "learning-engine-study" to setOf("daily.new", "daily.review", "typing.vi-autoplay-muted", "study.continuous-skim", "dashboard.insights.package_id"),
            "learning-engine-controller" to setOf("config_json"),
            "learning_engine_audio_prefs" to setOf("app.audio.muted"),
            "learning-engine-autoplay" to setOf(
                "autoplay.selected_package_id", "autoplay.direction", "autoplay.source", "autoplay.playback_order",
                "autoplay.front_delay_ms", "autoplay.front_delay", "autoplay.play_front_audio", "autoplay.play_answer_audio",
                "autoplay.post_answer_delay_ms", "autoplay.post_answer_delay", "autoplay.play_example_en_audio",
                "autoplay.post_example_en_delay_ms", "autoplay.post_example_en_delay", "autoplay.play_example_vi_audio",
                "autoplay.post_example_vi_delay_ms", "autoplay.post_example_vi_delay", "autoplay.keep_screen_on",
                "autoplay.background_playback", "autoplay.muted"
            ),
            "learning_engine_reminder_difficult_prefs" to setOf("difficult.content.ids"),
            "learning_engine_reminder_prefs" to setOf(
                "reminder.enabled", "reminder.selected_package_id", "reminder.selection_mode", "reminder.interval_millis",
                "reminder.interval_minutes", "reminder.active_start", "reminder.active_end", "reminder.display_duration_millis",
                "reminder.autoplay_pronunciation", "reminder.overlay_popup_enabled", "reminder.quick_pause_actions_enabled",
                "lockscreen.enabled", "lockscreen.selected_package_id", "lockscreen.selection_mode", "lockscreen.autoplay_pronunciation",
                "lockscreen.word_size", "lockscreen.vietnamese_size", "lockscreen.image_size", "lockscreen.card_background_opacity",
                "lockscreen.quick_review_interval_ms", "lockscreen.screen_off_prep_enabled", "lockscreen.screen_off_prepare_delay_ms",
                "home_widget.autonext_enabled", "home_widget.interval_ms", "home_widget.selected_package_id",
                "home_widget.selection_mode", "home_widget.word_size", "home_widget.vietnamese_size", "home_widget.image_size",
                "home_widget.card_background_opacity", "home_widget.update_only_screen_on", "home_widget.autoaudio_enabled"
            )
        )
    }
}
