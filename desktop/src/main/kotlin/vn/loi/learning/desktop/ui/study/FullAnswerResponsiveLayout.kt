package vn.loi.learning.desktop.ui.study

enum class AnswerSurfaceLayout {
    WIDE,
    MEDIUM,
    NARROW
}

data class FullAnswerResponsivePolicy(
    val layout: AnswerSurfaceLayout,
    val translationWeight: Float,
    val examplesWeight: Float,
    val examplesInitiallyExpanded: Boolean
) {
    init {
        require(translationWeight > 0f)
        require(examplesWeight > translationWeight)
    }
}

object FullAnswerResponsivePolicyResolver {
    const val NARROW_MAX_CONTENT_WIDTH_DP = 599
    const val MEDIUM_MAX_CONTENT_WIDTH_DP = 899

    fun resolve(availableContentWidthDp: Int): FullAnswerResponsivePolicy {
        require(availableContentWidthDp > 0)
        return when {
            availableContentWidthDp <= NARROW_MAX_CONTENT_WIDTH_DP ->
                FullAnswerResponsivePolicy(
                    layout = AnswerSurfaceLayout.NARROW,
                    translationWeight = 0.38f,
                    examplesWeight = 0.62f,
                    examplesInitiallyExpanded = false
                )
            availableContentWidthDp <= MEDIUM_MAX_CONTENT_WIDTH_DP ->
                FullAnswerResponsivePolicy(
                    layout = AnswerSurfaceLayout.MEDIUM,
                    translationWeight = 0.38f,
                    examplesWeight = 0.62f,
                    examplesInitiallyExpanded = true
                )
            else ->
                FullAnswerResponsivePolicy(
                    layout = AnswerSurfaceLayout.WIDE,
                    translationWeight = 0.38f,
                    examplesWeight = 0.62f,
                    examplesInitiallyExpanded = true
                )
        }
    }
}

data class CompactMeaningLayout(
    val horizontalPaddingDp: Int = 14,
    val verticalPaddingDp: Int = 6,
    val iconSizeDp: Int = 40,
    val iconPaddingDp: Int = 9,
    val textSizeSp: Int = 22,
    val textLineHeightSp: Int = 28
) {
    val estimatedSingleLineHeightDp: Int
        get() = maxOf(iconSizeDp, textLineHeightSp) + verticalPaddingDp * 2
}

data class ExamplesDisclosureState(val expanded: Boolean)

fun initialExamplesDisclosureState(policy: FullAnswerResponsivePolicy): ExamplesDisclosureState =
    ExamplesDisclosureState(expanded = policy.examplesInitiallyExpanded)

fun toggleExamplesDisclosure(state: ExamplesDisclosureState): ExamplesDisclosureState =
    state.copy(expanded = !state.expanded)
