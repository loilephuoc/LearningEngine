package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.shortcut.DesktopKeyChord
import vn.loi.learning.desktop.shortcut.DesktopShortcutKey

class FullAnswerResponsiveLayoutTest {
    @Test
    fun `available content width resolves deterministic narrow medium and wide policy`() {
        assertEquals(
            AnswerSurfaceLayout.NARROW,
            FullAnswerResponsivePolicyResolver.resolve(599).layout
        )
        assertEquals(
            AnswerSurfaceLayout.MEDIUM,
            FullAnswerResponsivePolicyResolver.resolve(600).layout
        )
        assertEquals(
            AnswerSurfaceLayout.MEDIUM,
            FullAnswerResponsivePolicyResolver.resolve(899).layout
        )
        assertEquals(
            AnswerSurfaceLayout.WIDE,
            FullAnswerResponsivePolicyResolver.resolve(900).layout
        )
    }

    @Test
    fun `wide gives examples more horizontal weight than translation`() {
        val policy = FullAnswerResponsivePolicyResolver.resolve(1040)

        assertTrue(policy.examplesWeight > policy.translationWeight)
        assertEquals(0.62f, policy.examplesWeight)
        assertEquals(0.38f, policy.translationWeight)
    }

    @Test
    fun `examples default collapsed only at narrow width`() {
        assertFalse(FullAnswerResponsivePolicyResolver.resolve(480).examplesInitiallyExpanded)
        assertTrue(FullAnswerResponsivePolicyResolver.resolve(680).examplesInitiallyExpanded)
        assertTrue(FullAnswerResponsivePolicyResolver.resolve(1040).examplesInitiallyExpanded)
    }

    @Test
    fun `narrow examples disclosure toggles open and closed without persistence`() {
        val policy = FullAnswerResponsivePolicyResolver.resolve(480)
        val collapsed = initialExamplesDisclosureState(policy)
        val expanded = toggleExamplesDisclosure(collapsed)

        assertFalse(collapsed.expanded)
        assertTrue(expanded.expanded)
        assertEquals(collapsed, toggleExamplesDisclosure(expanded))
    }

    @Test
    fun `E toggles both directions and Escape consumes while collapsing only expanded state`() {
        val collapsed = ExamplesDisclosureState(expanded = false)
        val expanded =
            applyExamplesDisclosureCommand(collapsed, ExamplesDisclosureCommand.TOGGLE)
        val toggledClosed =
            applyExamplesDisclosureCommand(expanded.state, ExamplesDisclosureCommand.TOGGLE)
        val escapedExpanded =
            applyExamplesDisclosureCommand(expanded.state, ExamplesDisclosureCommand.COLLAPSE)
        val escapedCollapsed =
            applyExamplesDisclosureCommand(collapsed, ExamplesDisclosureCommand.COLLAPSE)

        assertTrue(expanded.state.expanded)
        assertFalse(toggledClosed.state.expanded)
        assertFalse(escapedExpanded.state.expanded)
        assertEquals(collapsed, escapedCollapsed.state)
        assertTrue(escapedCollapsed.consumed)
    }

    @Test
    fun `keyboard command protects editable input and ignores modified E`() {
        val plainE = DesktopKeyChord(DesktopShortcutKey.E)

        assertEquals(
            ExamplesDisclosureCommand.TOGGLE,
            resolveExamplesDisclosureKeyboardCommand(plainE, textInputFocused = false)
        )
        assertEquals(
            ExamplesDisclosureCommand.COLLAPSE,
            resolveExamplesDisclosureKeyboardCommand(
                DesktopKeyChord(DesktopShortcutKey.ESCAPE),
                textInputFocused = false
            )
        )
        assertNull(resolveExamplesDisclosureKeyboardCommand(plainE, textInputFocused = true))
        assertEquals(
            ExamplesDisclosureCommand.TOGGLE,
            resolveExamplesDisclosureKeyboardCommand(
                DesktopKeyChord(DesktopShortcutKey.E, shiftPressed = true),
                textInputFocused = false
            )
        )
        assertNull(
            resolveExamplesDisclosureKeyboardCommand(
                DesktopKeyChord(DesktopShortcutKey.E, controlPressed = true),
                textInputFocused = false
            )
        )
    }

    @Test
    fun `controller dispatches only while a narrow disclosure is bound`() {
        val controller = ExamplesDisclosureKeyboardController()
        var dispatchCount = 0

        assertFalse(controller.dispatch(ExamplesDisclosureCommand.TOGGLE))
        controller.bind {
            dispatchCount++
            true
        }
        assertTrue(controller.dispatch(ExamplesDisclosureCommand.TOGGLE))
        assertEquals(1, dispatchCount)
        controller.unbind()
        assertFalse(controller.dispatch(ExamplesDisclosureCommand.COLLAPSE))
    }

    @Test
    fun `item identity change and Undo identity return reset narrow disclosure`() {
        val policy = FullAnswerResponsivePolicyResolver.resolve(480)
        val itemA =
            initialItemExamplesDisclosureState("item-a", policy)
                .copy(disclosure = ExamplesDisclosureState(expanded = true))
        val itemB = resetExamplesDisclosureForItem(itemA, "item-b", policy)
        val undoneA = resetExamplesDisclosureForItem(itemB, "item-a", policy)

        assertTrue(itemA.disclosure.expanded)
        assertFalse(itemB.disclosure.expanded)
        assertFalse(undoneA.disclosure.expanded)
    }

    @Test
    fun `short translation has compact single line target while long text remains content driven`() {
        val layout = CompactMeaningLayout()

        assertEquals(52, layout.estimatedSingleLineHeightDp)
        assertTrue(layout.estimatedSingleLineHeightDp <= 60)
        assertEquals(28, layout.textLineHeightSp)
    }

    @Test
    fun `production consumes measured width and keeps responsive behavior in presentation`() {
        val source = studySource("FocusedAnswerSurface.kt")

        assertTrue(source.contains("val availableContentWidthDp = maxWidth.value.toInt()"))
        assertTrue(source.contains("AnswerSurfaceLayout.WIDE"))
        assertTrue(source.contains("AnswerSurfaceLayout.MEDIUM"))
        assertTrue(source.contains("AnswerSurfaceLayout.NARROW"))
        assertTrue(source.contains("Modifier.weight(policy.translationWeight)"))
        assertTrue(source.contains("Modifier.weight(policy.examplesWeight)"))
        assertTrue(source.contains("remember(currentLearningItemId, policy.layout)"))
        assertTrue(source.contains("if (itemDisclosureState.disclosure.expanded && examples.isNotEmpty())"))
        assertTrue(source.contains("contentScale = ContentScale.Fit"))
    }

    @Test
    fun `translation is compact and does not compose part of speech`() {
        val source = studySource("FocusedAnswerSurface.kt")
        val meaningStart = source.indexOf("fun MeaningCard(")
        val exampleStart = source.indexOf("fun ExampleCard(")
        val meaningSource = source.substring(meaningStart, exampleStart)

        assertTrue(meaningSource.contains("CompactMeaningLayout()"))
        assertFalse(meaningSource.contains("StudyMeaningPosGroup"))
        assertFalse(meaningSource.contains("partOfSpeech"))
        assertFalse(meaningSource.contains("maxLines"))
        assertFalse(meaningSource.contains("TextOverflow"))
    }

    @Test
    fun `typing comparison is passed as presentation into the canonical answer header`() {
        val screen = studySource("StudyScreen.kt")
        val answer = studySource("FocusedAnswerSurface.kt")

        assertTrue(screen.contains("typingComparison = typingComparisonPresentation"))
        assertFalse(screen.contains("@Composable { TypingRevealComparison(presentation) }"))
        assertTrue(answer.contains("typingComparisonForCanonicalWord(typingComparison, word)"))
        val header =
            answer.substring(
                answer.indexOf("fun VocabularyIdentitySurface("),
                answer.indexOf("fun InlinePronunciationRow(")
            )
        assertFalse(header.contains("typingComparison?.invoke()"))
    }

    @Test
    fun `tooltip accessibility and focused Space consume one disclosure action`() {
        val source = studySource("FocusedAnswerSurface.kt")

        assertTrue(source.contains("TooltipBox("))
        assertTrue(source.contains("collapsedTooltip = strings.examplesOpenTooltip"))
        assertTrue(source.contains("expandedTooltip = strings.examplesCloseTooltip"))
        assertTrue(source.contains("contentDescription = \"\$label. \$tooltip\""))
        assertTrue(source.contains(".onPreviewKeyEvent { event ->"))
        assertTrue(source.contains("event.type == KeyEventType.KeyDown -> true"))
        assertTrue(source.contains("role = Role.Button"))
        assertTrue(source.contains("collectIsFocusedAsState()"))
        assertTrue(source.contains("if (focused) tooltipState.show() else tooltipState.dismiss()"))
        assertTrue(source.contains("LETheme.colors.borderFocus"))
    }

    @Test
    fun `Study root prioritizes disclosure before unchanged rating and audio resolver`() {
        val screen = studySource("StudyScreen.kt")

        val disclosureDispatch = screen.indexOf("examplesDisclosureKeyboard.dispatch")
        val studyResolver = screen.indexOf("resolveStudyKeyboardAction(")
        assertTrue(disclosureDispatch >= 0)
        assertTrue(studyResolver > disclosureDispatch)
        assertTrue(screen.contains("textInputFocused = typingInputFocused"))
    }

    private fun studySource(name: String): String = studySourceDirectory().resolve(name).readText()

    private fun studySourceDirectory(): File {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study")
        return if (fromRoot.isDirectory) {
            fromRoot
        } else {
            File("src/main/kotlin/vn/loi/learning/desktop/ui/study")
        }
    }
}
