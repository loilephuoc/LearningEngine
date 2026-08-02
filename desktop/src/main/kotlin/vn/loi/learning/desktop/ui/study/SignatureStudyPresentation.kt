package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable

internal enum class SignatureStudyViewport {
    EXPANDED,
    STANDARD,
    COMPACT,
    COMPRESSED
}

internal enum class SignatureCompressionStep {
    SPACING,
    IMAGE,
    SCHEDULER,
    EXAMPLES,
    METADATA
}

@Immutable
internal data class SignatureStudyPresentation(
    val viewport: SignatureStudyViewport,
    val stageMaxWidthDp: Int,
    val headerHeightDp: Int,
    val sectionSpacingDp: Int,
    val imageHeightFraction: Float,
    val maximumVisibleExamples: Int,
    val compactScheduler: Boolean,
    val compactMetadata: Boolean,
    val allowAnswerContentScroll: Boolean,
    val compressionOrder: List<SignatureCompressionStep>
)

internal object SignatureStudyPresentationResolver {
    private val compressionOrder = listOf(
        SignatureCompressionStep.SPACING,
        SignatureCompressionStep.IMAGE,
        SignatureCompressionStep.SCHEDULER,
        SignatureCompressionStep.EXAMPLES,
        SignatureCompressionStep.METADATA
    )

    fun resolve(widthDp: Int, heightDp: Int): SignatureStudyPresentation {
        require(widthDp > 0)
        require(heightDp > 0)
        val viewport = when {
            widthDp >= 1180 && heightDp >= 900 -> SignatureStudyViewport.EXPANDED
            widthDp >= 760 && heightDp >= 760 -> SignatureStudyViewport.STANDARD
            widthDp >= 560 && heightDp >= 640 -> SignatureStudyViewport.COMPACT
            else -> SignatureStudyViewport.COMPRESSED
        }
        return when (viewport) {
            SignatureStudyViewport.EXPANDED -> presentation(viewport, 760, 64, 20, 0.34f, 2, false, false, false)
            SignatureStudyViewport.STANDARD -> presentation(viewport, 700, 60, 16, 0.30f, 2, false, false, false)
            SignatureStudyViewport.COMPACT -> presentation(viewport, 620, 56, 12, 0.25f, 1, true, false, false)
            SignatureStudyViewport.COMPRESSED -> presentation(viewport, widthDp, 52, 8, 0.20f, 1, true, true, true)
        }
    }

    private fun presentation(
        viewport: SignatureStudyViewport,
        stageMaxWidthDp: Int,
        headerHeightDp: Int,
        sectionSpacingDp: Int,
        imageHeightFraction: Float,
        maximumVisibleExamples: Int,
        compactScheduler: Boolean,
        compactMetadata: Boolean,
        allowAnswerContentScroll: Boolean
    ) = SignatureStudyPresentation(
        viewport = viewport,
        stageMaxWidthDp = stageMaxWidthDp,
        headerHeightDp = headerHeightDp,
        sectionSpacingDp = sectionSpacingDp,
        imageHeightFraction = imageHeightFraction,
        maximumVisibleExamples = maximumVisibleExamples,
        compactScheduler = compactScheduler,
        compactMetadata = compactMetadata,
        allowAnswerContentScroll = allowAnswerContentScroll,
        compressionOrder = compressionOrder
    )
}
