package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.study.recall.RecallPlan
import vn.loi.learning.domain.study.recall.RecallPrompt

data class ListeningRecallPresentation(
    val audioResourceId: String,
    val blocks: List<PresentedLearningBlock>,
    val audioAvailable: Boolean
)

object ListeningRecallPresentationResolver {
    fun resolve(
        plan: RecallPlan,
        questionBlocks: List<PresentedLearningBlock>
    ): ListeningRecallPresentation? {
        if (DesktopRecallModeRouter.route(plan) != DesktopRecallRenderer.LISTENING) return null
        val prompt = plan.prompt as? RecallPrompt.Listening ?: return null
        val audio = questionBlocks.filterIsInstance<PresentedLearningBlock.Audio>()
            .firstOrNull { it.role == PresentedAudioRole.PRIMARY_WORD }
        val unavailable = questionBlocks.filterIsInstance<PresentedLearningBlock.Unavailable>().firstOrNull()
        return ListeningRecallPresentation(
            audioResourceId = prompt.audio.value,
            blocks = listOfNotNull(audio ?: unavailable),
            audioAvailable = audio != null
        )
    }
}

class ListeningSubmissionGate {
    private var submitted = false

    fun accept(rawInput: String): Boolean {
        if (submitted || rawInput.isBlank()) return false
        submitted = true
        return true
    }
}
