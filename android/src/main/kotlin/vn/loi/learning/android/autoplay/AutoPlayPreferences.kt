package vn.loi.learning.android.autoplay

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.loi.learning.android.platform.coordinatedApply

interface AutoPlayPreferenceStore {
    fun load(): AutoPlayConfig
    fun save(config: AutoPlayConfig)
}

class AutoPlayPreferencesController(private val store: AutoPlayPreferenceStore) {
    private val mutableConfig = MutableStateFlow(store.load())
    val config: StateFlow<AutoPlayConfig> = mutableConfig.asStateFlow()

    fun current(): AutoPlayConfig = mutableConfig.value

    fun updateConfig(config: AutoPlayConfig) {
        store.save(config)
        mutableConfig.value = config
    }

    fun updateSelectedPackageId(packageId: String?) {
        updateConfig(mutableConfig.value.copy(selectedPackageId = packageId))
    }

    fun updateDirection(direction: AutoPlayDirection) {
        updateConfig(mutableConfig.value.copy(direction = direction))
    }

    fun updateSource(source: AutoPlaySource) {
        updateConfig(mutableConfig.value.copy(source = source))
    }

    fun updatePlaybackOrder(playbackOrder: AutoPlayPlaybackOrder) {
        updateConfig(mutableConfig.value.copy(playbackOrder = playbackOrder))
    }

    fun updateFrontDelayMs(delayMs: Long) {
        val coerced = delayMs.coerceIn(AutoPlayConfig.MIN_FRONT_DELAY_MS, AutoPlayConfig.MAX_DELAY_MS)
        updateConfig(mutableConfig.value.copy(frontDelayMs = coerced))
    }

    fun updatePlayFrontAudio(enabled: Boolean) {
        updateConfig(mutableConfig.value.copy(playFrontAudio = enabled))
    }

    fun updatePlayAnswerAudio(enabled: Boolean) {
        updateConfig(mutableConfig.value.copy(playAnswerAudio = enabled))
    }

    fun updatePostAnswerDelayMs(delayMs: Long) {
        val coerced = delayMs.coerceIn(0L, AutoPlayConfig.MAX_DELAY_MS)
        updateConfig(mutableConfig.value.copy(postAnswerDelayMs = coerced))
    }

    fun updatePlayExampleEnglishAudio(enabled: Boolean) {
        updateConfig(mutableConfig.value.copy(playExampleEnglishAudio = enabled))
    }

    fun updatePostExampleEnglishDelayMs(delayMs: Long) {
        val coerced = delayMs.coerceIn(0L, AutoPlayConfig.MAX_DELAY_MS)
        updateConfig(mutableConfig.value.copy(postExampleEnglishDelayMs = coerced))
    }

    fun updatePlayExampleVietnameseAudio(enabled: Boolean) {
        updateConfig(mutableConfig.value.copy(playExampleVietnameseAudio = enabled))
    }

    fun updatePostExampleVietnameseDelayMs(delayMs: Long) {
        val coerced = delayMs.coerceIn(0L, AutoPlayConfig.MAX_DELAY_MS)
        updateConfig(mutableConfig.value.copy(postExampleVietnameseDelayMs = coerced))
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        updateConfig(mutableConfig.value.copy(keepScreenOn = enabled))
    }

    fun updateBackgroundPlayback(enabled: Boolean) {
        updateConfig(mutableConfig.value.copy(backgroundPlayback = enabled))
    }

    fun updateMuted(muted: Boolean) {
        updateConfig(mutableConfig.value.copy(isMuted = muted))
    }

    fun updateSleepTimerMinutes(minutes: Double?) {
        val coerced = minutes?.coerceIn(AutoPlayConfig.MIN_SLEEP_TIMER_MINUTES, AutoPlayConfig.MAX_SLEEP_TIMER_MINUTES)
        updateConfig(mutableConfig.value.copy(sleepTimerMinutes = coerced))
    }
}

class SharedPreferencesAutoPlayPreferenceStore(
    private val preferences: SharedPreferences
) : AutoPlayPreferenceStore {
    constructor(context: Context) : this(
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    )

    override fun load(): AutoPlayConfig {
        val selectedPackageId = preferences.getString(KEY_SELECTED_PACKAGE_ID, null)

        val directionStr = preferences.getString(KEY_DIRECTION, AutoPlayDirection.VIETNAMESE_TO_ENGLISH.name)
        val direction = runCatching {
            directionStr?.let { AutoPlayDirection.valueOf(it) }
        }.getOrNull() ?: AutoPlayDirection.VIETNAMESE_TO_ENGLISH

        val sourceStr = preferences.getString(KEY_SOURCE, AutoPlaySource.LEARNED.name)
        val source = runCatching {
            sourceStr?.let { AutoPlaySource.valueOf(it) }
        }.getOrNull() ?: AutoPlaySource.LEARNED

        val playbackOrderStr = preferences.getString(KEY_PLAYBACK_ORDER, AutoPlayPlaybackOrder.SHUFFLED.name)
        val playbackOrder = runCatching {
            playbackOrderStr?.let { AutoPlayPlaybackOrder.valueOf(it) }
        }.getOrNull() ?: AutoPlayPlaybackOrder.SHUFFLED

        val frontDelayMs = when {
            preferences.contains(KEY_FRONT_DELAY_MS) -> preferences.getLong(KEY_FRONT_DELAY_MS, 3000L)
            preferences.contains(KEY_FRONT_DELAY_INT) -> preferences.getInt(KEY_FRONT_DELAY_INT, 3) * 1000L
            else -> 3000L
        }.coerceIn(AutoPlayConfig.MIN_FRONT_DELAY_MS, AutoPlayConfig.MAX_DELAY_MS)

        val playFrontAudio = preferences.getBoolean(KEY_PLAY_FRONT_AUDIO, true)

        val playAnswerAudio = if (preferences.contains(KEY_PLAY_ANSWER_AUDIO)) {
            preferences.getBoolean(KEY_PLAY_ANSWER_AUDIO, true)
        } else {
            preferences.getBoolean(LEGACY_KEY_PLAY_WORD_AUDIO, true)
        }

        val postAnswerDelayMs = when {
            preferences.contains(KEY_POST_ANSWER_DELAY_MS) -> preferences.getLong(KEY_POST_ANSWER_DELAY_MS, 2000L)
            preferences.contains(KEY_POST_ANSWER_DELAY_INT) -> preferences.getInt(KEY_POST_ANSWER_DELAY_INT, 2) * 1000L
            preferences.contains(LEGACY_KEY_POST_WORD_DELAY) -> preferences.getInt(LEGACY_KEY_POST_WORD_DELAY, 2) * 1000L
            else -> 2000L
        }.coerceIn(0L, AutoPlayConfig.MAX_DELAY_MS)

        val playExampleEnglishAudio = if (preferences.contains(KEY_PLAY_EXAMPLE_EN_AUDIO)) {
            preferences.getBoolean(KEY_PLAY_EXAMPLE_EN_AUDIO, true)
        } else {
            preferences.getBoolean(LEGACY_KEY_PLAY_EXAMPLE_AUDIO, true)
        }

        val postExampleEnglishDelayMs = when {
            preferences.contains(KEY_POST_EXAMPLE_EN_DELAY_MS) -> preferences.getLong(KEY_POST_EXAMPLE_EN_DELAY_MS, 2000L)
            preferences.contains(KEY_POST_EXAMPLE_EN_DELAY_INT) -> preferences.getInt(KEY_POST_EXAMPLE_EN_DELAY_INT, 2) * 1000L
            preferences.contains(LEGACY_KEY_POST_EXAMPLE_DELAY) -> preferences.getInt(LEGACY_KEY_POST_EXAMPLE_DELAY, 3) * 1000L
            else -> 2000L
        }.coerceIn(0L, AutoPlayConfig.MAX_DELAY_MS)

        val playExampleVietnameseAudio = preferences.getBoolean(KEY_PLAY_EXAMPLE_VI_AUDIO, false)

        val postExampleVietnameseDelayMs = when {
            preferences.contains(KEY_POST_EXAMPLE_VI_DELAY_MS) -> preferences.getLong(KEY_POST_EXAMPLE_VI_DELAY_MS, 3000L)
            preferences.contains(KEY_POST_EXAMPLE_VI_DELAY_INT) -> preferences.getInt(KEY_POST_EXAMPLE_VI_DELAY_INT, 3) * 1000L
            else -> 3000L
        }.coerceIn(0L, AutoPlayConfig.MAX_DELAY_MS)

        val keepScreenOn = preferences.getBoolean(KEY_KEEP_SCREEN_ON, true)
        val backgroundPlayback = preferences.getBoolean(KEY_BACKGROUND_PLAYBACK, true)
        val isMuted = preferences.getBoolean(KEY_MUTED, false)

        val sleepTimerMinutes = if (preferences.contains(KEY_SLEEP_TIMER_MINUTES)) {
            val v = preferences.getFloat(KEY_SLEEP_TIMER_MINUTES, 0f).toDouble()
            if (v > 0.0) v.coerceIn(AutoPlayConfig.MIN_SLEEP_TIMER_MINUTES, AutoPlayConfig.MAX_SLEEP_TIMER_MINUTES) else null
        } else null

        return AutoPlayConfig(
            selectedPackageId = selectedPackageId,
            direction = direction,
            source = source,
            playbackOrder = playbackOrder,
            frontDelayMs = frontDelayMs,
            playFrontAudio = playFrontAudio,
            playAnswerAudio = playAnswerAudio,
            postAnswerDelayMs = postAnswerDelayMs,
            playExampleEnglishAudio = playExampleEnglishAudio,
            postExampleEnglishDelayMs = postExampleEnglishDelayMs,
            playExampleVietnameseAudio = playExampleVietnameseAudio,
            postExampleVietnameseDelayMs = postExampleVietnameseDelayMs,
            keepScreenOn = keepScreenOn,
            backgroundPlayback = backgroundPlayback,
            isMuted = isMuted,
            sleepTimerMinutes = sleepTimerMinutes
        )
    }

    override fun save(config: AutoPlayConfig) {
        val editor = preferences.edit()
            .putString(KEY_DIRECTION, config.direction.name)
            .putString(KEY_SOURCE, config.source.name)
            .putString(KEY_PLAYBACK_ORDER, config.playbackOrder.name)
            .putLong(KEY_FRONT_DELAY_MS, config.frontDelayMs)
            .putBoolean(KEY_PLAY_FRONT_AUDIO, config.playFrontAudio)
            .putBoolean(KEY_PLAY_ANSWER_AUDIO, config.playAnswerAudio)
            .putLong(KEY_POST_ANSWER_DELAY_MS, config.postAnswerDelayMs)
            .putBoolean(KEY_PLAY_EXAMPLE_EN_AUDIO, config.playExampleEnglishAudio)
            .putLong(KEY_POST_EXAMPLE_EN_DELAY_MS, config.postExampleEnglishDelayMs)
            .putBoolean(KEY_PLAY_EXAMPLE_VI_AUDIO, config.playExampleVietnameseAudio)
            .putLong(KEY_POST_EXAMPLE_VI_DELAY_MS, config.postExampleVietnameseDelayMs)
            .putBoolean(KEY_KEEP_SCREEN_ON, config.keepScreenOn)
            .putBoolean(KEY_BACKGROUND_PLAYBACK, config.backgroundPlayback)
            .putBoolean(KEY_MUTED, config.isMuted)

        if (config.selectedPackageId != null) {
            editor.putString(KEY_SELECTED_PACKAGE_ID, config.selectedPackageId)
        } else {
            editor.remove(KEY_SELECTED_PACKAGE_ID)
        }

        if (config.sleepTimerMinutes != null) {
            editor.putFloat(KEY_SLEEP_TIMER_MINUTES, config.sleepTimerMinutes.toFloat())
        } else {
            editor.remove(KEY_SLEEP_TIMER_MINUTES)
        }

        editor.coordinatedApply()
    }

    private companion object {
        const val FILE_NAME = "learning-engine-autoplay"
        const val KEY_SELECTED_PACKAGE_ID = "autoplay.selected_package_id"
        const val KEY_DIRECTION = "autoplay.direction"
        const val KEY_SOURCE = "autoplay.source"
        const val KEY_PLAYBACK_ORDER = "autoplay.playback_order"
        const val KEY_FRONT_DELAY_MS = "autoplay.front_delay_ms"
        const val KEY_FRONT_DELAY_INT = "autoplay.front_delay"
        const val KEY_PLAY_FRONT_AUDIO = "autoplay.play_front_audio"
        const val KEY_PLAY_ANSWER_AUDIO = "autoplay.play_answer_audio"
        const val KEY_POST_ANSWER_DELAY_MS = "autoplay.post_answer_delay_ms"
        const val KEY_POST_ANSWER_DELAY_INT = "autoplay.post_answer_delay"
        const val KEY_PLAY_EXAMPLE_EN_AUDIO = "autoplay.play_example_en_audio"
        const val KEY_POST_EXAMPLE_EN_DELAY_MS = "autoplay.post_example_en_delay_ms"
        const val KEY_POST_EXAMPLE_EN_DELAY_INT = "autoplay.post_example_en_delay"
        const val KEY_PLAY_EXAMPLE_VI_AUDIO = "autoplay.play_example_vi_audio"
        const val KEY_POST_EXAMPLE_VI_DELAY_MS = "autoplay.post_example_vi_delay_ms"
        const val KEY_POST_EXAMPLE_VI_DELAY_INT = "autoplay.post_example_vi_delay"
        const val KEY_KEEP_SCREEN_ON = "autoplay.keep_screen_on"
        const val KEY_BACKGROUND_PLAYBACK = "autoplay.background_playback"
        const val KEY_MUTED = "autoplay.muted"
        const val KEY_SLEEP_TIMER_MINUTES = "autoplay.sleep_timer_minutes"

        // Legacy keys for backwards-compatibility
        const val LEGACY_KEY_PLAY_WORD_AUDIO = "autoplay.play_word_audio"
        const val LEGACY_KEY_POST_WORD_DELAY = "autoplay.post_word_delay"
        const val LEGACY_KEY_PLAY_EXAMPLE_AUDIO = "autoplay.play_example_audio"
        const val LEGACY_KEY_POST_EXAMPLE_DELAY = "autoplay.post_example_delay"
    }
}
