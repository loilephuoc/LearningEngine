package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable

@Immutable
internal data class StudyVerticalSpaceInput(
    val viewportWidthDp: Int,
    val viewportHeightDp: Int,
    val imageAspectClass: StudyImageAspectClass,
    val typingRequired: Boolean,
    val decisionDockRequired: Boolean,
    val bottomControlsHeightDp: Int,
    val inventoryVisible: Boolean,
    val examplesExpanded: Boolean,
    val typographyScale: Float = 1f
)

@Immutable
internal data class StudyVerticalSpaceAllocation(
    val imageMaxWidthDp: Int,
    val imageMaxHeightDp: Int,
    val typingMinHeightDp: Int,
    val verticalSpacingDp: Int,
    val requiresBoundedScroll: Boolean,
    val imagePresentationClass: StudyImageAspectClass
)

internal object StudyVerticalSpaceAllocationResolver {
    fun resolve(input: StudyVerticalSpaceInput): StudyVerticalSpaceAllocation {
        require(input.viewportWidthDp > 0)
        require(input.viewportHeightDp > 0)
        require(input.bottomControlsHeightDp >= 0)
        require(input.typographyScale > 0f)
        val short = input.viewportHeightDp < 640
        val spacing = if (short) 6 else 10
        val typingMin = if (input.typingRequired) (116 * input.typographyScale).toInt() else 0
        val reserved =
            84 + // translation / POS
                (if (input.typingRequired) 36 + typingMin else 0) + // timer + true field minimum
                (if (input.decisionDockRequired) input.bottomControlsHeightDp else 0) +
                (if (input.inventoryVisible) 76 else 0) +
                (if (input.examplesExpanded) 180 else 0) +
                spacing * 4 + 24 // safe bottom and inter-section space
        val remaining = (input.viewportHeightDp - reserved).coerceAtLeast(0)
        val imageHeight = remaining
        return StudyVerticalSpaceAllocation(
            imageMaxWidthDp = input.viewportWidthDp,
            imageMaxHeightDp = imageHeight,
            typingMinHeightDp = typingMin,
            verticalSpacingDp = spacing,
            requiresBoundedScroll = reserved > input.viewportHeightDp,
            imagePresentationClass = input.imageAspectClass
        )
    }
}
