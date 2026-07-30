package vn.loi.learning.desktop.ui.study

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal data class FullAnswerMeasuredBlocks(
    val identityHeight: Int,
    val meaningHeight: Int,
    val requiredExampleHeight: Int,
    val schedulerFeedbackHeight: Int = 0,
    val continuationHeight: Int = 0
)

internal data class FullAnswerFitGeometry(
    val identityTop: Int,
    val identityBottom: Int,
    val imageTop: Int,
    val imageBottom: Int,
    val meaningTop: Int,
    val meaningBottom: Int,
    val requiredExampleTop: Int,
    val requiredExampleBottom: Int,
    val schedulerFeedbackTop: Int?,
    val schedulerFeedbackBottom: Int?,
    val continuationTop: Int?,
    val totalHeight: Int,
    val imageHeight: Int,
    val fitsWithoutScroll: Boolean
)

internal fun resolveFullAnswerFitGeometry(
    availableHeight: Int,
    verticalPadding: Int,
    sectionGap: Int,
    minimumImageHeight: Int,
    hasImage: Boolean = true,
    blocks: FullAnswerMeasuredBlocks
): FullAnswerFitGeometry {
    require(availableHeight > 0)
    require(verticalPadding >= 0)
    require(sectionGap >= 0)
    require(minimumImageHeight > 0)

    val requiredHeights =
        listOf(
            blocks.identityHeight,
            blocks.meaningHeight,
            blocks.requiredExampleHeight
        ).filter { it > 0 }
    val requiredGapCount = (requiredHeights.size - 1).coerceAtLeast(0) +
        if (hasImage && requiredHeights.isNotEmpty()) 1 else 0
    val requiredWithoutImage =
        verticalPadding * 2 + requiredHeights.sum() + requiredGapCount * sectionGap
    val measuredImageHeight =
        if (hasImage) {
            (availableHeight - requiredWithoutImage).coerceAtLeast(minimumImageHeight)
        } else {
            0
        }
    val fits = requiredWithoutImage + measuredImageHeight <= availableHeight

    var cursor = verticalPadding
    val identityTop = cursor
    val identityBottom = identityTop + blocks.identityHeight
    cursor = identityBottom + if (hasImage) sectionGap else 0
    val imageTop = cursor
    val imageBottom = imageTop + measuredImageHeight
    cursor =
        imageBottom +
            if (blocks.meaningHeight > 0 && (hasImage || blocks.identityHeight > 0)) sectionGap
            else 0
    val meaningTop = cursor
    val meaningBottom = meaningTop + blocks.meaningHeight
    cursor =
        meaningBottom +
            if (blocks.requiredExampleHeight > 0 && blocks.meaningHeight > 0) sectionGap else 0
    val requiredExampleTop = cursor
    val requiredExampleBottom = requiredExampleTop + blocks.requiredExampleHeight
    cursor = requiredExampleBottom

    val feedbackTop =
        if (blocks.schedulerFeedbackHeight > 0) cursor + sectionGap else null
    val feedbackBottom =
        feedbackTop?.plus(blocks.schedulerFeedbackHeight)
    if (feedbackBottom != null) cursor = feedbackBottom

    val continuationTop =
        if (blocks.continuationHeight > 0) cursor + sectionGap else null
    if (continuationTop != null) cursor = continuationTop + blocks.continuationHeight

    return FullAnswerFitGeometry(
        identityTop = identityTop,
        identityBottom = identityBottom,
        imageTop = imageTop,
        imageBottom = imageBottom,
        meaningTop = meaningTop,
        meaningBottom = meaningBottom,
        requiredExampleTop = requiredExampleTop,
        requiredExampleBottom = requiredExampleBottom,
        schedulerFeedbackTop = feedbackTop,
        schedulerFeedbackBottom = feedbackBottom,
        continuationTop = continuationTop,
        totalHeight = cursor + verticalPadding,
        imageHeight = measuredImageHeight,
        fitsWithoutScroll = fits
    )
}

@Composable
internal fun FullAnswerFitLayout(
    availableHeightDp: Int,
    layout: StudyVisualLayout,
    hasImage: Boolean,
    modifier: Modifier = Modifier,
    identity: @Composable () -> Unit,
    image: @Composable (Int) -> Unit,
    meaning: @Composable () -> Unit,
    requiredExample: @Composable () -> Unit,
    schedulerFeedback: (@Composable () -> Unit)? = null,
    continuation: (@Composable () -> Unit)? = null
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val loose = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        fun measure(slot: String, content: @Composable () -> Unit) =
            subcompose(slot, content).single().measure(loose)

        val identityPlaceable = measure("identity", identity)
        val meaningPlaceable = measure("meaning", meaning)
        val examplePlaceable = measure("required-example", requiredExample)
        val feedbackPlaceable = schedulerFeedback?.let { measure("scheduler-feedback", it) }
        val continuationPlaceable = continuation?.let { measure("continuation", it) }
        val density = this
        val verticalPaddingPx =
            with(density) { layout.fullAnswerCardVerticalPaddingDp.dp.roundToPx() }
        val sectionGapPx =
            with(density) { layout.fullAnswerSectionGapDp.dp.roundToPx() }
        val availableHeightPx =
            with(density) { availableHeightDp.dp.roundToPx() }.coerceAtLeast(1)
        val minimumImageHeightPx =
            with(density) { MINIMUM_IMAGE_HEIGHT_DP.dp.roundToPx() }
        val geometry =
            resolveFullAnswerFitGeometry(
                availableHeight = availableHeightPx,
                verticalPadding = verticalPaddingPx,
                sectionGap = sectionGapPx,
                minimumImageHeight = minimumImageHeightPx,
                blocks =
                    FullAnswerMeasuredBlocks(
                        identityHeight = identityPlaceable.height,
                        meaningHeight = meaningPlaceable.height,
                        requiredExampleHeight = examplePlaceable.height,
                        schedulerFeedbackHeight = feedbackPlaceable?.height ?: 0,
                        continuationHeight = continuationPlaceable?.height ?: 0
                    ),
                hasImage = hasImage
            )
        val imageHeightDp =
            (geometry.imageHeight / density.density).roundToInt().coerceAtLeast(1)
        val imagePlaceable =
            subcompose("image") { image(imageHeightDp) }
                .single()
                .measure(
                    constraints.copy(
                        minHeight = 0,
                        maxHeight = geometry.imageHeight
                    )
                )

        layout(constraints.maxWidth, geometry.totalHeight) {
            identityPlaceable.placeRelative(0, geometry.identityTop)
            imagePlaceable.placeRelative(0, geometry.imageTop)
            meaningPlaceable.placeRelative(0, geometry.meaningTop)
            examplePlaceable.placeRelative(0, geometry.requiredExampleTop)
            feedbackPlaceable?.placeRelative(0, requireNotNull(geometry.schedulerFeedbackTop))
            continuationPlaceable?.placeRelative(0, requireNotNull(geometry.continuationTop))
        }
    }
}

private const val MINIMUM_IMAGE_HEIGHT_DP = 96
