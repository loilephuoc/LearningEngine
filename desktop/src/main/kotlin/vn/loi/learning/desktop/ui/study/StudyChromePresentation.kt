package vn.loi.learning.desktop.ui.study

enum class StudyTopActionComposition {
    STANDARD_TEXT,
    COMPACT_ICON
}

enum class StudyShortcutStripComposition {
    STANDARD,
    COMPACT,
    MINIMUM
}

data class StudyChromePresentation(
    val topActionComposition: StudyTopActionComposition,
    val shortcutStripComposition: StudyShortcutStripComposition,
    val showShortcutLabels: Boolean,
    val showSessionStatusText: Boolean,
    val showStudyTitle: Boolean,
    val topActionButtonSizeDp: Int,
    val shortcutStripHeightDp: Int,
    val horizontalGapDp: Int,
    val horizontalPaddingDp: Int,
    val maximumShortcutItems: Int
)

internal fun resolveStudyChromePresentation(usableWidthDp: Int): StudyChromePresentation =
    when {
        usableWidthDp >= STANDARD_STUDY_CHROME_WIDTH_DP ->
            StudyChromePresentation(
                topActionComposition = StudyTopActionComposition.STANDARD_TEXT,
                shortcutStripComposition = StudyShortcutStripComposition.STANDARD,
                showShortcutLabels = true,
                showSessionStatusText = true,
                showStudyTitle = true,
                topActionButtonSizeDp = 48,
                shortcutStripHeightDp = 36,
                horizontalGapDp = 8,
                horizontalPaddingDp = 16,
                maximumShortcutItems = Int.MAX_VALUE
            )
        usableWidthDp >= MINIMUM_STUDY_CHROME_WIDTH_DP ->
            StudyChromePresentation(
                topActionComposition = StudyTopActionComposition.COMPACT_ICON,
                shortcutStripComposition = StudyShortcutStripComposition.COMPACT,
                showShortcutLabels = false,
                showSessionStatusText = false,
                showStudyTitle = false,
                topActionButtonSizeDp = 40,
                shortcutStripHeightDp = 32,
                horizontalGapDp = 6,
                horizontalPaddingDp = 8,
                maximumShortcutItems = 6
            )
        else ->
            StudyChromePresentation(
                topActionComposition = StudyTopActionComposition.COMPACT_ICON,
                shortcutStripComposition = StudyShortcutStripComposition.MINIMUM,
                showShortcutLabels = false,
                showSessionStatusText = false,
                showStudyTitle = false,
                topActionButtonSizeDp = 36,
                shortcutStripHeightDp = 28,
                horizontalGapDp = 4,
                horizontalPaddingDp = 4,
                maximumShortcutItems = 3
            )
    }

internal const val STANDARD_STUDY_CHROME_WIDTH_DP = 760
internal const val MINIMUM_STUDY_CHROME_WIDTH_DP = 420

internal fun measuredStudyChromeHeightDp(
    topRowHeightDp: Int,
    statisticsHeightDp: Int,
    progressHeightDp: Int,
    sectionGapDp: Int,
    statusStripHeightDp: Int
): Int = topRowHeightDp + statisticsHeightDp + progressHeightDp + sectionGapDp + statusStripHeightDp
