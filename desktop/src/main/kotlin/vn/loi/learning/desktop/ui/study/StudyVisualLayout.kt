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

enum class FullAnswerDensityClass {
    COMFORTABLE,
    COMPACT,
    MINIMUM
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
    val ratingButtonHeightDp: Int,
    val frontRatingSegmentHeightDp: Int,
    val ratingDockVerticalPaddingDp: Int,
    val topActionHeightDp: Int,
    val compactChrome: Boolean,
    val statisticsDashboardReservedHeightDp: Int,
    val headerReservedHeightDp: Int,
    val availableAnswerHeightDp: Int,
    val fullAnswerDensityClass: FullAnswerDensityClass,
    val fullAnswerImageMaxHeightDp: Int,
    val fullAnswerSectionGapDp: Int,
    val fullAnswerCardVerticalPaddingDp: Int,
    val fullAnswerExampleBudgetDp: Int,
    val commonAnswerFitsWithoutScroll: Boolean,
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
    const val MINIMUM_SUPPORTED_FULL_ANSWER_HEIGHT_DP = 720

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
        val compactChrome = heightMode != StudyHeightMode.COMFORTABLE
        val ratingButtonHeightDp = when (heightMode) {
            StudyHeightMode.COMFORTABLE -> 64
            StudyHeightMode.COMPACT_HEIGHT -> 56
            StudyHeightMode.MINIMUM_HEIGHT -> 52
        }
        val frontRatingSegmentHeightDp = when (heightMode) {
            StudyHeightMode.COMFORTABLE -> 48
            StudyHeightMode.COMPACT_HEIGHT -> 44
            StudyHeightMode.MINIMUM_HEIGHT -> 40
        }
        val ratingDockVerticalPaddingDp = when (heightMode) {
            StudyHeightMode.COMFORTABLE -> 4
            StudyHeightMode.COMPACT_HEIGHT -> 3
            StudyHeightMode.MINIMUM_HEIGHT -> 2
        }
        val topActionHeightDp = when (heightMode) {
            StudyHeightMode.COMFORTABLE -> 48
            StudyHeightMode.COMPACT_HEIGHT -> 40
            StudyHeightMode.MINIMUM_HEIGHT -> 36
        }
        val ratingDockReservedHeightDp =
            if (viewportWidthDp <= RATING_GRID_MAX_WIDTH_DP) {
                when (heightMode) {
                    StudyHeightMode.COMFORTABLE -> 144
                    StudyHeightMode.COMPACT_HEIGHT -> 128
                    StudyHeightMode.MINIMUM_HEIGHT -> 120
                }
            } else {
                when (heightMode) {
                    StudyHeightMode.COMFORTABLE -> 88
                    StudyHeightMode.COMPACT_HEIGHT -> 72
                    StudyHeightMode.MINIMUM_HEIGHT -> 64
                }
            }
        val statisticsDashboardReservedHeightDp =
            if (viewportClass == StudyViewportClass.COMPACT) 112 else 72
        val headerReservedHeightDp = statisticsDashboardReservedHeightDp +
            when (heightMode) {
                StudyHeightMode.COMFORTABLE -> 56
                StudyHeightMode.COMPACT_HEIGHT -> 48
                StudyHeightMode.MINIMUM_HEIGHT -> 40
            }
        val (footerReservedHeightDp, footerGapDp) = when (heightMode) {
            StudyHeightMode.COMFORTABLE -> 32 to 16
            StudyHeightMode.COMPACT_HEIGHT -> 28 to 8
            StudyHeightMode.MINIMUM_HEIGHT -> 24 to 6
        }
        val fixedChromeHeightDp =
            headerReservedHeightDp + ratingDockReservedHeightDp +
                footerReservedHeightDp + footerGapDp
        val fontScaleReserveDp = ((environment.fontScale - 1f).coerceAtLeast(0f) * 96).toInt()
        val nonImageAnswerHeightDp =
            when (heightMode) {
                StudyHeightMode.COMFORTABLE -> 80 + 88
                StudyHeightMode.COMPACT_HEIGHT -> 72 + 72
                StudyHeightMode.MINIMUM_HEIGHT -> 64 + 64
            } +
                (if (traits.hasExamples) {
                    when (heightMode) {
                        StudyHeightMode.COMFORTABLE -> 96
                        StudyHeightMode.COMPACT_HEIGHT -> 88
                        StudyHeightMode.MINIMUM_HEIGHT -> 80
                    }
                } else 0) +
                (if (traits.hasSchedulerFeedback) {
                    when (heightMode) {
                        StudyHeightMode.COMFORTABLE -> 60
                        StudyHeightMode.COMPACT_HEIGHT -> 48
                        StudyHeightMode.MINIMUM_HEIGHT -> 44
                    }
                } else 0) +
                when (heightMode) {
                    StudyHeightMode.COMFORTABLE -> 32
                    StudyHeightMode.COMPACT_HEIGHT -> 20
                    StudyHeightMode.MINIMUM_HEIGHT -> 16
                } +
                sectionSpacingDp * 4 +
                fontScaleReserveDp
        val availableAnswerHeightDp =
            (effectiveHeightDp - fixedChromeHeightDp).coerceAtLeast(0)
        val verticalImageBudgetDp =
            (availableAnswerHeightDp - nonImageAnswerHeightDp)
                .coerceAtLeast(120)

        val imageAvailableWidthDp =
            minOf(contentMaxWidthDp, (viewportWidthDp - 32).coerceAtLeast(1))
        val imageMaxWidthDp =
            if (traits.hasImage) {
                (imageAvailableWidthDp * IMAGE_CONTENT_WIDTH_FRACTION).toInt().coerceAtLeast(1)
            } else {
                0
            }
        val imageMaxHeightDp =
            if (traits.hasImage) verticalImageBudgetDp else 0

        val fullAnswerDensityClass = when {
            availableAnswerHeightDp >= 900 -> FullAnswerDensityClass.COMFORTABLE
            availableAnswerHeightDp >= 700 -> FullAnswerDensityClass.COMPACT
            else -> FullAnswerDensityClass.MINIMUM
        }
        val fullAnswerSectionGapDp = when (fullAnswerDensityClass) {
            FullAnswerDensityClass.COMFORTABLE -> 16
            FullAnswerDensityClass.COMPACT -> 8
            FullAnswerDensityClass.MINIMUM -> 6
        }
        val fullAnswerCardVerticalPaddingDp = when (fullAnswerDensityClass) {
            FullAnswerDensityClass.COMFORTABLE -> 12
            FullAnswerDensityClass.COMPACT -> 6
            FullAnswerDensityClass.MINIMUM -> 4
        }
        val fullAnswerExampleBudgetDp =
            if (traits.hasExamples) {
                when (fullAnswerDensityClass) {
                    FullAnswerDensityClass.COMFORTABLE -> 144
                    FullAnswerDensityClass.COMPACT -> 132
                    FullAnswerDensityClass.MINIMUM -> 120
                }
            } else {
                0
            }
        val fullAnswerIdentityBudgetDp = when (fullAnswerDensityClass) {
            FullAnswerDensityClass.COMFORTABLE -> 120
            FullAnswerDensityClass.COMPACT -> 104
            FullAnswerDensityClass.MINIMUM -> 92
        }
        val fullAnswerMeaningBudgetDp = when (fullAnswerDensityClass) {
            FullAnswerDensityClass.COMFORTABLE -> 96
            FullAnswerDensityClass.COMPACT -> 84
            FullAnswerDensityClass.MINIMUM -> 76
        }
        val fullAnswerSafeMarginsDp = when (fullAnswerDensityClass) {
            FullAnswerDensityClass.COMFORTABLE -> 72
            FullAnswerDensityClass.COMPACT -> 56
            FullAnswerDensityClass.MINIMUM -> 44
        }
        val densityRoundingReserveDp =
            kotlin.math.ceil(7f * environment.density).toInt()
                .let { pixels -> kotlin.math.ceil(pixels / environment.density).toInt() }
        val fullAnswerNonImageBudgetDp =
            fullAnswerSafeMarginsDp +
                fullAnswerIdentityBudgetDp +
                fullAnswerMeaningBudgetDp +
                fullAnswerExampleBudgetDp +
                fullAnswerCardVerticalPaddingDp * 2 +
                fullAnswerSectionGapDp * 3 +
                densityRoundingReserveDp +
                fontScaleReserveDp
        val fullAnswerRawImageBudgetDp =
            availableAnswerHeightDp - fullAnswerNonImageBudgetDp
        val fullAnswerImageMaxHeightDp =
            if (traits.hasImage) {
                fullAnswerRawImageBudgetDp.coerceAtLeast(MINIMUM_READABLE_IMAGE_HEIGHT_DP)
            } else {
                0
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
            ratingButtonHeightDp = ratingButtonHeightDp,
            frontRatingSegmentHeightDp = frontRatingSegmentHeightDp,
            ratingDockVerticalPaddingDp = ratingDockVerticalPaddingDp,
            topActionHeightDp = topActionHeightDp,
            compactChrome = compactChrome,
            statisticsDashboardReservedHeightDp = statisticsDashboardReservedHeightDp,
            headerReservedHeightDp = headerReservedHeightDp,
            availableAnswerHeightDp = availableAnswerHeightDp,
            fullAnswerDensityClass = fullAnswerDensityClass,
            fullAnswerImageMaxHeightDp = fullAnswerImageMaxHeightDp,
            fullAnswerSectionGapDp = fullAnswerSectionGapDp,
            fullAnswerCardVerticalPaddingDp = fullAnswerCardVerticalPaddingDp,
            fullAnswerExampleBudgetDp = fullAnswerExampleBudgetDp,
            commonAnswerFitsWithoutScroll =
                effectiveHeightDp >= MINIMUM_SUPPORTED_FULL_ANSWER_HEIGHT_DP &&
                    fullAnswerRawImageBudgetDp >=
                        if (traits.hasImage) MINIMUM_READABLE_IMAGE_HEIGHT_DP else 0,
            preserveRatingReachability = true
        )
    }

    private const val IMAGE_CONTENT_WIDTH_FRACTION = 0.9
    private const val MINIMUM_READABLE_IMAGE_HEIGHT_DP = 96
}
