package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypingViewportCompactionPresentationTest {
    @Test
    fun `typing suppresses stage badge while other scenes retain it`() {
        val screen = source("StudyScreen.kt")

        assertTrue(screen.contains("learningScene !is TypingScene"))
        assertTrue(screen.contains("LEStatusBadge("))
        assertTrue(screen.contains("contentPresentationStage ?: uiState.learningStage"))
    }

    @Test
    fun `existing POS authority remains singular and blank-safe`() {
        val renderer = source("LearningSceneRenderer.kt")

        assertTrue(renderer.contains("primary && !partOfSpeech.isNullOrBlank()"))
        assertTrue(renderer.contains("StudyPosBadge("))
        assertTrue(renderer.indexOf("StudyPosBadge(") == renderer.lastIndexOf("StudyPosBadge("))
    }

    @Test
    fun `typing meaning row gives translation remaining width and timer intrinsic end slot`() {
        val renderer = source("LearningSceneRenderer.kt")
        val screen = source("StudyScreen.kt")

        assertTrue(renderer.contains("typingMeaningTrailingContent"))
        assertTrue(renderer.contains("Modifier.weight(1f)"))
        assertTrue(renderer.contains("Alignment.CenterEnd"))
        assertTrue(screen.contains("typingMeaningTrailingContent ="))
        assertTrue(screen.contains("modifier = Modifier.wrapContentWidth()"))
    }

    @Test
    fun `ready timer keeps accessibility but has no visible ready caption`() {
        val panel = source("StudyScreen.kt")
            .substringAfter("private fun TypingAutoRatingTimerPanel(")
            .substringBefore("private fun typingDecisionExplanation")

        assertTrue(panel.contains("typingTimerReadyAccessibility"))
        assertTrue(panel.contains("preview.state != TypingRatingPreviewState.READY"))
        assertFalse(panel.contains("if (preview.state == TypingRatingPreviewState.READY) {\n                    workspaceStrings.typingTimerReady"))
    }

    @Test
    fun `compacted allocator reclaims standalone timer height for image`() {
        val compacted = StudyVerticalSpaceAllocationResolver.resolve(input(800))
        val legacyImageAllowance = compacted.imageMaxHeightDp - 32

        assertTrue(compacted.imageMaxHeightDp > legacyImageAllowance)
        assertFalse(source("StudyVerticalSpaceAllocation.kt").contains("32 + typingMin"))
    }

    @Test
    fun `typing renderer consumes measured body height through the single allocator`() {
        val screen = source("StudyScreen.kt")
        val renderer = source("LearningSceneRenderer.kt")

        assertTrue(screen.contains("typingAvailableHeightDp = fullAnswerAvailableBodyHeightDp"))
        assertTrue(renderer.contains("StudyVerticalSpaceAllocationResolver.resolve("))
        assertTrue(renderer.contains("viewportHeightDp = availableHeight.coerceAtLeast(1)"))
        assertTrue(renderer.contains("typingImageMaxHeightDp = typingAllocation?.imageMaxHeightDp"))
        assertTrue(renderer.contains("imageMaxHeightDp = typingImageMaxHeightDp ?: layout.imageMaxHeightDp"))
        assertTrue(renderer.contains("verticalArrangement = Arrangement.spacedBy(sceneSpacingDp.dp)"))
        assertTrue(renderer.contains("verticalArrangement = Arrangement.spacedBy(blockSpacingDp.dp)"))
        assertTrue(renderer.contains("typingAllocation?.verticalSpacingDp == 6"))
    }

    private fun input(height: Int) = StudyVerticalSpaceInput(
        viewportWidthDp = 900,
        viewportHeightDp = height,
        imageAspectClass = StudyImageAspectClass.STANDARD_LANDSCAPE,
        typingRequired = true,
        externalReservedHeightDp = 0,
        examplesExpanded = false
    )

    private fun source(name: String): String = Files.readString(
        listOf(
            Path.of("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/$name"),
            Path.of("src/main/kotlin/vn/loi/learning/desktop/ui/study/$name")
        ).first(Files::exists)
    )
}
