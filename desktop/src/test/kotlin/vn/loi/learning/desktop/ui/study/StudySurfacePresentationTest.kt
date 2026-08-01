package vn.loi.learning.desktop.ui.study

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.theme.DarkLEColors
import vn.loi.learning.desktop.ui.theme.LightLEColors
import vn.loi.learning.desktop.ui.designsystem.components.base.resolveSurfaceStyle

class StudySurfacePresentationTest {
    @Test
    fun `front roles form one deterministic discovery hierarchy`() {
        val hero = front(StudySurfaceRole.HERO)
        val meaning = front(StudySurfaceRole.PRIMARY_SUPPORT)
        val utility = front(StudySurfaceRole.UTILITY)
        val action = front(StudySurfaceRole.ACTION)

        assertEquals(StudySurfaceLayer.HERO_CONTENT, hero.layer)
        assertTrue(hero.hierarchyWeight > meaning.hierarchyWeight)
        assertTrue(meaning.hierarchyWeight > action.hierarchyWeight)
        assertTrue(action.hierarchyWeight > utility.hierarchyWeight)
        assertFailsWith<IllegalArgumentException> { front(StudySurfaceRole.HERO_SUPPORT) }
    }

    @Test
    fun `answer roles form one deterministic understanding hierarchy`() {
        val word = answer(StudySurfaceRole.HERO)
        val image = answer(StudySurfaceRole.HERO_SUPPORT)
        val meaning = answer(StudySurfaceRole.SECONDARY_PRIMARY)
        val examples = answer(StudySurfaceRole.SUPPORTING)
        val scheduler = answer(StudySurfaceRole.EXPLANATORY)
        val rating = answer(StudySurfaceRole.ACTION)

        assertEquals(listOf(800, 700, 600, 500, 400, 300), listOf(word, image, meaning, rating, examples, scheduler).map { it.hierarchyWeight })
        assertEquals(StudySurfaceLayer.ACTION, rating.layer)
    }

    @Test
    fun `light and dark themes preserve semantic role order`() {
        listOf(LightLEColors, DarkLEColors).forEach { colors ->
            val hero = resolveSurfaceStyle(colors, answer(StudySurfaceRole.HERO).surfaceVariant)
            val meaning = resolveSurfaceStyle(colors, answer(StudySurfaceRole.SECONDARY_PRIMARY).surfaceVariant)
            val examples = resolveSurfaceStyle(colors, answer(StudySurfaceRole.SUPPORTING).surfaceVariant)
            assertFalse(hero.containerColor == hero.contentColor)
            assertFalse(meaning.containerColor == meaning.contentColor)
            assertFalse(examples.containerColor == examples.contentColor)
        }
    }

    @Test
    fun `responsive classes do not alter semantic hierarchy`() {
        val expected = StudyViewportClass.values().map { answer(StudySurfaceRole.HERO) }
        assertTrue(expected.all { it == expected.first() })
    }

    @Test
    fun `unified discovery stage preserves hero meaning action grouping`() {
        val stage = UnifiedStudyStageResolver.resolve(StudySurfaceStage.DISCOVERY)
        assertEquals(
            listOf(
                StudySurfaceRole.HERO,
                StudySurfaceRole.PRIMARY_SUPPORT,
                StudySurfaceRole.ACTION
            ),
            stage.orderedRoles
        )
        assertEquals(stage.orderedRoles.dropLast(1).toSet(), stage.integratedRoles)
        assertEquals(StudySurfaceRole.ACTION, stage.actionRole)
    }

    @Test
    fun `unified understanding stage preserves learning explanation decision grouping`() {
        val stage = UnifiedStudyStageResolver.resolve(StudySurfaceStage.UNDERSTANDING)
        assertEquals(
            listOf(
                StudySurfaceRole.HERO,
                StudySurfaceRole.HERO_SUPPORT,
                StudySurfaceRole.SECONDARY_PRIMARY,
                StudySurfaceRole.SUPPORTING,
                StudySurfaceRole.EXPLANATORY,
                StudySurfaceRole.ACTION
            ),
            stage.orderedRoles
        )
        assertEquals(StudyBorderProminence.NONE, stage.borderProminence)
        assertEquals(StudyRestingElevation.FLAT, stage.restingElevation)
    }

    @Test
    fun `unified grouping is deterministic and viewport neutral`() {
        StudySurfaceStage.values().forEach { stage ->
            val first = UnifiedStudyStageResolver.resolve(stage)
            val acrossViewports = StudyViewportClass.values().map { UnifiedStudyStageResolver.resolve(stage) }
            assertTrue(acrossViewports.all { it == first })
        }
    }

    @Test
    fun `unified stage removes presentation-only nested card wrappers`() {
        val front = source("DiscoveryFrontSurface.kt")
        val answer = source("FocusedAnswerSurface.kt")
        val scheduler = source("CompactSchedulerFeedback.kt")
        val meaningStart = answer.indexOf("fun MeaningCard(")
        val exampleStart = answer.indexOf("fun ExampleCard(")
        val englishRowStart = answer.indexOf("fun EnglishExampleAudioRow(")
        assertFalse(front.contains("LESurface("))
        assertFalse(answer.substring(meaningStart, exampleStart).contains("LESurface("))
        assertFalse(answer.substring(exampleStart, englishRowStart).contains("LESurface("))
        assertFalse(scheduler.contains("LESurface("))
        assertTrue(source("StudyScreen.kt").contains("UnifiedStudyStageResolver.resolve"))
    }

    @Test
    fun `front and answer composables consume shared authority without display text identity`() {
        val front = source("DiscoveryFrontSurface.kt")
        val answer = source("FocusedAnswerSurface.kt")
        val resolver = source("StudySurfacePresentation.kt")
        assertTrue(front.contains("StudySurfacePresentationResolver.resolve"))
        assertTrue(answer.contains("StudySurfacePresentationResolver.resolve"))
        assertFalse(resolver.contains("Nghĩa tiếng Việt"))
        assertFalse(resolver.contains("English"))
        assertFalse(resolver.contains("Color(0x"))
        assertFalse(resolver.contains(".dp"))
    }

    @Test
    fun `image layout typography interaction and semantics authorities remain external`() {
        val answer = source("FocusedAnswerSurface.kt")
        val screen = source("StudyScreen.kt")
        val resolver = source("StudySurfacePresentation.kt")
        assertTrue(answer.contains("ContentScale.Fit"))
        assertTrue(answer.contains("imageMaxWidthDp"))
        assertTrue(answer.contains("imageMaxHeightDp"))
        assertTrue(answer.contains("LETheme.typography.displayWord"))
        assertTrue(screen.contains("performKeyboardAction"))
        assertTrue(screen.contains("ratingFeedback"))
        listOf("onKeyEvent", "semantics", "ReviewScheduler", "FSRS", "repository", "persistence")
            .forEach { assertFalse(resolver.contains(it), it) }
    }

    private fun front(role: StudySurfaceRole) =
        StudySurfacePresentationResolver.resolve(StudySurfaceStage.DISCOVERY, role)

    private fun answer(role: StudySurfaceRole) =
        StudySurfacePresentationResolver.resolve(StudySurfaceStage.UNDERSTANDING, role)

    private fun source(name: String): String {
        val fromRoot = File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
        return (if (fromRoot.isFile) fromRoot else File("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")).readText()
    }
}
