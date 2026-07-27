package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode
import vn.loi.learning.desktop.runtime.StudyPresentationPreferences

class StudyPresentationStagingStateTest {
    private val adaptive = StudyPresentationPreferences()
    private val manual = StudyPresentationPreferences(
        controlMode = StudyPresentationControlMode.MANUAL,
        showEnglish = false,
        showVietnamese = true
    )

    @Test
    fun `persisted change stays pending for current item and applies on next item`() {
        val current = StudyPresentationStagingState("item-1", adaptive)
        val pending = current.reconcile("item-1", manual)

        assertEquals(adaptive, pending.active)
        assertEquals(manual, pending.pending)
        assertEquals(manual, pending.next)

        val advanced = pending.reconcile("item-2", manual)
        assertEquals(manual, advanced.active)
        assertNull(advanced.pending)
    }

    @Test
    fun `quick control and settings share the same persisted reconciliation path`() {
        val current = StudyPresentationStagingState("item-1", adaptive)
        val fromQuickControl = current.stage(manual)
        val fromSettings = current.reconcile("item-1", manual)

        assertEquals(fromQuickControl, fromSettings)
    }

    @Test
    fun `header reports pending mode and language state for next item`() {
        val status = resolveStudyPresentationHeaderStatus(
            StudyPresentationStagingState("item-1", adaptive).stage(manual)
        )

        assertEquals("Manual EN− VI+", status.label)
        assertTrue(status.pending)
    }

    @Test
    fun `adaptive header omits inactive language switches`() {
        val status = resolveStudyPresentationHeaderStatus(
            StudyPresentationStagingState("item-1", adaptive)
        )

        assertEquals("Adaptive", status.label)
        assertFalse(status.pending)
    }
}
