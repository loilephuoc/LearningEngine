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

enum class StudyHeightMode {
    COMFORTABLE,
    COMPACT_HEIGHT,
    MINIMUM_HEIGHT
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
    val heightMode: StudyHeightMode,
    val contentMaxWidthDp: Int,
    val imageMaxWidthDp: Int,
    val imageMaxHeightDp: Int,
    val identityWordFontSizeSp: Int,
    val identityWordLineHeightSp: Int,
    val metadataArrangement: MetadataArrangement,
    val ratingArrangement: RatingArrangement,
    val sectionSpacingDp: Int,
    val ratingDockReservedHeightDp: Int,
    val statisticsDashboardReservedHeightDp: Int,
    val headerReservedHeightDp: Int,
    val availableAnswerHeightDp: Int,
    val preserveRatingReachability: Boolean = true
)

data class StudyDisplayEnvironment(
    val widthDp: Int,
    val heightDp: Int,
    val density: Float,
    val fontScale: Float
) {
    init {
        require(widthDp > 0) { "Viewport width must be positive, got: $widthDp" }
        require(heightDp > 0) { "Viewport height must be positive, got: $heightDp" }
        require(density > 0f) { "Display density must be positive." }
        require(fontScale > 0f) { "Display font scale must be positive." }
    }
}

object StudyVisualLayoutResolver {

    const val COMPACT_MAX_WIDTH_DP = 599
    const val STANDARD_MAX_WIDTH_DP = 1023
    const val RATING_GRID_MAX_WIDTH_DP = 479

    fun resolve(
        viewportWidthDp: Int,
        viewportHeightDp: Int,
        traits: StudyVisualContentTraits
    ): StudyVisualLayout = resolve(
        StudyDisplayEnvironment(viewportWidthDp, viewportHeightDp, 1f, 1f),
        traits
    )

    fun resolve(
        environment: StudyDisplayEnvironment,
        traits: StudyVisualContentTraits
    ): StudyVisualLayout {
        val viewportWidthDp = environment.widthDp
        val viewportHeightDp = environment.heightDp

        val viewportClass = when {
            viewportWidthDp <= COMPACT_MAX_WIDTH_DP -> StudyViewportClass.COMPACT
            viewportWidthDp <= STANDARD_MAX_WIDTH_DP -> StudyViewportClass.STANDARD
            else -> StudyViewportClass.WIDE
        }
        val effectiveHeightDp =
            (viewportHeightDp / environment.fontScale.coerceAtLeast(1f)).toInt()
        val heightMode = when {
            effectiveHeightDp < 720 -> StudyHeightMode.MINIMUM_HEIGHT
            effectiveHeightDp < 900 -> StudyHeightMode.COMPACT_HEIGHT
            else -> StudyHeightMode.COMFORTABLE
        }

        val contentMaxWidthDp = when (viewportClass) {
            StudyViewportClass.COMPACT -> viewportWidthDp
            StudyViewportClass.STANDARD -> 680
            StudyViewportClass.WIDE -> 800
        }

        val sectionSpacingDp = when (heightMode) {
            StudyHeightMode.MINIMUM_HEIGHT -> 6
            StudyHeightMode.COMPACT_HEIGHT -> 8
            StudyHeightMode.COMFORTABLE -> when (viewportClass) {
                StudyViewportClass.COMPACT -> 8
                StudyViewportClass.STANDARD -> 12
                StudyViewportClass.WIDE -> 16
            }
        }
        val ratingDockReservedHeightDp = if (viewportWidthDp <= RATING_GRID_MAX_WIDTH_DP) 144 else 88
        val statisticsDashboardReservedHeightDp =
            if (viewportClass == StudyViewportClass.COMPACT) 112 else 72
        val headerReservedHeightDp = statisticsDashboardReservedHeightDp +
            when (heightMode) {
                StudyHeightMode.COMFORTABLE -> 56
                StudyHeightMode.COMPACT_HEIGHT -> 48
                StudyHeightMode.MINIMUM_HEIGHT -> 40
            }
        val fixedChromeHeightDp =
            headerReservedHeightDp + ratingDockReservedHeightDp + 32 + 16
        val fontScaleReserveDp = ((environment.fontScale - 1f).coerceAtLeast(0f) * 96).toInt()
        val nonImageAnswerHeightDp =
            80 +
                88 +
                (if (traits.hasExamples) 96 else 0) +
                (if (traits.hasSchedulerFeedback) 60 else 0) +
                32 +
                sectionSpacingDp * 4 +
                fontScaleReserveDp
        val availableAnswerHeightDp =
            (viewportHeightDp - fixedChromeHeightDp).coerceAtLeast(0)
        val verticalImageBudgetDp =
            (availableAnswerHeightDp - nonImageAnswerHeightDp)
                .coerceAtLeast(120)

        val (imageMaxWidthDp, imageMaxHeightDp) = when {
            !traits.hasImage -> Pair(0, 0)
            viewportClass == StudyViewportClass.COMPACT -> {
                Pair(
                    (viewportWidthDp - 32).coerceAtLeast(240).coerceAtMost(560),
                    minOf(
                        when (heightMode) {
                            StudyHeightMode.COMFORTABLE -> 220
                            StudyHeightMode.COMPACT_HEIGHT -> 160
                            StudyHeightMode.MINIMUM_HEIGHT -> 120
                        },
                        verticalImageBudgetDp
                    )
                )
            }
            viewportClass == StudyViewportClass.STANDARD -> {
                Pair(620, minOf(if (heightMode == StudyHeightMode.COMFORTABLE) 200 else 150, verticalImageBudgetDp))
            }
            else -> {
                Pair(620, minOf(if (heightMode == StudyHeightMode.COMFORTABLE) 200 else 150, verticalImageBudgetDp))
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
            heightMode = heightMode,
            contentMaxWidthDp = contentMaxWidthDp,
            imageMaxWidthDp = imageMaxWidthDp,
            imageMaxHeightDp = imageMaxHeightDp,
            identityWordFontSizeSp = identityFontSizeSp,
            identityWordLineHeightSp = identityLineHeightSp,
            metadataArrangement = metadataArrangement,
            ratingArrangement = ratingArrangement,
            sectionSpacingDp = sectionSpacingDp,
            ratingDockReservedHeightDp = ratingDockReservedHeightDp,
            statisticsDashboardReservedHeightDp = statisticsDashboardReservedHeightDp,
            headerReservedHeightDp = headerReservedHeightDp,
            availableAnswerHeightDp = availableAnswerHeightDp,
            preserveRatingReachability = true
        )
    }
}
