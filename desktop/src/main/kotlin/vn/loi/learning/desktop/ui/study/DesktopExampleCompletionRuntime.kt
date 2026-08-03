package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.study.recall.RecallPlan
import vn.loi.learning.domain.study.recall.RecallPrompt

data class ExampleCompletionPromptPresentation(
    val prefix: String,
    val blank: String,
    val suffix: String
)

enum class ExampleCompletionUnavailableReason {
    INVALID_TARGET_SPAN,
    UNSAFE_BLANK
}

sealed interface ExampleCompletionPresentationResult {
    data class Ready(val prompt: ExampleCompletionPromptPresentation) : ExampleCompletionPresentationResult
    data class Unavailable(val reason: ExampleCompletionUnavailableReason) : ExampleCompletionPresentationResult
}

object ExampleCompletionPresentationResolver {
    fun resolve(plan: RecallPlan): ExampleCompletionPresentationResult? {
        if (DesktopRecallModeRouter.route(plan) != DesktopRecallRenderer.EXAMPLE_COMPLETION) return null
        val prompt = plan.prompt as? RecallPrompt.ExampleCompletion ?: return null
        val span = prompt.targetSpan
        if (span.startInclusive !in 0..prompt.example.length || span.endExclusive > prompt.example.length) {
            return ExampleCompletionPresentationResult.Unavailable(
                ExampleCompletionUnavailableReason.INVALID_TARGET_SPAN
            )
        }
        val blank = prompt.example.substring(span.startInclusive, span.endExclusive)
        if (blank.isEmpty() || blank.any { it != '_' }) {
            return ExampleCompletionPresentationResult.Unavailable(
                ExampleCompletionUnavailableReason.UNSAFE_BLANK
            )
        }
        return ExampleCompletionPresentationResult.Ready(
            ExampleCompletionPromptPresentation(
                prefix = prompt.example.substring(0, span.startInclusive),
                blank = blank,
                suffix = prompt.example.substring(span.endExclusive)
            )
        )
    }
}

class ExampleCompletionSubmissionGate {
    private var submitted = false

    fun accept(rawInput: String, promptAvailable: Boolean): Boolean {
        if (submitted || !promptAvailable || rawInput.isBlank()) return false
        submitted = true
        return true
    }
}
