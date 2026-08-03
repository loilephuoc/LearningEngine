package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import org.jetbrains.skia.Image
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
    private var submitted = false

    fun accept(rawInput: String, mediaState: ImageRecallMediaState): Boolean {
        if (submitted || mediaState != ImageRecallMediaState.READY || rawInput.isBlank()) return false
        submitted = true
        return true
    }
}
