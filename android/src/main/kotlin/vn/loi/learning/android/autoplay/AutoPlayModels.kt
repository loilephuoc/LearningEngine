package vn.loi.learning.android.autoplay

import vn.loi.learning.domain.content.model.ContentId

enum class AutoPlayDirection(val displayName: String, val description: String) {
    VIETNAMESE_TO_ENGLISH("Vietnamese → English", "See and hear Vietnamese first, then reveal English."),
    ENGLISH_TO_VIETNAMESE("English → Vietnamese", "See and hear English first, then reveal Vietnamese.")
}

enum class AutoPlaySource(val displayName: String, val description: String) {
    DUE("Due for Review", "Items currently scheduled and waiting for review"),
    AGAIN_HARD("Again / Hard", "Items recently rated Again or Hard"),
    LEARNED("Learned", "All previously learned vocabulary in order"),
    RANDOM_LEARNED("Random Learned", "Randomized selection of learned vocabulary"),
    RANDOM_ALL("Random All", "Randomized selection across all package vocabulary")
}

data class AutoPlayConfig(
    val direction: AutoPlayDirection = AutoPlayDirection.VIETNAMESE_TO_ENGLISH,
    val source: AutoPlaySource = AutoPlaySource.LEARNED,
    val frontDelayMs: Long = 3000L,
    val playFrontAudio: Boolean = true,
    val playAnswerAudio: Boolean = true,
    val postAnswerDelayMs: Long = 2000L,
    val playExampleEnglishAudio: Boolean = true,
    val postExampleEnglishDelayMs: Long = 2000L,
    val playExampleVietnameseAudio: Boolean = false,
    val postExampleVietnameseDelayMs: Long = 3000L,
    val keepScreenOn: Boolean = true
) {
    init {
        require(frontDelayMs in MIN_FRONT_DELAY_MS..MAX_DELAY_MS) {
            "Front delay must be between 0.1 and 60.0 seconds (was ${frontDelayMs}ms)"
        }
        require(postAnswerDelayMs in 0L..MAX_DELAY_MS) {
            "Post-answer delay must be between 0.0 and 60.0 seconds (was ${postAnswerDelayMs}ms)"
        }
        require(postExampleEnglishDelayMs in 0L..MAX_DELAY_MS) {
            "Post-English-example delay must be between 0.0 and 60.0 seconds (was ${postExampleEnglishDelayMs}ms)"
        }
        require(postExampleVietnameseDelayMs in 0L..MAX_DELAY_MS) {
            "Post-Vietnamese-example delay must be between 0.0 and 60.0 seconds (was ${postExampleVietnameseDelayMs}ms)"
        }
    }

    val frontDelaySeconds: Double
        get() = frontDelayMs / 1000.0

    val postAnswerDelaySeconds: Double
        get() = postAnswerDelayMs / 1000.0

    val postExampleEnglishDelaySeconds: Double
        get() = postExampleEnglishDelayMs / 1000.0

    val postExampleVietnameseDelaySeconds: Double
        get() = postExampleVietnameseDelayMs / 1000.0

    companion object {
        const val MIN_FRONT_DELAY_MS = 100L
        const val MAX_DELAY_MS = 60_000L

        fun normalizeDecimalSeconds(input: String): Double? {
            val sanitized = input.trim().replace(',', '.')
            val parsed = sanitized.toDoubleOrNull() ?: return null
            if (parsed.isNaN() || parsed.isInfinite()) return null
            return parsed
        }

        fun formatSeconds(seconds: Double): String {
            return if (seconds % 1.0 == 0.0) {
                seconds.toLong().toString()
            } else {
                val formatted = "%.2f".format(java.util.Locale.US, seconds).trimEnd('0').trimEnd('.')
                formatted
            }
        }
    }
}

data class AutoPlayItem(
    val contentId: ContentId,
    val headword: String,
    val ipa: String? = null,
    val partOfSpeech: String? = null,
    val vietnameseMeaning: String,
    val englishExample: String? = null,
    val vietnameseExample: String? = null,
    val wordAudioPath: String? = null,
    val meaningAudioPath: String? = null,
    val exampleAudioPath: String? = null,
    val exampleTranslatedAudioPath: String? = null,
    val imagePath: String? = null
) {
    fun frontAudioPath(direction: AutoPlayDirection): String? = when (direction) {
        AutoPlayDirection.VIETNAMESE_TO_ENGLISH -> meaningAudioPath
        AutoPlayDirection.ENGLISH_TO_VIETNAMESE -> wordAudioPath
    }

    fun answerAudioPath(direction: AutoPlayDirection): String? = when (direction) {
        AutoPlayDirection.VIETNAMESE_TO_ENGLISH -> wordAudioPath
        AutoPlayDirection.ENGLISH_TO_VIETNAMESE -> meaningAudioPath
    }
}

enum class AutoPlayStage {
    IDLE,
    FRONT_WAIT,
    REVEAL,
    ANSWER_AUDIO,
    POST_ANSWER_DELAY,
    EXAMPLE_EN_AUDIO,
    POST_EXAMPLE_EN_DELAY,
    EXAMPLE_VI_AUDIO,
    POST_EXAMPLE_VI_DELAY,
    ADVANCE,
    PAUSED,
    COMPLETED,
    STOPPED
}

sealed interface AutoPlayEngineState {
    data class Idle(val config: AutoPlayConfig) : AutoPlayEngineState
    data class Empty(val source: AutoPlaySource, val config: AutoPlayConfig) : AutoPlayEngineState
    data class Running(
        val stage: AutoPlayStage,
        val item: AutoPlayItem,
        val currentIndex: Int,
        val totalCount: Int,
        val config: AutoPlayConfig,
        val isPaused: Boolean = false,
        val stageBeforePause: AutoPlayStage? = null
    ) : AutoPlayEngineState
    data class Completed(
        val totalCount: Int,
        val config: AutoPlayConfig
    ) : AutoPlayEngineState
}
