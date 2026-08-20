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
        assertFalse(recreated.typingViMuted())
        recreated.updateTypingViMuted(true)
        assertTrue(AndroidStudyPreferencesController(store).typingViMuted())
        assertFalse(recreated.continuousSkimEnabled())
        recreated.updateContinuousSkim(true)
        assertTrue(AndroidStudyPreferencesController(store).continuousSkimEnabled())

        assertNull(recreated.insightsScopePackageId())
        recreated.updateInsightsScopePackageId("pkg-123")
        assertEquals("pkg-123", AndroidStudyPreferencesController(store).insightsScopePackageId())
        recreated.updateInsightsScopePackageId(null)
        assertNull(AndroidStudyPreferencesController(store).insightsScopePackageId())
    }

    private class FakeStore : AndroidStudyPreferenceStore {
        private var value = DailyStudyBudgetLimits()
        private var typingMuted = false
        private var continuousSkim = false
        private var insightsPackageId: String? = null
        override fun load() = value
        override fun save(limits: DailyStudyBudgetLimits) { value = limits }
        override fun loadTypingViMuted() = typingMuted
        override fun saveTypingViMuted(muted: Boolean) { typingMuted = muted }
        override fun loadContinuousSkim() = continuousSkim
        override fun saveContinuousSkim(enabled: Boolean) { continuousSkim = enabled }
        override fun loadInsightsScopePackageId() = insightsPackageId
        override fun saveInsightsScopePackageId(packageId: String?) { insightsPackageId = packageId }
    }
}
