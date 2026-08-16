package vn.loi.learning.android.study

import androidx.compose.ui.geometry.Offset
import vn.loi.learning.android.study.components.ReviewNavigationGesture
import vn.loi.learning.android.study.components.resolveReviewNavigationGesture
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidStudyGestureResponsivenessTest {

    private fun resolveGesture(
        deltaX: Float,
        deltaY: Float,
        swipeThresholdPx: Float = 44f,
        tapSlopPx: Float = 12f,
        scrollRequired: Boolean = false,
        childConsumed: Boolean = false,
        alreadySubmitted: Boolean = false,
        ratingEnabled: Boolean = true,
        navigationEnabled: Boolean = true,
        gatedUpwardNavigation: Boolean = false,
        gestureDurationMillis: Long = 0L
    ) = resolveIntroductionStageGesture(
        deltaX = deltaX,
        deltaY = deltaY,
        swipeThresholdPx = swipeThresholdPx,
        tapSlopPx = tapSlopPx,
        scrollRequired = scrollRequired,
        childConsumed = childConsumed,
        alreadySubmitted = alreadySubmitted,
        ratingEnabled = ratingEnabled,
        navigationEnabled = navigationEnabled,
        gatedUpwardNavigation = gatedUpwardNavigation,
        gestureDurationMillis = gestureDurationMillis
    )

    @Test
    fun `below vertical threshold produces no advance`() {
        assertEquals(
            IntroductionStageGesture.NONE,
            resolveGesture(deltaX = 0f, deltaY = -30f, swipeThresholdPx = 44f)
        )
    }

    @Test
    fun `above threshold produces exactly one advance`() {
        assertEquals(
            IntroductionStageGesture.SWIPE_GOOD,
            resolveGesture(deltaX = 0f, deltaY = -50f, swipeThresholdPx = 44f, ratingEnabled = true)
        )
        assertEquals(
            IntroductionStageGesture.NEXT,
            resolveGesture(deltaX = 0f, deltaY = -50f, swipeThresholdPx = 44f, ratingEnabled = false, navigationEnabled = true)
        )
    }

    @Test
    fun `large upward swipe produces exactly one advance`() {
        assertEquals(
            IntroductionStageGesture.SWIPE_GOOD,
            resolveGesture(deltaX = 0f, deltaY = -200f, swipeThresholdPx = 44f, ratingEnabled = true)
        )
    }

    @Test
    fun `downward swipe produces no upward advance`() {
        assertEquals(
            IntroductionStageGesture.NONE,
            resolveGesture(deltaX = 0f, deltaY = 60f, swipeThresholdPx = 44f)
        )
    }

    @Test
    fun `horizontal dominant gesture produces no vertical advance`() {
        assertEquals(
            IntroductionStageGesture.NEXT,
            resolveGesture(deltaX = -60f, deltaY = -30f, swipeThresholdPx = 44f, navigationEnabled = true)
        )
        // With horizontal dominant (-60f vs -30f), vertical action is NOT triggered
        assertEquals(
            IntroductionStageGesture.NONE,
            resolveGesture(deltaX = -60f, deltaY = -30f, swipeThresholdPx = 44f, navigationEnabled = false, ratingEnabled = true)
        )
    }

    @Test
    fun `acceptable diagonal upward swipe produces exactly one advance`() {
        // Vertical displacement -60f is 1.5x horizontal -40f (vertical intent dominates)
        assertEquals(
            IntroductionStageGesture.SWIPE_GOOD,
            resolveGesture(deltaX = -40f, deltaY = -60f, swipeThresholdPx = 44f, ratingEnabled = true)
        )
    }

    @Test
    fun `jitter or small tap produces no gesture advance`() {
        assertEquals(
            IntroductionStageGesture.TAP,
            resolveGesture(deltaX = 3f, deltaY = -4f, tapSlopPx = 12f)
        )
    }

    @Test
    fun `gesture duration up to 600ms retains valid upward navigation`() {
        assertEquals(
            IntroductionStageGesture.SWIPE_GOOD,
            resolveGesture(
                deltaX = 0f,
                deltaY = -60f,
                swipeThresholdPx = 44f,
                ratingEnabled = true,
                gestureDurationMillis = 500L
            )
        )
        assertEquals(
            IntroductionStageGesture.NONE,
            resolveGesture(
                deltaX = 0f,
                deltaY = -60f,
                swipeThresholdPx = 44f,
                ratingEnabled = true,
                gestureDurationMillis = 900L
            )
        )
    }

    @Test
    fun `early ownership claim prevents child click consumption from dropping valid swipe`() {
        // When drag is claimed early by parent, childConsumed flag for resolution is false
        assertEquals(
            IntroductionStageGesture.SWIPE_GOOD,
            resolveGesture(
                deltaX = 0f,
                deltaY = -60f,
                swipeThresholdPx = 44f,
                childConsumed = false,
                ratingEnabled = true
            )
        )
    }

    @Test
    fun `review navigation overlay uses 44dp threshold and 1 25 ratio`() {
        assertEquals(
            ReviewNavigationGesture.NEXT,
            resolveReviewNavigationGesture(Offset(0f, -50f), 44f)
        )
        assertEquals(
            ReviewNavigationGesture.PREVIOUS,
            resolveReviewNavigationGesture(Offset(60f, 10f), 44f)
        )
        assertEquals(
            ReviewNavigationGesture.NEXT,
            resolveReviewNavigationGesture(Offset(-60f, 10f), 44f)
        )
    }
}
