package vn.loi.learning.android.study

import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.FocusedPracticeKind

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

internal data class PartOfSpeechPresentation(
    val canonicalLabel: String,
    val paletteIndex: Int
)

private val partOfSpeechAliases = mapOf(
    "n" to "NOUN", "noun" to "NOUN",
    "v" to "VERB", "verb" to "VERB",
    "adj" to "ADJECTIVE", "adjective" to "ADJECTIVE",
    "adv" to "ADVERB", "adverb" to "ADVERB",
    "phrasal v" to "PHRASAL VERB", "phrasal verb" to "PHRASAL VERB", "verb phrase" to "PHRASAL VERB",
    "prep" to "PREPOSITION", "preposition" to "PREPOSITION",
    "pron" to "PRONOUN", "pronoun" to "PRONOUN",
    "conj" to "CONJUNCTION", "conjunction" to "CONJUNCTION",
    "interj" to "INTERJECTION", "interjection" to "INTERJECTION",
    "det" to "DETERMINER", "determiner" to "DETERMINER",
    "art" to "ARTICLE", "article" to "ARTICLE",
    "aux" to "AUXILIARY", "auxiliary" to "AUXILIARY",
    "modal" to "MODAL"
)

private val explicitPartOfSpeechPalette = mapOf(
    "NOUN" to 0,
    "VERB" to 1,
    "ADJECTIVE" to 2,
    "ADVERB" to 3,
    "PHRASAL VERB" to 4,
    "PREPOSITION" to 5,
    "PRONOUN" to 6,
    "CONJUNCTION" to 7,
    "INTERJECTION" to 8
)

internal fun canonicalPartOfSpeech(partOfSpeech: String?): String? {
    val normalized = partOfSpeech
        ?.trim()
        ?.removeSurrounding("(", ")")
        ?.replace('_', ' ')
        ?.replace(Regex("\\s+"), " ")
        ?.lowercase()
        ?.takeIf(String::isNotBlank)
        ?: return null
    return partOfSpeechAliases[normalized] ?: normalized.uppercase()
}

internal fun partOfSpeechPresentation(partOfSpeech: String?): PartOfSpeechPresentation? {
    val canonical = canonicalPartOfSpeech(partOfSpeech) ?: return null
    val paletteSize = 9
    val stableFallback = (canonical.hashCode().toLong() and 0x7fffffffL).rem(paletteSize).toInt()
    return PartOfSpeechPresentation(canonical, explicitPartOfSpeechPalette[canonical] ?: stableFallback)
}

internal fun normalizedIntroductionPronunciation(partOfSpeech: String?, pronunciation: String?): String? {
    var value = pronunciation?.trim()?.takeIf(String::isNotBlank) ?: return null
    val legacyPosDescriptor = Regex(
        "^/*\\s*\\(\\s*(?:(?:noun|verb|adjective|adverb|pronoun|preposition|conjunction|interjection|article|determiner|phrasal\\s+verb|proper\\s+noun)\\s*(?:[/,&+]\\s*)?)+\\)\\s*",
        RegexOption.IGNORE_CASE
    )
    value = value.replace(legacyPosDescriptor, "")
    val canonicalPos = canonicalPartOfSpeech(partOfSpeech)
    if (canonicalPos != null) {
        val aliases = (partOfSpeechAliases.filterValues { it == canonicalPos }.keys + canonicalPos.lowercase())
            .sortedByDescending(String::length)
            .joinToString("|") { Regex.escape(it) }
        value = value.replace(Regex("^/*\\s*\\(\\s*(?:$aliases)\\s*\\)\\s*", RegexOption.IGNORE_CASE), "")
        value = value.replace(Regex("^(?:$aliases)\\s+(?=/)", RegexOption.IGNORE_CASE), "")
    }
    value = value.trim().trim('/').trim()
    if (value.isBlank()) return null
    return "/$value/"
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

internal enum class IntroductionStageGesture { NONE, TAP, SWIPE_GOOD, PREVIOUS, NEXT }

internal fun usesFocusedSkimUx(kind: FocusedPracticeKind): Boolean =
    kind == FocusedPracticeKind.DIFFICULT || kind == FocusedPracticeKind.QUICK_REVIEW

internal fun focusedPracticeHeadwordGlowActive(
    focusedSkimUx: Boolean,
    revealed: Boolean,
    transitionPending: Boolean,
    historyPreview: Boolean
): Boolean = focusedSkimUx && revealed && transitionPending && !historyPreview

internal fun introductionRatingInputEnabled(
    revealed: Boolean,
    historyPreview: Boolean,
    interactionPending: Boolean,
    canCorrectRating: Boolean = false
): Boolean = revealed && (!historyPreview || canCorrectRating) && !interactionPending

internal fun resolveIntroductionStageGesture(
    deltaX: Float,
    deltaY: Float,
    swipeThresholdPx: Float,
    tapSlopPx: Float,
    scrollRequired: Boolean,
    childConsumed: Boolean,
    alreadySubmitted: Boolean,
    ratingEnabled: Boolean = true,
    navigationEnabled: Boolean = false,
    gatedUpwardNavigation: Boolean = false,
    gestureDurationMillis: Long = 0L
): IntroductionStageGesture {
    if (childConsumed || alreadySubmitted) return IntroductionStageGesture.NONE
    val absX = kotlin.math.abs(deltaX)
    val absY = kotlin.math.abs(deltaY)
    if (absX <= tapSlopPx && absY <= tapSlopPx) return IntroductionStageGesture.TAP
    if (navigationEnabled && absX >= swipeThresholdPx && absX > absY * 1.25f) {
        return if (deltaX > 0f) IntroductionStageGesture.PREVIOUS else IntroductionStageGesture.NEXT
    }
    return if (!scrollRequired && gestureDurationMillis <= 600L &&
        deltaY <= -swipeThresholdPx && absY > absX * 1.25f
    ) {
        if (ratingEnabled || gatedUpwardNavigation) IntroductionStageGesture.SWIPE_GOOD
        else if (navigationEnabled) IntroductionStageGesture.NEXT
        else IntroductionStageGesture.NONE
    } else {
        IntroductionStageGesture.NONE
    }
}

internal fun progressDescription(label: String, completed: Int, target: Int, configuredTarget: Int): String =
    if (target == configuredTarget) "$label $completed of $target"
    else "$label $completed of $target available, configured target $configuredTarget"

internal fun completionResultDescription(total: Int, newCompleted: Int, reviewCompleted: Int): String {
    require(total >= 0 && newCompleted >= 0 && reviewCompleted >= 0)
    require(newCompleted + reviewCompleted == total)
    val itemLabel = if (total == 1) "item" else "items"
    val split = buildList {
        if (newCompleted > 0) add("$newCompleted new")
        if (reviewCompleted > 0) add("$reviewCompleted review")
    }
    return "$total $itemLabel completed" + if (split.size > 1) " · ${split.joinToString(" · ")}" else ""
}

internal data class PackageStudyPosition(val position: Int, val total: Int)

internal fun resolvePackageStudyPosition(
    orderedPackageContentIds: List<String>,
    currentContentId: String
): PackageStudyPosition? {
    val canonicalOrder = orderedPackageContentIds.distinct()
    val index = canonicalOrder.indexOf(currentContentId)
    return if (index >= 0) PackageStudyPosition(index + 1, canonicalOrder.size) else null
}

internal object StudyRatingFeedbackPolicy {
    const val pulseMillis = 220
    const val exitMillis = 120
    const val enterMillis = 160
    const val timeoutMillis = 12_000L
}

internal object AndroidTypingSuccessPresentationPolicy {
    const val minimumDwellMillis = 150L
    const val audioWatchdogMillis = 120_000L

    fun expectedAdvanceMillis(audioDurationMillis: Long): Long =
        maxOf(minimumDwellMillis, audioDurationMillis.coerceAtLeast(0L))
}

internal fun shouldStartTypingRevealAnswerAutoplay(
    revealed: Boolean,
    completionPending: Boolean,
    alreadyStarted: Boolean
): Boolean = revealed && !completionPending && !alreadyStarted

internal data class RatingFeedbackAudio(
    val role: AudioRole,
    val path: String?
)

internal fun resolveRatingFeedbackAudio(
    @Suppress("UNUSED_PARAMETER")
    currentFocus: IntroductionPlaybackFocus,
    answerAudioPath: String?,
    @Suppress("UNUSED_PARAMETER")
    exampleAudioPath: String?
): RatingFeedbackAudio = RatingFeedbackAudio(AudioRole.EXPECTED_ANSWER, answerAudioPath)

internal data class OutgoingStudyFeedback(
    val feedbackId: String,
    val learningItemId: String,
    val selectedRating: ReviewRating,
    val audio: RatingFeedbackAudio,
    val origin: IntroductionRatingFeedbackOrigin
)

internal enum class IntroductionRatingFeedbackOrigin { MANUAL_BUTTON, SWIPE_GOOD }

internal data class PendingIntroductionHudRating(
    val feedbackId: String,
    val rating: ReviewRating,
    val targetCount: Int
)

internal fun optimisticIntroductionRating(
    hud: AndroidStudySessionHud,
    feedback: OutgoingStudyFeedback
): PendingIntroductionHudRating = PendingIntroductionHudRating(
    feedbackId = feedback.feedbackId,
    rating = feedback.selectedRating,
    targetCount = hud.ratingCount(feedback.selectedRating) + 1
)

internal fun AndroidStudySessionHud.ratingCount(rating: ReviewRating): Int = when (rating) {
    ReviewRating.AGAIN -> againCount
    ReviewRating.HARD -> hardCount
    ReviewRating.GOOD -> goodCount
    ReviewRating.EASY -> easyCount
}

internal fun displayedIntroductionRatingCount(
    hud: AndroidStudySessionHud,
    rating: ReviewRating,
    pending: PendingIntroductionHudRating?
): Int = if (pending?.rating == rating) maxOf(hud.ratingCount(rating), pending.targetCount) else hud.ratingCount(rating)

internal fun PendingIntroductionHudRating.isAcknowledgedBy(hud: AndroidStudySessionHud): Boolean =
    hud.ratingCount(rating) >= targetCount

internal fun outgoingStudyFeedback(
    state: AndroidStudyState.Introduction,
    rating: ReviewRating,
    currentFocus: IntroductionPlaybackFocus,
    origin: IntroductionRatingFeedbackOrigin = IntroductionRatingFeedbackOrigin.MANUAL_BUTTON
): OutgoingStudyFeedback = OutgoingStudyFeedback(
    feedbackId = "${state.learningItemId}:${rating.name}",
    learningItemId = state.learningItemId,
    selectedRating = rating,
    origin = origin,
    audio = resolveRatingFeedbackAudio(
        currentFocus,
        state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio,
        state.resolvedExampleEnglishAudio
    )
)
