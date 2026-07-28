package vn.loi.learning.desktop.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.partofspeech.PartOfSpeechSemanticRegistry

class LEPartOfSpeechTokensTest {
    @Test
    fun `known POS families remain distinct in light and dark themes`() {
        listOf(false, true).forEach { dark ->
            val tokens = createLEPartOfSpeechTokens(dark)
            val noun = tokens.resolve(LEPosColorFamily.BLUE, 0)
            val verb = tokens.resolve(LEPosColorFamily.GREEN, 1)
            val adjective = tokens.resolve(LEPosColorFamily.PURPLE, 2)
            assertNotEquals(noun, verb)
            assertNotEquals(verb, adjective)
            assertNotEquals(noun, adjective)
        }
    }

    @Test
    fun `all known styles provide readable light and dark content contrast`() {
        listOf(false, true).forEach { dark ->
            val tokens = createLEPartOfSpeechTokens(dark)
            LEPosColorFamily.entries.filterNot { it == LEPosColorFamily.DYNAMIC }.forEach { family ->
                val resolved = tokens.resolve(family, family.ordinal)
                assertTrue(
                    contrastRatio(resolved.contentColor, resolved.containerColor) >= 4.5,
                    "$family dark=$dark"
                )
                assertNotEquals(resolved.borderColor, resolved.containerColor, family.name)
            }
        }
    }

    @Test
    fun `unknown style is deterministic readable and theme aware`() {
        val registry = PartOfSpeechSemanticRegistry()
        val identity = registry.resolve("technical term")!!
        val recreatedIdentity = PartOfSpeechSemanticRegistry().resolve("TECHNICAL TERM")!!
        val light = createLEPartOfSpeechTokens(false)
            .resolve(LEPosColorFamily.DYNAMIC, identity.colorKey.visualSlot)
        val recreated = createLEPartOfSpeechTokens(false)
            .resolve(LEPosColorFamily.DYNAMIC, recreatedIdentity.colorKey.visualSlot)
        val dark = createLEPartOfSpeechTokens(true)
            .resolve(LEPosColorFamily.DYNAMIC, identity.colorKey.visualSlot)
        assertEquals(identity, recreatedIdentity)
        assertEquals(light, recreated)
        assertNotEquals(light, dark)
        assertTrue(contrastRatio(light.contentColor, light.containerColor) >= 4.5)
        assertTrue(contrastRatio(dark.contentColor, dark.containerColor) >= 4.5)
    }

    @Test
    fun `Study POS presentation delegates to one theme resolver without raw color authority`() {
        val answer = studySource("FocusedAnswerSurface.kt")
        val presentation = studySource("StudyVisualThemePresentation.kt")
        assertTrue(answer.contains("resolvePartOfSpeechPresentation(partOfSpeech, LETheme.partOfSpeech)"))
        assertTrue(answer.contains("text = resolved.canonicalLabel"))
        assertTrue(answer.contains("StudyMeaningPosGroup(partOfSpeech = partOfSpeech)"))
        assertFalse(answer.contains("when (partOfSpeech)"))
        assertFalse(answer.contains("Color(0x"))
        assertFalse(answer.contains("MaterialTheme.colorScheme"))
        assertFalse(presentation.contains("resolveStudyPosBadgeStyle"))
    }

    @Test
    fun `composition root reconciles once and import registers parsed content outside Compose`() {
        val app = File("src/main/kotlin/vn/loi/learning/desktop/ui/App.kt").readText()
        val factory = rootSource("infrastructure/LearningApplicationFactory.kt")
        val importer = rootSource("application/contentpackaging/PackageImportService.kt")
        assertTrue(app.contains("ProvidePartOfSpeechRegistry(applicationContext.partOfSpeechRegistry)"))
        assertTrue(factory.contains("PartOfSpeechRegistryReconciler("))
        assertTrue(importer.contains("partOfSpeechRegistry?.register(importedContent.contents)"))
        assertFalse(studySource("FocusedAnswerSurface.kt").contains("contentRepository"))
        assertFalse(studySource("FocusedAnswerSurface.kt").contains("findAll()"))
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val lighter = max(first.luminance(), second.luminance()).toDouble()
        val darker = min(first.luminance(), second.luminance()).toDouble()
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun studySource(name: String): String =
        File("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name").readText()

    private fun rootSource(relative: String): String {
        val fromDesktop = File("../src/main/kotlin/vn/loi/learning/$relative")
        return if (fromDesktop.isFile) fromDesktop.readText()
        else File("src/main/kotlin/vn/loi/learning/$relative").readText()
    }
}
