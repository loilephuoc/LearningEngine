package vn.loi.learning.desktop.ui.study

import vn.loi.learning.desktop.shortcut.DesktopKeyChord
import vn.loi.learning.desktop.shortcut.DesktopShortcutKey

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

data class ItemExamplesDisclosureState(
    val itemId: String?,
    val disclosure: ExamplesDisclosureState
)

fun initialItemExamplesDisclosureState(
    itemId: String?,
    policy: FullAnswerResponsivePolicy
): ItemExamplesDisclosureState =
    ItemExamplesDisclosureState(
        itemId = itemId,
        disclosure = initialExamplesDisclosureState(policy)
    )

fun resetExamplesDisclosureForItem(
    current: ItemExamplesDisclosureState,
    itemId: String?,
    policy: FullAnswerResponsivePolicy
): ItemExamplesDisclosureState =
    if (current.itemId == itemId) current else initialItemExamplesDisclosureState(itemId, policy)

fun toggleExamplesDisclosure(state: ExamplesDisclosureState): ExamplesDisclosureState =
    state.copy(expanded = !state.expanded)

enum class ExamplesDisclosureCommand {
    TOGGLE,
    COLLAPSE
}

data class ExamplesDisclosureCommandResult(
    val state: ExamplesDisclosureState,
    val consumed: Boolean
)

fun applyExamplesDisclosureCommand(
    state: ExamplesDisclosureState,
    command: ExamplesDisclosureCommand
): ExamplesDisclosureCommandResult =
    when (command) {
        ExamplesDisclosureCommand.TOGGLE ->
            ExamplesDisclosureCommandResult(toggleExamplesDisclosure(state), consumed = true)
        ExamplesDisclosureCommand.COLLAPSE ->
            ExamplesDisclosureCommandResult(
                state = state.copy(expanded = false),
                consumed = true
            )
    }

fun resolveExamplesDisclosureKeyboardCommand(
    chord: DesktopKeyChord,
    textInputFocused: Boolean
): ExamplesDisclosureCommand? {
    if (
        textInputFocused ||
        chord.controlPressed ||
        chord.altPressed
    ) {
        return null
    }
    return when (chord.key) {
        DesktopShortcutKey.E -> ExamplesDisclosureCommand.TOGGLE
        DesktopShortcutKey.ESCAPE ->
            ExamplesDisclosureCommand.COLLAPSE.takeUnless { chord.shiftPressed }
        else -> null
    }
}

class ExamplesDisclosureKeyboardController {
    private var handler: ((ExamplesDisclosureCommand) -> Boolean)? = null

    fun bind(handler: (ExamplesDisclosureCommand) -> Boolean) {
        this.handler = handler
    }

    fun unbind() {
        handler = null
    }

    fun dispatch(command: ExamplesDisclosureCommand): Boolean =
        handler?.invoke(command) ?: false
}
