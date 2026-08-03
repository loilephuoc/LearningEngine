package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.study.recall.RecallChoice
import vn.loi.learning.domain.study.recall.RecallMode
import vn.loi.learning.domain.study.recall.RecallPlan
import vn.loi.learning.domain.study.recall.RecallPrompt

enum class DesktopRecallRenderer {
    TYPING,
    MULTIPLE_CHOICE,
    UNSUPPORTED
}

object DesktopRecallModeRouter {
    fun route(plan: RecallPlan?): DesktopRecallRenderer = when (plan?.mode) {
        RecallMode.TYPING -> DesktopRecallRenderer.TYPING
        RecallMode.MULTIPLE_CHOICE -> DesktopRecallRenderer.MULTIPLE_CHOICE
        null -> DesktopRecallRenderer.UNSUPPORTED
        else -> DesktopRecallRenderer.UNSUPPORTED
    }
}

data class MultipleChoiceOptionPresentation(
    val optionId: String,
    val text: String,
    val position: Int,
    val total: Int
)

data class MultipleChoicePresentation(
    val question: String,
    val options: List<MultipleChoiceOptionPresentation>
)

object MultipleChoicePresentationResolver {
    fun resolve(plan: RecallPlan): MultipleChoicePresentation? {
        if (DesktopRecallModeRouter.route(plan) != DesktopRecallRenderer.MULTIPLE_CHOICE) return null
        val prompt = plan.prompt as? RecallPrompt.MultipleChoice ?: return null
        if (prompt.choices.size !in 2..4) return null
        return MultipleChoicePresentation(
            question = prompt.question,
            options = prompt.choices.mapIndexed { index, choice -> choice.toPresentation(index, prompt.choices.size) }
        )
    }

    private fun RecallChoice.toPresentation(index: Int, total: Int) = MultipleChoiceOptionPresentation(
        optionId = id,
        text = text,
        position = index + 1,
        total = total
    )
}

class MultipleChoiceSubmissionGate {
    private var submittedOptionId: String? = null

    fun accept(optionId: String, availableOptionIds: Set<String>): Boolean {
        if (submittedOptionId != null || optionId !in availableOptionIds) return false
        submittedOptionId = optionId
        return true
    }

    val selectedOptionId: String?
        get() = submittedOptionId
}

internal fun multipleChoiceOptionForNumberKey(
    keyNumber: Int,
    options: List<MultipleChoiceOptionPresentation>
): MultipleChoiceOptionPresentation? = options.getOrNull(keyNumber - 1)
