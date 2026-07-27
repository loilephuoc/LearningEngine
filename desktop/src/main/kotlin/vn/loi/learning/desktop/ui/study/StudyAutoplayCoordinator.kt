package vn.loi.learning.desktop.ui.study

import java.nio.file.Path

data class StudyAutoplayTransition(
    val itemId: String?,
    val answerRevealed: Boolean
)

class StudyAutoplayCoordinator {
    private var lastTransition: StudyAutoplayTransition? = null

    fun nextAutoplay(
        transition: StudyAutoplayTransition,
        availability: StudyPresentationAvailability,
        effective: EffectiveStudyPresentation
    ): Path? {
        if (transition == lastTransition) return null
        lastTransition = transition
        return when {
            effective.autoplayPrimaryEnglish -> availability.primaryEnglishAudio
            effective.autoplayVietnameseMeaning -> availability.vietnameseMeaningAudio
            effective.autoplayEnglishExample -> availability.englishExampleAudio
            effective.autoplayVietnameseExample -> availability.vietnameseExampleAudio
            else -> null
        }
    }
}

fun hiddenLoopPaths(
    availability: StudyPresentationAvailability,
    effective: EffectiveStudyPresentation
): Set<Path> = buildSet {
    if (!effective.showVietnameseMeaning) availability.vietnameseMeaningAudio?.let(::add)
    if (!effective.showEnglishExamples) availability.englishExampleAudio?.let(::add)
    if (!effective.showVietnameseExamples) availability.vietnameseExampleAudio?.let(::add)
}
