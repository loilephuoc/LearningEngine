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
import vn.loi.learning.application.port.RecoveryOperation
import vn.loi.learning.application.port.RecoveryOperationBusyException
import vn.loi.learning.infrastructure.recovery.PortableBackupSupplementV2
import vn.loi.learning.infrastructure.recovery.PortableBackupV2RestoreConsumer
import vn.loi.learning.infrastructure.recovery.PortableBackupV2SnapshotContributor

internal class AndroidPortableBackupSnapshot(
    private val context: Context,
    private val recordings: QuickVoiceRecordingRepository = QuickVoiceRecordingRepository.getInstance(context)
) : PortableBackupV2SnapshotContributor, PortableBackupV2RestoreConsumer {
    override fun estimatedSnapshotBytes(): Long {
        val recordingRoot = context.filesDir.toPath().resolve("recordings/quick_voice")
        val recordingsBytes = if (Files.isDirectory(recordingRoot)) Files.walk(recordingRoot).use { paths ->
            paths.filter(Files::isRegularFile).mapToLong(Files::size).sum()
        } else 0L
        val background = context.filesDir.toPath().resolve("lockscreen_custom_bg.png")
        val backgroundBytes = if (Files.isRegularFile(background)) Files.size(background) else 0L
        return Math.addExact(Math.addExact(recordingsBytes, backgroundBytes), preferenceSnapshot().toString().toByteArray().size.toLong())
    }

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

    override fun preflight(stagingDirectory: Path) {
        if (recordings.isRecordingActive) {
            throw RecoveryOperationBusyException(RecoveryOperation.RESTORE)
        }
        val preferencesPath = stagingDirectory.resolve(PREFERENCES_PATH)
        if (Files.isRegularFile(preferencesPath)) {
            val text = Files.newBufferedReader(preferencesPath, StandardCharsets.UTF_8).use { it.readText() }
            val json = jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject
                ?: error("Invalid preferences.json payload.")
            val version = (json["schemaVersion"] as? JsonPrimitive)?.content?.toIntOrNull()
            if (version != 1) error("Unsupported preferences.json schemaVersion: $version")
            if (json["stores"] !is kotlinx.serialization.json.JsonObject) error("Missing stores in preferences.json.")
        }
        val recordingIndexPath = stagingDirectory.resolve(RECORDING_INDEX_PATH)
        if (Files.isRegularFile(recordingIndexPath)) {
            val text = Files.newBufferedReader(recordingIndexPath, StandardCharsets.UTF_8).use { it.readText() }
            val json = jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject
                ?: error("Invalid recordings index.json payload.")
            val version = (json["schemaVersion"] as? JsonPrimitive)?.content?.toIntOrNull()
            if (version != 1) error("Unsupported recordings index schemaVersion: $version")
        }
    }

    override fun captureCurrentState(): Any {
        val prefs = DURABLE_KEYS.keys.associateWith {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).all
        }
        val rollbackDir = try {
            Files.createTempDirectory(context.cacheDir.toPath(), ".platform-rollback-")
        } catch (_: Exception) { null }
        val recordingItems = recordings.captureRecordingsStateToDirectory(rollbackDir)
        val bgPath = context.filesDir.toPath().resolve("lockscreen_custom_bg.png")
        val hasBg = Files.isRegularFile(bgPath)
        if (hasBg && rollbackDir != null) {
            Files.copy(bgPath, rollbackDir.resolve("lockscreen_custom_bg.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
        return AndroidPlatformRestoreState(prefs, recordingItems, rollbackDir, hasBg)
    }

    override fun applyRestored(stagingDirectory: Path) {
        AndroidPreferenceSnapshotGate.mutation {
            val preferencesPath = stagingDirectory.resolve(PREFERENCES_PATH)
            if (Files.isRegularFile(preferencesPath)) {
                val text = Files.newBufferedReader(preferencesPath, StandardCharsets.UTF_8).use { it.readText() }
                val json = jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject
                val storesObj = json?.get("stores") as? kotlinx.serialization.json.JsonObject
                if (storesObj != null) {
                    DURABLE_KEYS.forEach { (storeName, allowlist) ->
                        val sp = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
                        val editor = sp.edit()
                        allowlist.forEach { editor.remove(it) }
                        TRANSIENT_KEYS[storeName]?.forEach { editor.remove(it) }
                        val storeJson = storesObj[storeName] as? kotlinx.serialization.json.JsonObject
                        storeJson?.forEach { (k, v) ->
                            if (k in allowlist) {
                                putRestoredPreferenceValue(editor, k, v)
                            }
                        }
                        editor.commit()
                    }
                }
            }
            val stagedBg = stagingDirectory.resolve(BACKGROUND_PATH)
            val liveBg = context.filesDir.toPath().resolve("lockscreen_custom_bg.png")
            if (Files.isRegularFile(stagedBg)) {
                Files.copy(stagedBg, liveBg, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            } else {
                Files.deleteIfExists(liveBg)
            }
        }
        val recordingIndex = stagingDirectory.resolve(RECORDING_INDEX_PATH)
        if (Files.isRegularFile(recordingIndex)) {
            recordings.restoreRecordingsFromBackup(stagingDirectory.resolve("android/recordings"))
        }
    }

    override fun rollback(capturedState: Any?) {
        val state = capturedState as? AndroidPlatformRestoreState ?: return
        try {
            AndroidPreferenceSnapshotGate.mutation {
                DURABLE_KEYS.keys.forEach { storeName ->
                    val sp = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
                    val editor = sp.edit()
                    DURABLE_KEYS[storeName]?.forEach { editor.remove(it) }
                    TRANSIENT_KEYS[storeName]?.forEach { editor.remove(it) }
                    val capturedMap = state.preferences[storeName].orEmpty()
                    capturedMap.forEach { (k, v) ->
                        if (k in DURABLE_KEYS[storeName].orEmpty()) {
                            putCapturedPreferenceValue(editor, k, v)
                        }
                    }
                    editor.commit()
                }
                val liveBg = context.filesDir.toPath().resolve("lockscreen_custom_bg.png")
                val stagedBg = state.rollbackDir?.resolve("lockscreen_custom_bg.png")
                if (state.hasLockscreenBackground && stagedBg != null && Files.isRegularFile(stagedBg)) {
                    Files.copy(stagedBg, liveBg, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                } else {
                    Files.deleteIfExists(liveBg)
                }
            }
            recordings.rollbackRecordingsFromDirectory(state.recordings, state.rollbackDir)
        } finally {
            state.rollbackDir?.let { deleteTree(it) }
        }
    }

    override fun validateLive() {
        DURABLE_KEYS.keys.forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).all
        }
    }

    override fun commit(capturedState: Any?) {
        (capturedState as? AndroidPlatformRestoreState)?.rollbackDir?.let { runCatching { deleteTree(it) } }
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

        private val TRANSIENT_KEYS = mapOf(
            "learning_engine_reminder_prefs" to setOf(
                "reminder.paused_until_epoch_millis",
                "reminder.unlocked_paused_until_epoch_millis",
                "home_widget.current_candidate_id",
                "lockscreen.shuffle_state",
                "lockscreen.candidate_index"
            ),
            "learning-engine-autoplay" to setOf(
                "autoplay.sleep_timer_remaining_ms",
                "autoplay.active_playback_id"
            )
        )

        private val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        private fun putRestoredPreferenceValue(editor: android.content.SharedPreferences.Editor, key: String, element: JsonElement) {
            when {
                element is kotlinx.serialization.json.JsonArray -> {
                    val set = element.map { (it as? JsonPrimitive)?.content ?: it.toString() }.toSet()
                    editor.putStringSet(key, set)
                }
                element is JsonPrimitive -> {
                    if (element.isString) {
                        editor.putString(key, element.content)
                    } else {
                        val booleanVal = element.content.toBooleanStrictOrNull()
                        if (booleanVal != null) {
                            editor.putBoolean(key, booleanVal)
                        } else {
                            when (key) {
                                "daily.new", "daily.review", "reminder.interval_minutes" -> {
                                    editor.putInt(key, element.content.toIntOrNull() ?: 0)
                                }
                                "lockscreen.word_size", "lockscreen.vietnamese_size", "lockscreen.image_size",
                                "lockscreen.card_background_opacity", "home_widget.word_size",
                                "home_widget.vietnamese_size", "home_widget.image_size", "home_widget.card_background_opacity" -> {
                                    editor.putFloat(key, element.content.toFloatOrNull() ?: 1.0f)
                                }
                                else -> {
                                    val longVal = element.content.toLongOrNull()
                                    if (longVal != null) {
                                        editor.putLong(key, longVal)
                                    } else {
                                        val floatVal = element.content.toFloatOrNull()
                                        if (floatVal != null) {
                                            editor.putFloat(key, floatVal)
                                        } else {
                                            editor.putString(key, element.content)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        }

        private fun putCapturedPreferenceValue(editor: android.content.SharedPreferences.Editor, key: String, value: Any?) {
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
    }
}

private data class AndroidPlatformRestoreState(
    val preferences: Map<String, Map<String, *>>,
    val recordings: List<vn.loi.learning.android.recording.VoiceRecordingItem>,
    val rollbackDir: Path?,
    val hasLockscreenBackground: Boolean
)

private fun deleteTree(root: Path) {
    root.toFile().deleteRecursively()
}
