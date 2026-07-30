package vn.loi.learning.desktop.ui.study

import java.nio.file.Path

data class StudyAutoplayTransition(
    val itemId: String?,
    val phase: StudyAutoplayPhase
)

enum class StudyAutoplayPhase {
    QUESTION_BOUND,
    ANSWER_REVEALED
}

class StudyAutoplayCoordinator {
    private var lastTransition: StudyAutoplayTransition? = null

    fun nextAutoplay(
        transition: StudyAutoplayTransition,
        questionAvailability: StudyPresentationAvailability,
        questionEffective: EffectiveStudyPresentation,
        fullAnswerAudio: FullAnswerAudio
    ): Path? {
        if (transition == lastTransition) return null
        val previous = lastTransition
        lastTransition = transition
        if (transition.phase == StudyAutoplayPhase.QUESTION_BOUND) {
            return when {
                questionEffective.autoplayPrimaryEnglish ->
                    questionAvailability.primaryEnglishAudio
                questionEffective.autoplayVietnameseMeaning ->
                    questionAvailability.vietnameseMeaningAudio
                else -> null
            }
        }
        val isLiveReveal =
            previous?.itemId == transition.itemId &&
                previous?.phase == StudyAutoplayPhase.QUESTION_BOUND
        return if (isLiveReveal) fullAnswerAudio.primaryEnglish else null
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
    scene: LearningScene?,
    effective: EffectiveStudyPresentation
): StudyPresentationAvailability {
    val availableBlocks = scene?.let { current ->
        current.blocks + current.supportingScenes.flatMap { it.blocks }
    }.orEmpty()
    val blocks = visibleStudySceneBlocks(availableBlocks, effective)
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
