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
}
