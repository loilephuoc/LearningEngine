package vn.loi.learning.desktop.ui.study

enum class StudyViewportClass {
    COMPACT,
    STANDARD,
    WIDE
}

enum class MetadataArrangement {
    INLINE,
    STACKED
}

enum class RatingArrangement {
    HORIZONTAL,
    GRID_2X2
}

data class StudyVisualContentTraits(
    val hasImage: Boolean = false,
    val hasPronunciation: Boolean = false,
    val hasPartOfSpeech: Boolean = false,
    val hasExamples: Boolean = false,
    val hasSchedulerFeedback: Boolean = false
)

data class StudyVisualLayout(
    val viewportClass: StudyViewportClass,
    val contentMaxWidthDp: Int,
    val imageMaxWidthDp: Int,
    val imageMaxHeightDp: Int,
    val identityWordFontSizeSp: Int,
    val identityWordLineHeightSp: Int,
    val metadataArrangement: MetadataArrangement,
    val ratingArrangement: RatingArrangement,
    val sectionSpacingDp: Int,
    val ratingDockReservedHeightDp: Int,
    val preserveRatingReachability: Boolean = true
)

object StudyVisualLayoutResolver {

    const val COMPACT_MAX_WIDTH_DP = 599
    const val STANDARD_MAX_WIDTH_DP = 1023
    const val RATING_GRID_MAX_WIDTH_DP = 479

    fun resolve(
        viewportWidthDp: Int,
        viewportHeightDp: Int,
        traits: StudyVisualContentTraits
    ): StudyVisualLayout {
        require(viewportWidthDp > 0) { "Viewport width must be positive, got: $viewportWidthDp" }
        require(viewportHeightDp > 0) { "Viewport height must be positive, got: $viewportHeightDp" }

        val viewportClass = when {
            viewportWidthDp <= COMPACT_MAX_WIDTH_DP -> StudyViewportClass.COMPACT
            viewportWidthDp <= STANDARD_MAX_WIDTH_DP -> StudyViewportClass.STANDARD
            else -> StudyViewportClass.WIDE
        }

        val contentMaxWidthDp = when (viewportClass) {
            StudyViewportClass.COMPACT -> viewportWidthDp
            StudyViewportClass.STANDARD -> 680
            StudyViewportClass.WIDE -> 800
        }

        val sectionSpacingDp = when (viewportClass) {
            StudyViewportClass.COMPACT -> 8
            StudyViewportClass.STANDARD -> 12
            StudyViewportClass.WIDE -> 16
        }
        val ratingDockReservedHeightDp = if (viewportWidthDp <= RATING_GRID_MAX_WIDTH_DP) 144 else 88
        val chromeReservedHeightDp = 68 + ratingDockReservedHeightDp + 32
        val nonImageAnswerHeightDp =
            90 +
                100 +
                (if (traits.hasExamples) 150 else 0) +
                (if (traits.hasSchedulerFeedback) 90 else 0) +
                64 +
                sectionSpacingDp * 5
        val verticalImageBudgetDp =
            (viewportHeightDp - chromeReservedHeightDp - nonImageAnswerHeightDp)
                .coerceAtLeast(160)

        val (imageMaxWidthDp, imageMaxHeightDp) = when {
            !traits.hasImage -> Pair(0, 0)
            viewportClass == StudyViewportClass.COMPACT -> {
                Pair(
                    (viewportWidthDp - 32).coerceAtLeast(240).coerceAtMost(560),
                    minOf(260, verticalImageBudgetDp)
                )
            }
            viewportClass == StudyViewportClass.STANDARD -> {
                Pair(620, minOf(240, verticalImageBudgetDp))
            }
            else -> {
                Pair(620, minOf(240, verticalImageBudgetDp))
            }
        }

        val (identityFontSizeSp, identityLineHeightSp) = when (viewportClass) {
            StudyViewportClass.COMPACT -> Pair(36, 44)
            StudyViewportClass.STANDARD -> Pair(46, 52)
            StudyViewportClass.WIDE -> Pair(52, 58)
        }

        val metadataArrangement = when {
            viewportClass == StudyViewportClass.COMPACT -> MetadataArrangement.STACKED
            traits.hasPronunciation && traits.hasPartOfSpeech && viewportWidthDp < 480 -> MetadataArrangement.STACKED
            else -> MetadataArrangement.INLINE
        }

        val ratingArrangement = when {
            viewportWidthDp <= RATING_GRID_MAX_WIDTH_DP -> RatingArrangement.GRID_2X2
            else -> RatingArrangement.HORIZONTAL
        }

        return StudyVisualLayout(
            viewportClass = viewportClass,
            contentMaxWidthDp = contentMaxWidthDp,
            imageMaxWidthDp = imageMaxWidthDp,
            imageMaxHeightDp = imageMaxHeightDp,
            identityWordFontSizeSp = identityFontSizeSp,
            identityWordLineHeightSp = identityLineHeightSp,
            metadataArrangement = metadataArrangement,
            ratingArrangement = ratingArrangement,
            sectionSpacingDp = sectionSpacingDp,
            ratingDockReservedHeightDp = ratingDockReservedHeightDp,
            preserveRatingReachability = true
        )
    }
}
