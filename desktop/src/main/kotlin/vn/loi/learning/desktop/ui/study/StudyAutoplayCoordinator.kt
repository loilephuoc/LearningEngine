package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode

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
        if (!transition.answerRevealed) {
            if (effective.controlMode != StudyPresentationControlMode.MANUAL) return null
            return when {
                effective.autoplayVietnameseMeaning ->
                    availability.vietnameseMeaningAudio
                effective.autoplayPrimaryEnglish ->
                    availability.primaryEnglishAudio
                else -> null
            }
        }
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

fun questionTransitionAvailability(
    availability: StudyPresentationAvailability,
    scene: LearningScene?
): StudyPresentationAvailability {
    val blocks = scene?.let { current ->
        current.blocks + current.supportingScenes.flatMap { it.blocks }
    }.orEmpty()
    fun audio(role: PresentedAudioRole): Path? =
        blocks.filterIsInstance<PresentedLearningBlock.Audio>()
            .firstOrNull { it.role == role }
            ?.path
    return availability.copy(
        primaryEnglishAudio = audio(PresentedAudioRole.PRIMARY_WORD),
        vietnameseMeaningAudio = audio(PresentedAudioRole.MEANING_TRANSLATION),
        englishExampleAudio = audio(PresentedAudioRole.EXAMPLE_PRIMARY),
        vietnameseExampleAudio = audio(PresentedAudioRole.EXAMPLE_TRANSLATION)
    )
}
