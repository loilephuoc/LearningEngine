package vn.loi.learning.android.study.design

internal enum class StudyContentDensity { RELAXED, STANDARD, DENSE }
internal enum class StudyMediaRole { HERO, STANDARD, SUPPORTING, COMPACT }
internal enum class StudyMotionRole { PRESS, SELECTION, REVEAL, MEDIA_RESIZE, CARD_EXIT, CARD_ENTER, AUDIO_PULSE, FEEDBACK }
internal enum class StudyFeedbackVisualState { NEUTRAL, SELECTED, CORRECT, INCORRECT, RATING_AGAIN, RATING_HARD, RATING_GOOD, RATING_EASY, AUDIO_ACTIVE }

internal data class StudyDensityInput(
    val viewportHeightDp: Int,
    val viewportWidthDp: Int,
    val hasImage: Boolean,
    val hasExamples: Boolean,
    val contentLength: Int,
    val choiceCount: Int = 0,
    val imeVisible: Boolean = false
)

internal fun resolveStudyContentDensity(input: StudyDensityInput): StudyContentDensity = when {
    input.imeVisible -> StudyContentDensity.DENSE
    input.viewportHeightDp < 620 -> StudyContentDensity.DENSE
    input.hasImage && input.hasExamples && (input.viewportHeightDp < 760 || input.contentLength > 180) -> StudyContentDensity.DENSE
    input.choiceCount >= 5 && input.viewportHeightDp < 800 -> StudyContentDensity.DENSE
    input.viewportHeightDp >= 860 && input.viewportWidthDp >= 360 && input.contentLength <= 100 -> StudyContentDensity.RELAXED
    else -> StudyContentDensity.STANDARD
}

internal data class StudyMediaBounds(val minHeightDp: Int, val maxHeightDp: Int)

internal fun resolveStudyMediaBounds(
    role: StudyMediaRole,
    density: StudyContentDensity,
    availableHeightDp: Int,
    hasMedia: Boolean = true
): StudyMediaBounds? {
    if (!hasMedia) return null
    val roleMax = when (role) {
        StudyMediaRole.HERO -> 420
        StudyMediaRole.STANDARD -> 280
        StudyMediaRole.SUPPORTING -> 200
        StudyMediaRole.COMPACT -> 140
    }
    val densityScale = when (density) {
        StudyContentDensity.RELAXED -> 1f
        StudyContentDensity.STANDARD -> 0.84f
        StudyContentDensity.DENSE -> 0.68f
    }
    val minimum = when (role) {
        StudyMediaRole.HERO -> 160
        StudyMediaRole.STANDARD -> 128
        StudyMediaRole.SUPPORTING -> 112
        StudyMediaRole.COMPACT -> 96
    }
    val viewportCap = (availableHeightDp * 0.42f).toInt().coerceAtLeast(minimum)
    return StudyMediaBounds(minimum, (roleMax * densityScale).toInt().coerceIn(minimum, viewportCap))
}

internal fun studyMotionDurationMillis(role: StudyMotionRole, reducedMotion: Boolean): Int {
    if (reducedMotion) return 0
    return when (role) {
        StudyMotionRole.PRESS, StudyMotionRole.CARD_EXIT -> 120
        StudyMotionRole.SELECTION, StudyMotionRole.CARD_ENTER, StudyMotionRole.REVEAL -> 160
        StudyMotionRole.MEDIA_RESIZE -> 180
        StudyMotionRole.AUDIO_PULSE, StudyMotionRole.FEEDBACK -> 220
    }
}

internal fun studyMediaRole(mode: String, revealed: Boolean): StudyMediaRole = when {
    mode == "Introduction" && !revealed -> StudyMediaRole.HERO
    mode == "ImageRecall" -> StudyMediaRole.HERO
    mode == "Introduction" -> StudyMediaRole.STANDARD
    mode == "Typing" || mode == "MultipleChoice" -> StudyMediaRole.SUPPORTING
    else -> StudyMediaRole.COMPACT
}
