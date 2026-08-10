package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.android.study.design.*

class StudyDesignPolicyTest {
    @Test fun `density resolves relaxed standard dense and IME`() {
        assertEquals(StudyContentDensity.RELAXED, resolveStudyContentDensity(StudyDensityInput(920, 420, true, false, 60)))
        assertEquals(StudyContentDensity.STANDARD, resolveStudyContentDensity(StudyDensityInput(760, 380, false, false, 120)))
        assertEquals(StudyContentDensity.DENSE, resolveStudyContentDensity(StudyDensityInput(580, 360, true, true, 220)))
        assertEquals(StudyContentDensity.DENSE, resolveStudyContentDensity(StudyDensityInput(900, 420, false, false, 40, imeVisible = true)))
    }

    @Test fun `media roles preserve hierarchy density minimum and no-media behavior`() {
        val relaxedHero = resolveStudyMediaBounds(StudyMediaRole.HERO, StudyContentDensity.RELAXED, 1000)!!
        val standard = resolveStudyMediaBounds(StudyMediaRole.STANDARD, StudyContentDensity.STANDARD, 1000)!!
        val supporting = resolveStudyMediaBounds(StudyMediaRole.SUPPORTING, StudyContentDensity.STANDARD, 1000)!!
        val denseHero = resolveStudyMediaBounds(StudyMediaRole.HERO, StudyContentDensity.DENSE, 520)!!
        assertTrue(relaxedHero.maxHeightDp > standard.maxHeightDp)
        assertTrue(standard.maxHeightDp > supporting.maxHeightDp)
        assertTrue(denseHero.maxHeightDp >= denseHero.minHeightDp)
        assertNull(resolveStudyMediaBounds(StudyMediaRole.HERO, StudyContentDensity.RELAXED, 900, hasMedia = false))
        assertEquals(StudyMediaRole.HERO, studyMediaRole("ImageRecall", revealed = false))
        assertEquals(StudyMediaRole.STANDARD, studyMediaRole("Introduction", revealed = true))
    }

    @Test fun `motion roles are ordered and reduced motion is zero`() {
        val quick = studyMotionDurationMillis(StudyMotionRole.PRESS, false)
        val standard = studyMotionDurationMillis(StudyMotionRole.CARD_ENTER, false)
        val emphasis = studyMotionDurationMillis(StudyMotionRole.FEEDBACK, false)
        assertTrue(quick < standard && standard < emphasis)
        StudyMotionRole.entries.forEach { assertEquals(0, studyMotionDurationMillis(it, true)) }
        assertTrue(StudyFeedbackVisualState.entries.containsAll(listOf(
            StudyFeedbackVisualState.CORRECT,
            StudyFeedbackVisualState.INCORRECT,
            StudyFeedbackVisualState.RATING_AGAIN,
            StudyFeedbackVisualState.RATING_HARD,
            StudyFeedbackVisualState.RATING_GOOD,
            StudyFeedbackVisualState.RATING_EASY,
            StudyFeedbackVisualState.AUDIO_ACTIVE
        )))
    }
}
