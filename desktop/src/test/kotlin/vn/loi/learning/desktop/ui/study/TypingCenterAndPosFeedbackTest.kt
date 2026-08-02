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
    fun `success overlay renders typed IPA before the existing POS badge`() {
        val overlay = section("private fun TypingSuccessFocusOverlay(", "private fun ReviewRating.toColorRole")

        assertFalse(overlay.contains("successMessage"))
        assertFalse(overlay.contains("typingCorrectSuccess"))
        assertTrue(overlay.contains("partOfSpeech: String?"))
        assertTrue(overlay.contains("ipa: String?"))
        assertTrue(overlay.contains("translation: String?"))
        assertTrue(overlay.contains("visibleTranslation?.let"))
        assertTrue(overlay.indexOf("text = canonicalAnswer") < overlay.indexOf("text = translatedAnswer"))
        assertTrue(overlay.indexOf("text = translatedAnswer") < overlay.indexOf("PopupLexicalMetadataRow("))
        assertTrue(overlay.contains("Arrangement.spacedBy(LETheme.spacing.space2)"))
        assertTrue(overlay.contains("color = LETheme.colors.textSecondary"))
        assertTrue(overlay.indexOf("presentation.ipa?.let") < overlay.indexOf("StudyPosBadge("))
        assertTrue(overlay.contains("PopupLexicalMetadataRow("))
        assertTrue(overlay.contains("resolvePartOfSpeechPresentation(partOfSpeech"))
        assertTrue(overlay.contains("StudyPosBadge("))
        assertTrue(overlay.contains("posPresentation?.canonicalLabel"))
        assertTrue(overlay.contains("Modifier.widthIn(max = 360.dp)"))
    }

    @Test
    fun `lexical metadata row is centered bounded and keeps POS outside weighted IPA`() {
        val row = section("private fun PopupLexicalMetadataRow(", "private fun ReviewRating.toColorRole")

        assertTrue(row.contains("if (!showIpa && !showPos) return"))
        assertTrue(row.contains("Arrangement.spacedBy(LETheme.spacing.space3, Alignment.CenterHorizontally)"))
        assertTrue(row.contains("verticalAlignment = Alignment.CenterVertically"))
        assertTrue(row.contains("clearAndSetSemantics { }"))
        assertTrue(row.contains("Modifier.weight(1f, fill = false)"))
        assertTrue(row.contains("viewportClass == StudyViewportClass.COMPACT"))
        assertTrue(row.indexOf("presentation.ipa?.let") < row.indexOf("StudyPosBadge("))
    }

    @Test
    fun `success accessibility reads answer IPA POS transition and explanation in order`() {
        val overlay = section("private fun TypingSuccessFocusOverlay(", "private fun PopupLexicalMetadataRow(")
        val success = overlay.indexOf("typingSuccessAccessibility")
        val answer = overlay.indexOf("\$canonicalAnswer. ")
        val translation = overlay.indexOf("visibleTranslation?.let")
        val ipa = overlay.indexOf("typingPronunciationAccessibility")
        val pos = overlay.indexOf("typingPartOfSpeechAccessibility")
        val transition = overlay.indexOf("typingRatingTransitionAccessibility")
        val explanation = overlay.indexOf("explanation?.let")

        assertTrue(success < answer)
        assertTrue(answer < translation)
        assertTrue(translation < ipa)
        assertTrue(ipa < pos)
        assertTrue(pos < transition)
        assertTrue(transition < explanation)

        val localization = Files.readString(
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/localization/DesktopStrings.kt")
        )
        assertTrue(localization.contains("typingPronunciationAccessibility = { ipa -> \"Phát âm: \$ipa\" }"))
        assertTrue(localization.contains("typingPartOfSpeechAccessibility = { partOfSpeech -> \"Từ loại: \$partOfSpeech\" }"))
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
