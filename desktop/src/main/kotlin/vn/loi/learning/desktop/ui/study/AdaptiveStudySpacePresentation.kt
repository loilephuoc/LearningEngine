package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable

@Immutable
internal data class AdaptiveStudySpaceRequest(
    val isAnswer: Boolean,
    val examplesExpanded: Boolean,
    val hasExamples: Boolean,
    val viewportWidthDp: Int,
    val viewportHeightDp: Int,
    val bottomControlHeightDp: Int,
    val intrinsicExampleHeightDp: Int,
    val imageAspectClass: StudyImageAspectClass? = null
)

@Immutable
internal data class AdaptiveStudySpacePresentation(
    val imageMinimumHeightDp: Int,
    val imageMaximumHeightDp: Int,
    val verticalSpacingDp: Int,
    val typingFieldMinimumHeightDp: Int,
    val showAllExampleContent: Boolean,
    val allowBoundedContentScroll: Boolean
)

internal object AdaptiveStudySpacePresentationResolver {
    fun resolve(request: AdaptiveStudySpaceRequest): AdaptiveStudySpacePresentation {
        require(request.viewportWidthDp > 0)
        require(request.viewportHeightDp > 0)
        require(request.bottomControlHeightDp >= 0)
        require(request.intrinsicExampleHeightDp >= 0)

        val short = request.viewportHeightDp < 640
        val compact = request.viewportWidthDp < 600 || request.viewportHeightDp < 760
        val spacing = when {
            short -> 6
            compact -> 8
            else -> 12
        }
        val typingMinimum = when {
            request.viewportWidthDp >= 1024 -> 168
            request.viewportWidthDp >= 600 -> 156
            compact -> 144
            else -> 132
        }
        val fixedAnswerContent = 184 + request.bottomControlHeightDp / 5
        val examplesRequirement =
            if (request.examplesExpanded && request.hasExamples) {
                request.intrinsicExampleHeightDp.coerceAtLeast(180)
            } else if (request.hasExamples) {
                52
            } else {
                0
            }
        val availableForImage =
            (request.viewportHeightDp - fixedAnswerContent - examplesRequirement - spacing * 4)
                .coerceAtLeast(120)
        val imageMinimum = if (compact) 120 else 144
        val qualityCap = when (request.imageAspectClass) {
            StudyImageAspectClass.WIDE_LANDSCAPE,
            StudyImageAspectClass.EXTREME -> (request.viewportHeightDp * 0.48f).toInt()
            else -> (request.viewportHeightDp * 0.58f).toInt()
        }
        val imageMaximum = availableForImage
            .coerceAtLeast(imageMinimum)
            .coerceAtMost(qualityCap.coerceAtLeast(imageMinimum))
        val contentRequired = fixedAnswerContent + examplesRequirement + imageMinimum + spacing * 4

        return AdaptiveStudySpacePresentation(
            imageMinimumHeightDp = imageMinimum,
            imageMaximumHeightDp = imageMaximum,
            verticalSpacingDp = spacing,
            typingFieldMinimumHeightDp = typingMinimum,
            showAllExampleContent = request.examplesExpanded && request.hasExamples,
            allowBoundedContentScroll = request.isAnswer && contentRequired > request.viewportHeightDp
        )
    }
}
