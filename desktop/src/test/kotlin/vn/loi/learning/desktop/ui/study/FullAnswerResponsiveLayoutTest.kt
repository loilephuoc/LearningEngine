package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertTrue(source.contains("remember(policy.layout)"))
        assertTrue(source.contains("if (disclosureState.expanded && examples.isNotEmpty())"))
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
    fun `typing comparison is passed into the shared responsive answer surface`() {
        val screen = studySource("StudyScreen.kt")

        assertTrue(screen.contains("typingComparison = typingComparison"))
        assertTrue(screen.contains("@Composable { TypingRevealComparison(presentation) }"))
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
