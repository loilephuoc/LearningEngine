package vn.loi.learning.android.study

import kotlin.test.*
import org.junit.Test
import vn.loi.learning.application.study.DailyStudyBudgetLimits

class AndroidStudyPreferencesTest {
    @Test
    fun `defaults persist updates across controller recreation and reject invalid values`() {
        val store = FakeStore()
        val first = AndroidStudyPreferencesController(store)
        assertEquals(DailyStudyBudgetLimits(20, 100), first.current())
        assertTrue(first.updateNew(50))
        assertTrue(first.updateReview(80))
        assertFalse(first.updateNew(0))
        assertFalse(first.updateReview(1_000))

        val recreated = AndroidStudyPreferencesController(store)
        assertEquals(DailyStudyBudgetLimits(50, 80), recreated.current())
    }

    private class FakeStore : AndroidStudyPreferenceStore {
        private var value = DailyStudyBudgetLimits()
        override fun load() = value
        override fun save(limits: DailyStudyBudgetLimits) { value = limits }
    }
}
