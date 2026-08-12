package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Immutable

@Immutable
internal data class StudyVerticalSpaceInput(
    val viewportWidthDp: Int,
    val viewportHeightDp: Int,
    val imageAspectClass: StudyImageAspectClass,
    val typingRequired: Boolean,
    val externalReservedHeightDp: Int,
    val examplesExpanded: Boolean,
    val typingOuterHeightDp: Int = 0
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
        require(input.externalReservedHeightDp >= 0)
        require(input.typingOuterHeightDp >= 0)
        val short = input.viewportHeightDp < 640
        val spacing = if (short) 6 else 10
        val typingMin = if (input.typingRequired) {
            input.typingOuterHeightDp.takeIf { it > 0 }
                ?: TypingFieldLayoutMetricsResolver.resolve(input.viewportWidthDp).outerMinimumHeightDp
        } else {
            0
        }
        val reserved =
            input.externalReservedHeightDp +
                84 + // translation / POS
                (if (input.typingRequired) typingMin else 0) + // timer shares translation row
                (if (input.examplesExpanded) 180 else 0) +
                spacing * 3 // image/lexical/timer/typing gaps inside the body
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
