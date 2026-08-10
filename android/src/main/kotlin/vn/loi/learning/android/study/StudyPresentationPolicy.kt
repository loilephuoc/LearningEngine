package vn.loi.learning.android.study

/** Presentation-only sizing and interaction policy for the Android Study canvas. */
internal data class IntroductionImageBounds(
    val frontMaxHeightDp: Int,
    val revealMaxHeightDp: Int
)

internal fun resolveIntroductionImageBounds(availableViewportHeightDp: Int): IntroductionImageBounds =
    IntroductionImageBounds(
        frontMaxHeightDp = (availableViewportHeightDp * 0.54f).toInt().coerceIn(210, 460),
        revealMaxHeightDp = (availableViewportHeightDp * 0.36f).toInt().coerceIn(170, 340)
    )

internal fun introductionClueTextSizeSp(length: Int): Int = when {
    length <= 42 -> 32
    length <= 84 -> 28
    else -> 24
}

internal fun introductionPartOfSpeechLabel(partOfSpeech: String?): String? = partOfSpeech
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?.replace('_', ' ')
    ?.lowercase()
    ?.let { "($it)" }

internal fun introductionMetadataLine(partOfSpeech: String?, pronunciation: String?): String? {
    val pos = introductionPartOfSpeechLabel(partOfSpeech)
    val spoken = pronunciation?.trim()?.takeIf(String::isNotEmpty)?.let {
        if (it.startsWith('/') && it.endsWith('/')) it else "/$it/"
    }
    return listOfNotNull(pos, spoken).takeIf(List<String>::isNotEmpty)?.joinToString("  ")
}

internal enum class IntroductionPlaybackFocus { WORD, EXAMPLE }

internal fun nextIntroductionPlaybackFocus(
    current: IntroductionPlaybackFocus,
    hasWordAudio: Boolean,
    hasExampleAudio: Boolean
): IntroductionPlaybackFocus? = when {
    hasExampleAudio && current == IntroductionPlaybackFocus.WORD -> IntroductionPlaybackFocus.EXAMPLE
    hasWordAudio -> IntroductionPlaybackFocus.WORD
    hasExampleAudio -> IntroductionPlaybackFocus.EXAMPLE
    else -> null
}

internal enum class IntroductionStageGesture { NONE, TAP, SWIPE_GOOD }

internal fun resolveIntroductionStageGesture(
    deltaX: Float,
    deltaY: Float,
    swipeThresholdPx: Float,
    tapSlopPx: Float,
    scrollRequired: Boolean,
    childConsumed: Boolean,
    alreadySubmitted: Boolean,
    ratingEnabled: Boolean = true
): IntroductionStageGesture {
    if (childConsumed || alreadySubmitted) return IntroductionStageGesture.NONE
    val absX = kotlin.math.abs(deltaX)
    val absY = kotlin.math.abs(deltaY)
    if (absX <= tapSlopPx && absY <= tapSlopPx) return IntroductionStageGesture.TAP
    return if (ratingEnabled && !scrollRequired && deltaY <= -swipeThresholdPx && absX <= absY * 0.55f) {
        IntroductionStageGesture.SWIPE_GOOD
    } else {
        IntroductionStageGesture.NONE
    }
}

internal fun progressDescription(label: String, completed: Int, target: Int, configuredTarget: Int): String =
    if (target == configuredTarget) "$label $completed of $target"
    else "$label $completed of $target available, configured target $configuredTarget"
