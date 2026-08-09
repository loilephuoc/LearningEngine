package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.*
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.ui.*

class AndroidStudyVisualHierarchyTest {

    @Test
    fun `touch targets meet minimum 48dp accessibility standard`() {
        assertEquals(48.dp, LearningSpacing.touchTarget)
    }

    @Test
    fun `typography hierarchy defines distinct emphasis levels`() {
        // Vocabulary prompt has strongest headline emphasis
        assertEquals(32, LearningContentTypography.vocabulary.fontSize.value.toInt())

        // Meaning is prominent title
        assertEquals(22, LearningContentTypography.meaning.fontSize.value.toInt())

        // Pronunciation and translation have secondary body emphasis
        assertEquals(14, LearningContentTypography.pronunciation.fontSize.value.toInt())
        assertEquals(14, LearningContentTypography.translation.fontSize.value.toInt())
    }

    @Test
    fun `design token shapes use consistent rounding`() {
        assertNotNull(LearningEngineShapes.medium)
        assertNotNull(LearningEngineShapes.large)
        assertEquals(1.dp, LearningElevation.card)
    }

    @Test
    fun `light and dark semantic colors provide valid tone mappings`() {
        val light = LearningEngineLightSemanticColors
        val dark = LearningEngineDarkSemanticColors

        assertNotEquals(light.success, dark.success)
        assertNotEquals(light.dueReview, dark.dueReview)
    }

    @Test
    fun `hero image size follows intrinsic landscape square and portrait aspect`() {
        val bounds = LearningImageFitBounds(minHeightDp = 120, maxHeightDp = 360)
        assertEquals(AspectAwareImageSize(360, 202), resolveAspectAwareImageSize(360, 16f / 9f, bounds))
        assertEquals(360, resolveAspectAwareImageSize(360, 1f, bounds).heightDp)
        assertEquals(180, resolveAspectAwareImageSize(360, 0.5f, bounds).widthDp)
        assertEquals(360, resolveAspectAwareImageSize(360, 0.5f, bounds).heightDp)
    }

    @Test
    fun `aspect-aware hero keeps fit decoding authority and content-driven canvas`() {
        val components = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/ui/LearningEngineComponents.kt")
        )
        val screen = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/study/StudyScreen.kt")
        )
        assertTrue(components.contains("presentation.bitmap.width.toFloat() / presentation.bitmap.height.toFloat()"))
        assertTrue(components.contains("contentScale = ContentScale.Fit"))
        assertFalse(components.contains("ContentScale.Crop"))
        val introduction = screen.substringAfter("private fun IntroductionLearningStage(")
            .substringBefore("private fun IntroductionAudioTextTarget(")
        assertFalse(introduction.contains("Arrangement.SpaceEvenly"))
        assertFalse(introduction.contains("Modifier.fillMaxWidth().heightIn(\n                    min ="))
    }
}
