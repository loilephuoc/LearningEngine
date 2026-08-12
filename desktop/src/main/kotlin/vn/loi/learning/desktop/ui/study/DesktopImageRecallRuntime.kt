package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.jetbrains.skia.Image
import vn.loi.learning.application.recall.RecallAnswerContractEvaluator
import vn.loi.learning.application.recall.RecallAnswerMatch
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluation
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingRecallPrompt
import vn.loi.learning.domain.study.recall.RecallAnswerContract
import vn.loi.learning.domain.study.recall.RecallPlan
import vn.loi.learning.domain.study.recall.RecallPrompt

enum class ImageRecallMediaState {
    LOADING,
    READY,
    UNAVAILABLE,
    DECODE_FAILED
}

data class ImageRecallPresentation(
    val imageResourceId: String,
    val image: PresentedLearningBlock.Image?,
    val unavailable: PresentedLearningBlock.Unavailable?,
    val safeDescription: String?
)

object ImageRecallPresentationResolver {
    fun resolve(
        plan: RecallPlan,
        questionBlocks: List<PresentedLearningBlock>
    ): ImageRecallPresentation? {
        if (DesktopRecallModeRouter.route(plan) != DesktopRecallRenderer.IMAGE_RECALL) return null
        val prompt = plan.prompt as? RecallPrompt.ImageRecall ?: return null
        val image = questionBlocks.filterIsInstance<PresentedLearningBlock.Image>().firstOrNull()
        return ImageRecallPresentation(
            imageResourceId = prompt.image.value,
            image = image,
            unavailable = questionBlocks.filterIsInstance<PresentedLearningBlock.Unavailable>().firstOrNull(),
            safeDescription = image?.description?.takeIf(String::isNotBlank)
        )
    }
}

object DesktopImageRecallMediaProbe {
    fun inspect(path: Path): ImageRecallMediaState = runCatching {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) return ImageRecallMediaState.UNAVAILABLE
        Image.makeFromEncoded(Files.readAllBytes(path)).let { image ->
            check(image.width > 0 && image.height > 0)
        }
        ImageRecallMediaState.READY
    }.getOrElse { ImageRecallMediaState.DECODE_FAILED }
}

class ImageRecallSubmissionGate {
    private var submittedRevision: Long? = null

    fun accept(revision: Long, correct: Boolean, mediaState: ImageRecallMediaState): Boolean {
        if (submittedRevision != null || !correct || mediaState != ImageRecallMediaState.READY) return false
        submittedRevision = revision
        return true
    }
}

data class ImageRecallInputState(
    val value: TextFieldValue = TextFieldValue(),
    val evaluation: RecallAnswerMatch? = null,
    val explicitIncorrectFeedback: Boolean = false,
    val automaticSuccessRequested: Boolean = false,
    val revision: Long = 0
)

object ImageRecallInputInteraction {
    fun update(
        state: ImageRecallInputState,
        value: TextFieldValue,
        answerContract: RecallAnswerContract
    ): ImageRecallInputState {
        if (value.text == state.value.text && value.composition == state.value.composition) {
            return state.copy(value = value)
        }
        val evaluation = value.text.takeIf(String::isNotBlank)
            ?.let { RecallAnswerContractEvaluator.evaluate(answerContract, it) }
        return state.copy(
            value = value,
            evaluation = evaluation,
            explicitIncorrectFeedback = false,
            automaticSuccessRequested = evaluation?.correct == true && value.composition == null,
            revision = state.revision + 1
        )
    }

    fun update(
        state: ImageRecallInputState,
        text: String,
        answerContract: RecallAnswerContract
    ): ImageRecallInputState =
        update(state, TextFieldValue(text, selection = TextRange(text.length)), answerContract)

    fun submitIncorrect(
        state: ImageRecallInputState,
        answerContract: RecallAnswerContract
    ): ImageRecallInputState {
        if (state.value.text.isBlank()) return state
        val evaluation = RecallAnswerContractEvaluator.evaluate(answerContract, state.value.text)
        return state.copy(
            evaluation = evaluation,
            explicitIncorrectFeedback = !evaluation.correct,
            automaticSuccessRequested = evaluation.correct && state.value.composition == null,
            revision = state.revision + 1
        )
    }
}

fun recallContractTypingEvaluation(
    answerContract: RecallAnswerContract,
    input: String
): TypingAnswerEvaluation =
    TypingAnswerEvaluator().evaluate(
        TypingRecallPrompt(
            if (RecallAnswerContractEvaluator.evaluate(answerContract, input).correct) {
                input
            } else {
                answerContract.canonicalAnswer
            }
        ),
        input
    )
