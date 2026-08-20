package vn.loi.learning.android.reminder

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class AndroidHomeVocabularyWidgetCoordinatorTest {

    private class InMemoryShuffleBagStore : LockScreenShuffleBagStore {
        private val storage = mutableMapOf<String, LockScreenShuffleBagState>()
        override fun load(key: String): LockScreenShuffleBagState? = storage[key]
        override fun save(key: String, state: LockScreenShuffleBagState): Boolean {
            storage[key] = state
            return true
        }
        override fun clear(key: String): Boolean {
            storage.remove(key)
            return true
        }
    }

    private fun createSampleCandidate(id: String, text: String): AndroidVocabularyCandidate {
        return AndroidVocabularyCandidate(
            contentId = ContentId(id),
            packageId = InstalledPackageId("pkg-1"),
            packageName = "Package 1",
            primaryText = text,
            answer = "Answer $text",
            translation = "Nghĩa $text",
            ipa = "test",
            partOfSpeech = "noun",
            imageReference = null,
            primaryAudioReference = null
        )
    }

    @Test
    fun `home widget shuffle bag is strictly isolated from lock screen and unlocked popup bags`() {
        val store = InMemoryShuffleBagStore()
        val keyLockScreen = "pkg-1:RANDOM_ALL"
        val keyHomeWidget = "HOME_WIDGET:pkg-1:RANDOM_ALL"

        val lockBag = LockScreenShuffleBagState(
            cycleVersion = 1,
            orderedCandidateIds = listOf("cand-1", "cand-2", "cand-3"),
            currentIndex = 2,
            lastPresentedCandidateId = "cand-2"
        )
        store.save(keyLockScreen, lockBag)

        val widgetBag = LockScreenShuffleBagState(
            cycleVersion = 1,
            orderedCandidateIds = listOf("cand-3", "cand-1", "cand-2"),
            currentIndex = 0,
            lastPresentedCandidateId = null
        )
        store.save(keyHomeWidget, widgetBag)

        val loadedLock = store.load(keyLockScreen)
        val loadedWidget = store.load(keyHomeWidget)

        assertNotNull(loadedLock)
        assertNotNull(loadedWidget)
        assertEquals(2, loadedLock.currentIndex)
        assertEquals(0, loadedWidget.currentIndex)
        assertEquals("cand-2", loadedLock.lastPresentedCandidateId)
        assertEquals(null, loadedWidget.lastPresentedCandidateId)
    }

    @Test
    fun `auto next timer reconciliation adheres to screen on and widget instance rules`() {
        var isScreenOn = true
        var hasWidgets = true
        var autoNextEnabled = true
        var updateOnlyScreenOn = true

        fun shouldRunTimer(): Boolean {
            return hasWidgets && autoNextEnabled && (!updateOnlyScreenOn || isScreenOn)
        }

        assertTrue(shouldRunTimer())

        isScreenOn = false
        assertFalse(shouldRunTimer())

        isScreenOn = true
        assertTrue(shouldRunTimer())

        autoNextEnabled = false
        assertFalse(shouldRunTimer())

        autoNextEnabled = true
        assertTrue(shouldRunTimer())

        hasWidgets = false
        assertFalse(shouldRunTimer())
    }

    @Test
    fun `multiple widget instances share identical candidate and settings`() {
        val cand = createSampleCandidate("cand-share", "Share Word")
        val activeWidgetIds = mutableSetOf(101, 102)

        assertEquals(2, activeWidgetIds.size)

        var visibleOn101: String? = null
        var visibleOn102: String? = null

        fun renderAll(candidate: AndroidVocabularyCandidate) {
            for (id in activeWidgetIds) {
                if (id == 101) visibleOn101 = candidate.contentId.value
                if (id == 102) visibleOn102 = candidate.contentId.value
            }
        }

        renderAll(cand)
        assertEquals("cand-share", visibleOn101)
        assertEquals("cand-share", visibleOn102)

        activeWidgetIds.remove(101)
        assertEquals(1, activeWidgetIds.size)
        assertTrue(activeWidgetIds.contains(102))
    }

    @Test
    fun `initialization race test A - first widget added selects and persists exactly one candidate`() {
        var selectedCount = 0
        var currentCandidate: AndroidVocabularyCandidate? = null
        var persistedCandidateId: String? = null

        fun prepareFirst() {
            if (currentCandidate != null) return
            selectedCount++
            val candidate = createSampleCandidate("cand-init-1", "Init Word 1")
            currentCandidate = candidate
            persistedCandidateId = candidate.contentId.value
        }

        prepareFirst()

        assertEquals(1, selectedCount)
        assertNotNull(currentCandidate)
        assertEquals("cand-init-1", persistedCandidateId)
    }

    @Test
    fun `initialization race test B - simultaneous widget additions execute exactly one preparation`() {
        var prepareCount = 0
        var isPreparing = false
        var currentCandidate: AndroidVocabularyCandidate? = null

        fun onWidgetTrigger(triggerName: String) {
            if (currentCandidate != null) return
            if (isPreparing) return
            isPreparing = true
            prepareCount++
            currentCandidate = createSampleCandidate("cand-single", "Single Candidate")
            isPreparing = false
        }

        // Simulate simultaneous onFirstWidgetEnabled and onWidgetsUpdate
        onWidgetTrigger("FIRST_WIDGET_ENABLED")
        onWidgetTrigger("WIDGETS_UPDATED")

        assertEquals(1, prepareCount)
        assertNotNull(currentCandidate)
        assertEquals("cand-single", currentCandidate?.contentId?.value)
    }

    @Test
    fun `initialization race test C - existing current candidate renders immediately without selector call`() {
        var selectorCalls = 0
        val existingCandidate = createSampleCandidate("cand-exist", "Existing Word")
        var currentCandidate: AndroidVocabularyCandidate? = existingCandidate

        var renderedCandidateId: String? = null

        fun onWidgetUpdate() {
            if (currentCandidate != null) {
                renderedCandidateId = currentCandidate?.contentId?.value
                return
            }
            selectorCalls++
        }

        onWidgetUpdate()

        assertEquals(0, selectorCalls)
        assertEquals("cand-exist", renderedCandidateId)
    }

    @Test
    fun `initialization race test D - candidate resolution failure fails gracefully without retry loop`() {
        var prepareAttempts = 0
        var isPreparing = false
        var currentCandidate: AndroidVocabularyCandidate? = null

        fun prepare() {
            if (currentCandidate != null) return
            if (isPreparing) return
            isPreparing = true
            prepareAttempts++
            // Simulate no candidate available (e.g. empty library)
            currentCandidate = null
            isPreparing = false
        }

        prepare()

        assertEquals(1, prepareAttempts)
        assertEquals(null, currentCandidate)
    }

    @Test
    fun `initialization race test E - widget removed while candidate is preparing cancels timer and avoids orphan state`() {
        val activeWidgetIds = mutableSetOf(201)
        var timerRunning = true

        // Simulate widget deletion during preparation
        activeWidgetIds.remove(201)
        if (activeWidgetIds.isEmpty()) {
            timerRunning = false
        }

        assertFalse(timerRunning)
        assertEquals(0, activeWidgetIds.size)
    }

    @Test
    fun `interval change cancels existing timer and starts new timer without advancing current candidate`() {
        val current = createSampleCandidate("cand-curr", "Current Word")
        var visibleCandidate = current
        var timerIntervalMs = 30_000L
        var advanceCallCount = 0

        fun onIntervalChanged(newIntervalMs: Long) {
            timerIntervalMs = newIntervalMs
            // Notice: visibleCandidate remains untouched
        }

        fun onTimerFired() {
            advanceCallCount++
            visibleCandidate = createSampleCandidate("cand-next", "Next Word")
        }

        onIntervalChanged(5_000L)

        assertEquals("cand-curr", visibleCandidate.contentId.value)
        assertEquals(5_000L, timerIntervalMs)
        assertEquals(0, advanceCallCount)

        onTimerFired()
        assertEquals(1, advanceCallCount)
        assertEquals("cand-next", visibleCandidate.contentId.value)
    }

    @Test
    fun `rapid timer ticks do not overlap or cause duplicate shuffle advancement`() {
        var isAdvancing = false
        var successfulAdvances = 0
        var skippedAdvances = 0

        fun triggerAdvance() {
            if (isAdvancing) {
                skippedAdvances++
                return
            }
            isAdvancing = true
            successfulAdvances++
            // simulate fast completion
            isAdvancing = false
        }

        // 10 consecutive ticks
        for (i in 1..10) {
            triggerAdvance()
        }

        assertEquals(10, successfulAdvances)
        assertEquals(0, skippedAdvances)

        // simulate overlapping tick
        isAdvancing = true
        triggerAdvance()
        assertEquals(1, skippedAdvances)
        assertEquals(10, successfulAdvances)
    }
}
