package vn.loi.learning.desktop.ui.study

enum class StudyIdentityComposition {
    STANDARD_INLINE,
    COMPACT_INLINE,
    STACKED
}

data class StudyIdentityPresentation(
    val composition: StudyIdentityComposition,
    val horizontalGapDp: Int,
    val verticalGapDp: Int,
    val cardPaddingDp: Int,
    val speakerButtonSizeDp: Int,
    val speakerIconSizeDp: Int,
    val ipaFontSizeSp: Int,
    val posHorizontalPaddingDp: Int,
    val posVerticalPaddingDp: Int
)

internal fun resolveStudyIdentityPresentation(
    cardWidthDp: Int
): StudyIdentityPresentation = when {
    cardWidthDp >= COMFORTABLE_IDENTITY_CARD_WIDTH_DP ->
        StudyIdentityPresentation(
            composition = StudyIdentityComposition.STANDARD_INLINE,
            horizontalGapDp = 8,
            verticalGapDp = 4,
            cardPaddingDp = 4,
            speakerButtonSizeDp = 40,
            speakerIconSizeDp = 22,
            ipaFontSizeSp = 22,
            posHorizontalPaddingDp = 8,
            posVerticalPaddingDp = 4
        )
    cardWidthDp >= MINIMUM_INLINE_IDENTITY_CARD_WIDTH_DP ->
        StudyIdentityPresentation(
            composition = StudyIdentityComposition.COMPACT_INLINE,
            horizontalGapDp = 4,
            verticalGapDp = 2,
            cardPaddingDp = 2,
            speakerButtonSizeDp = 34,
            speakerIconSizeDp = 18,
            ipaFontSizeSp = 20,
            posHorizontalPaddingDp = 4,
            posVerticalPaddingDp = 2
        )
    else ->
        StudyIdentityPresentation(
            composition = StudyIdentityComposition.STACKED,
            horizontalGapDp = 4,
            verticalGapDp = 4,
            cardPaddingDp = 2,
            speakerButtonSizeDp = 34,
            speakerIconSizeDp = 18,
            ipaFontSizeSp = 20,
            posHorizontalPaddingDp = 4,
            posVerticalPaddingDp = 2
        )
}

internal const val COMFORTABLE_IDENTITY_CARD_WIDTH_DP = 480
internal const val MINIMUM_INLINE_IDENTITY_CARD_WIDTH_DP = 240

internal fun measuredStudyIdentityHeightDp(
    presentation: StudyIdentityPresentation,
    wordLineHeightDp: Int,
    inlineMetadataHeightDp: Int,
    stackedMetadataHeightsDp: List<Int>
): Int {
    val metadataHeight =
        if (presentation.composition == StudyIdentityComposition.STACKED) {
            stackedMetadataHeightsDp.sum() +
                presentation.verticalGapDp * (stackedMetadataHeightsDp.size - 1).coerceAtLeast(0)
        } else {
            inlineMetadataHeightDp
        }
    return presentation.cardPaddingDp * 2 +
        wordLineHeightDp +
        presentation.verticalGapDp +
        metadataHeight
}
