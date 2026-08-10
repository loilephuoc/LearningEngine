package vn.loi.learning.android.study.design

import vn.loi.learning.domain.study.recall.RecallOutcome

internal data class ClozePresentation(
    val completedSentence: String,
    val clozeTranslation: String?,
    val supportingExample: String?,
    val supportingExampleTranslation: String?
)

internal fun resolveClozePresentation(
    prefix: String,
    answer: String,
    suffix: String,
    example: String?,
    translation: String?
): ClozePresentation {
    val completed = prefix + answer + suffix
    val duplicate = equivalentStudyText(completed, example)
    return ClozePresentation(
        completedSentence = completed,
        clozeTranslation = translation?.takeIf { duplicate && it.isNotBlank() },
        supportingExample = example?.takeIf { !duplicate && it.isNotBlank() },
        supportingExampleTranslation = translation?.takeIf { !duplicate && it.isNotBlank() }
    )
}

internal fun resolveExampleCompletionDensity(
    base: StudyContentDensity,
    imeVisible: Boolean,
    sentenceLength: Int,
    translationLength: Int
): StudyContentDensity = when {
    imeVisible || sentenceLength + translationLength > 180 -> StudyContentDensity.DENSE
    base == StudyContentDensity.RELAXED && sentenceLength < 80 -> StudyContentDensity.RELAXED
    else -> StudyContentDensity.STANDARD
}

internal fun exampleCompletionFeedback(outcome: RecallOutcome?): StudyFeedbackVisualState = when (outcome) {
    RecallOutcome.CORRECT -> StudyFeedbackVisualState.CORRECT
    RecallOutcome.INCORRECT -> StudyFeedbackVisualState.INCORRECT
    RecallOutcome.REVEALED -> StudyFeedbackVisualState.SELECTED
    null -> StudyFeedbackVisualState.NEUTRAL
    else -> StudyFeedbackVisualState.SELECTED
}

internal fun clozeMotionDurationMillis(reducedMotion: Boolean): Int =
    studyMotionDurationMillis(StudyMotionRole.REVEAL, reducedMotion)

private fun equivalentStudyText(first: String?, second: String?): Boolean {
    if (first.isNullOrBlank() || second.isNullOrBlank()) return false
    fun normalize(value: String) = value.trim().replace(Regex("\\s+"), " ").lowercase()
    return normalize(first) == normalize(second)
}
