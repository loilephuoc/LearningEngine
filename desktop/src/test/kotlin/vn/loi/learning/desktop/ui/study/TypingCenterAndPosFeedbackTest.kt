package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.partofspeech.PartOfSpeechSemanticRegistry

class TypingCenterAndPosFeedbackTest {
    @Test
    fun `typing text cursor and submit share viewport independent vertical center`() {
        val input = section("private fun CenteredTypingField(", "private fun TypingSuccessFocusOverlay(")

        assertTrue(input.contains("BasicTextField("))
        assertTrue(input.contains("TypingFieldMeasuredLayout("))
        assertTrue(input.contains("horizontalInset = LETheme.spacing.space4"))
        assertTrue(input.contains("resolvedLineBoxMinimumHeightDp"))
        assertTrue(input.contains("wrapContentHeight(Alignment.CenterVertically)"))
        assertTrue(input.contains("contentAlignment = Alignment.Center"))
        assertTrue(input.contains("cursorBrush = SolidColor(LETheme.colors.accentPrimary)"))
        assertTrue(input.contains("modifier = measuredModifier"))
        assertTrue(input.contains("fieldMetrics.outerMinimumHeightDp"))
        assertFalse(input.contains("typingMinimumHeightDp"))
        assertTrue(input.contains("IconButton("))
    }

    @Test
    fun `success overlay replaces success copy with typed POS badge`() {
        val overlay = section("private fun TypingSuccessFocusOverlay(", "private fun ReviewRating.toColorRole")

        assertFalse(overlay.contains("successMessage"))
        assertFalse(overlay.contains("typingCorrectSuccess"))
        assertTrue(overlay.contains("partOfSpeech: String?"))
        assertTrue(overlay.contains("resolvePartOfSpeechPresentation(partOfSpeech"))
        assertTrue(overlay.contains("StudyPosBadge("))
        assertTrue(overlay.contains("posPresentation?.canonicalLabel"))
        assertTrue(overlay.contains("Modifier.widthIn(max = 360.dp)"))
    }

    @Test
    fun `POS authority supports standard long and fallback identities`() {
        val registry = PartOfSpeechSemanticRegistry()
        val expected = mapOf(
            "noun" to "NOUN",
            "verb" to "VERB",
            "phrasal verb" to "PHRASAL VERB",
            "proper noun" to "PROPER NOUN"
        )
        expected.forEach { (raw, canonical) ->
            assertEquals(canonical, registry.resolve(raw)?.canonical?.value)
        }
        assertNotNull(registry.resolve("DOMAIN SPECIFIC POS"))
    }

    private fun section(start: String, end: String): String =
        Files.readString(Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt"))
            .substringAfter(start)
            .substringBefore(end)
}
