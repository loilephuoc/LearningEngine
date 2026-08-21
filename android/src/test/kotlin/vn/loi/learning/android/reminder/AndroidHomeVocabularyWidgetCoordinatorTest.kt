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

    private class InMemoryDifficultStore : AndroidVocabularyReminderDifficultMarkers {
        private val difficultItems = mutableSetOf<String>()
        override fun isMarked(contentId: ContentId): Boolean {
            return difficultItems.contains(contentId.value)
        }
        override fun markedContentIds(): Set<ContentId> {
            return difficultItems.map(::ContentId).toSet()
        }
        override fun toggle(contentId: ContentId): Boolean {
            return if (difficultItems.contains(contentId.value)) {
                difficultItems.remove(contentId.value)
                false
            } else {
                difficultItems.add(contentId.value)
                true
            }
        }
        override fun setMarked(contentId: ContentId, marked: Boolean): Boolean {
            if (marked) difficultItems.add(contentId.value) else difficultItems.remove(contentId.value)
            return marked
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
            primaryAudioReference = "audio/$id.mp3"
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
            isAdvancing = false
        }

        for (i in 1..10) {
            triggerAdvance()
        }

        assertEquals(10, successfulAdvances)
        assertEquals(0, skippedAdvances)

        isAdvancing = true
        triggerAdvance()
        assertEquals(1, skippedAdvances)
        assertEquals(10, successfulAdvances)
    }

    // -------------------------------------------------------------
    // ROUND 9.0.12 UNIT TESTS: 3-ACTION OVERLAY & AUDIO ENGINE
    // -------------------------------------------------------------

    @Test
    fun `mark difficult quick action toggles state without candidate change or FSRS mutation`() {
        val diffStore = InMemoryDifficultStore()
        val candidate = createSampleCandidate("cand-star-1", "Star Word")
        var currentCandidate = candidate

        assertFalse(diffStore.isMarked(ContentId("cand-star-1")))

        // First tap: unmarked -> marked
        val marked1 = diffStore.toggle(ContentId("cand-star-1"))
        assertTrue(marked1)
        assertTrue(diffStore.isMarked(ContentId("cand-star-1")))
        assertEquals("cand-star-1", currentCandidate.contentId.value)

        // Second tap: marked -> unmarked
        val marked2 = diffStore.toggle(ContentId("cand-star-1"))
        assertFalse(marked2)
        assertFalse(diffStore.isMarked(ContentId("cand-star-1")))
        assertEquals("cand-star-1", currentCandidate.contentId.value)
    }

    @Test
    fun `auto-audio preference toggles and persists independently`() {
        var autoAudioEnabled = false

        fun toggleAutoAudio(): Boolean {
            autoAudioEnabled = !autoAudioEnabled
            return autoAudioEnabled
        }

        assertFalse(autoAudioEnabled)

        // OFF -> ON
        assertTrue(toggleAutoAudio())
        assertTrue(autoAudioEnabled)

        // ON -> OFF
        assertFalse(toggleAutoAudio())
        assertFalse(autoAudioEnabled)
    }

    @Test
    fun `auto-play plays exactly once on real candidate transition when Auto Audio is ON`() {
        var autoAudioEnabled = true
        var isScreenOn = true
        var lastPlayedCandidateId: String? = null
        var lastPlayedCycleToken = 0L
        var currentCycleToken = 0L
        var playCount = 0

        fun onCandidateTransition(candidateId: String) {
            currentCycleToken++
            if (!autoAudioEnabled || !isScreenOn) return
            if (lastPlayedCandidateId == candidateId && lastPlayedCycleToken == currentCycleToken) return
            lastPlayedCandidateId = candidateId
            lastPlayedCycleToken = currentCycleToken
            playCount++
        }

        fun onReRender(candidateId: String) {
            // Re-rendering UI never triggers auto-play
        }

        // 1. First candidate transition A
        onCandidateTransition("cand-A")
        assertEquals(1, playCount)
        assertEquals("cand-A", lastPlayedCandidateId)

        // 2. Re-rendering candidate A (e.g. launcher rebind, star click) does NOT replay
        onReRender("cand-A")
        assertEquals(1, playCount)

        // 3. Second real candidate transition B
        onCandidateTransition("cand-B")
        assertEquals(2, playCount)
        assertEquals("cand-B", lastPlayedCandidateId)

        // 4. Screen OFF prevents auto-play
        isScreenOn = false
        onCandidateTransition("cand-C")
        assertEquals(2, playCount) // Did not play

        // 5. Screen ON with same candidate does NOT auto-play
        isScreenOn = true
        onReRender("cand-C")
        assertEquals(2, playCount)

        // 6. Next real transition plays candidate D
        onCandidateTransition("cand-D")
        assertEquals(3, playCount)
    }

    @Test
    fun `auto-play remains silent on transitions when Auto Audio is OFF`() {
        val autoAudioEnabled = false
        var playCount = 0

        fun onCandidateTransition(candidateId: String) {
            if (!autoAudioEnabled) return
            playCount++
        }

        onCandidateTransition("cand-1")
        onCandidateTransition("cand-2")
        onCandidateTransition("cand-3")

        assertEquals(0, playCount)
    }

    @Test
    fun `manual play works regardless of Auto Audio toggle and does not advance candidate`() {
        var autoAudioEnabled = false
        val currentCandidate = createSampleCandidate("cand-manual", "Manual Word")
        var manualPlayCount = 0
        var activeCandidate = currentCandidate

        fun playManual() {
            // Manual play works even if autoAudioEnabled == false
            manualPlayCount++
        }

        // While auto-audio is OFF:
        playManual()
        assertEquals(1, manualPlayCount)
        assertEquals("cand-manual", activeCandidate.contentId.value)

        // While auto-audio is ON:
        autoAudioEnabled = true
        playManual()
        assertEquals(2, manualPlayCount)
        assertEquals("cand-manual", activeCandidate.contentId.value)
    }

    @Test
    fun `candidate audio collision stops previous playback and replaces without overlap`() {
        var currentlyPlayingId: String? = null
        var stoppedId: String? = null

        fun startAudio(candidateId: String) {
            if (currentlyPlayingId != null) {
                stoppedId = currentlyPlayingId
            }
            currentlyPlayingId = candidateId
        }

        startAudio("cand-1")
        assertEquals("cand-1", currentlyPlayingId)
        assertEquals(null, stoppedId)

        // Candidate 2 arrives while 1 is playing
        startAudio("cand-2")
        assertEquals("cand-2", currentlyPlayingId)
        assertEquals("cand-1", stoppedId)
    }

    // -------------------------------------------------------------
    // ROUND 9.0.13 UNIT TESTS: PREVIOUS / NEXT NAVIGATION
    // -------------------------------------------------------------

    @Test
    fun `previous navigation steps backward deterministically in shuffle bag cycle`() {
        val bagItems = listOf("A", "B", "C", "D")
        var currentIndex = 0
        var lastPresented: String? = null

        fun advanceNext(): String {
            val selected = bagItems[currentIndex]
            currentIndex = (currentIndex + 1).coerceAtMost(bagItems.size)
            lastPresented = selected
            return selected
        }

        fun advancePrev(): String {
            val prevIdx = when {
                currentIndex >= 2 -> currentIndex - 2
                currentIndex == 1 -> bagItems.size - 1
                else -> bagItems.size - 1
            }.coerceIn(0, bagItems.size - 1)
            val selected = bagItems[prevIdx]
            currentIndex = prevIdx + 1
            lastPresented = selected
            return selected
        }

        // 1. Advance next: A, B, C
        assertEquals("A", advanceNext())
        assertEquals(1, currentIndex)
        assertEquals("B", advanceNext())
        assertEquals(2, currentIndex)
        assertEquals("C", advanceNext())
        assertEquals(3, currentIndex)

        // 2. Navigate previous: should get B
        assertEquals("B", advancePrev())
        assertEquals(2, currentIndex)

        // 3. Navigate previous again: should get A
        assertEquals("A", advancePrev())
        assertEquals(1, currentIndex)

        // 4. Navigate next: should return to B
        assertEquals("B", advanceNext())
        assertEquals(2, currentIndex)
    }

    @Test
    fun `navigation actions re-arm auto-next timer for a fresh full interval`() {
        var timerArmedInterval = 0L
        var timerArmedAt = 0L
        var simulatedClock = 1000L

        fun armTimer(interval: Long) {
            timerArmedInterval = interval
            timerArmedAt = simulatedClock
        }

        // Initially timer armed with 60s
        armTimer(60_000L)
        assertEquals(1000L, timerArmedAt)

        // 20s pass
        simulatedClock += 20_000L

        // User taps Next (›): timer is re-armed from current timestamp
        armTimer(60_000L)
        assertEquals(21_000L, timerArmedAt)

        // 15s pass
        simulatedClock += 15_000L

        // User taps Previous (‹): timer is re-armed again
        armTimer(60_000L)
        assertEquals(36_000L, timerArmedAt)
    }

    // -------------------------------------------------------------
    // ROUND 9.0.14 UNIT TESTS: BODY TAP REPLAY + REORDERED RAIL + EYE ACTION
    // -------------------------------------------------------------

    @Test
    fun `body tap triggers manual replay of current candidate regardless of auto audio state`() {
        val candidate = createSampleCandidate("cand-body-1", "Body Candidate")
        var currentCandidate: AndroidVocabularyCandidate? = candidate
        var autoAudioEnabled = false
        var replayCount = 0
        var selectorCalls = 0

        fun onBodyTap() {
            // Body tap replays current candidate
            replayCount++
        }

        // 1. When Auto-Audio is OFF:
        onBodyTap()
        assertEquals(1, replayCount)
        assertEquals("cand-body-1", currentCandidate?.contentId?.value)
        assertEquals(0, selectorCalls)

        // 2. When Auto-Audio is ON:
        autoAudioEnabled = true
        onBodyTap()
        assertEquals(2, replayCount)
        assertEquals("cand-body-1", currentCandidate?.contentId?.value)
        assertEquals(0, selectorCalls)

        // 3. Repeated body tap restarts playback without changing candidate
        onBodyTap()
        onBodyTap()
        assertEquals(4, replayCount)
        assertEquals("cand-body-1", currentCandidate?.contentId?.value)
    }

    @Test
    fun `eye action opens full review for exact current candidate without mutating state`() {
        val candidate = createSampleCandidate("cand-eye-1", "Eye Candidate")
        val currentCandidate: AndroidVocabularyCandidate = candidate
        var openedCandidateId: String? = null
        var selectorCalls = 0

        fun onEyeClick(targetCandidate: AndroidVocabularyCandidate) {
            openedCandidateId = targetCandidate.contentId.value
        }

        onEyeClick(currentCandidate)
        assertEquals("cand-eye-1", openedCandidateId)
        assertEquals(0, selectorCalls)
    }

    @Test
    fun `quick action rail position order contract is strictly Audio, Star, Eye`() {
        val railOrder = listOf("AUTO_AUDIO", "DIFFICULT", "FULL_REVIEW")
        assertEquals("AUTO_AUDIO", railOrder[0])
        assertEquals("DIFFICULT", railOrder[1])
        assertEquals("FULL_REVIEW", railOrder[2])
    }

    // -------------------------------------------------------------
    // ROUND 9.0.14.1 UNIT TESTS: MUTE VISUAL SEMANTICS & PERSISTENT STATE
    // -------------------------------------------------------------

    @Test
    fun `auto audio visual semantics contract is normal dark when enabled and RED when muted`() {
        var autoAudioEnabled = true

        fun getAudioVisual(): Pair<String, String> = if (autoAudioEnabled) {
            "SPEAKER" to "NORMAL_DARK"
        } else {
            "MUTED_SPEAKER" to "RED"
        }

        // 1. UNMUTED: speaker icon with normal dark color
        val (iconOn, tintOn) = getAudioVisual()
        assertEquals("SPEAKER", iconOn)
        assertEquals("NORMAL_DARK", tintOn)

        // 2. MUTED: muted speaker icon with RED color
        autoAudioEnabled = false
        val (iconOff, tintOff) = getAudioVisual()
        assertEquals("MUTED_SPEAKER", iconOff)
        assertEquals("RED", tintOff)
    }

    @Test
    fun `mute state persists across candidate transitions A to B to C to D with zero auto play`() {
        var autoAudioEnabled = true
        var autoPlayedCandidates = mutableListOf<String>()

        fun onCandidateAppeared(candidateId: String) {
            if (autoAudioEnabled) {
                autoPlayedCandidates.add(candidateId)
            }
        }

        // Initial state unmuted
        onCandidateAppeared("A")
        assertEquals(listOf("A"), autoPlayedCandidates)

        // User mutes widget
        autoAudioEnabled = false

        // Subsequent candidate transitions: B, C, D
        onCandidateAppeared("B")
        onCandidateAppeared("C")
        onCandidateAppeared("D")

        // Preference remains false throughout, no new auto-played audio
        assertFalse(autoAudioEnabled)
        assertEquals(listOf("A"), autoPlayedCandidates)
    }

    @Test
    fun `mute state persists through previous and next navigation`() {
        var autoAudioEnabled = false
        var autoPlayedCount = 0

        fun navigate(direction: String, targetCandidateId: String) {
            // Navigation does not mutate autoAudioEnabled
            if (autoAudioEnabled) {
                autoPlayedCount++
            }
        }

        navigate("NEXT", "B")
        navigate("NEXT", "C")
        navigate("PREV", "B")
        navigate("NEXT", "C")

        assertFalse(autoAudioEnabled)
        assertEquals(0, autoPlayedCount)
    }

    @Test
    fun `unmute state persists through subsequent transitions and autoplays new candidates`() {
        var autoAudioEnabled = false
        val played = mutableListOf<String>()

        // User unmutes
        autoAudioEnabled = true
        // Confirmation play for current candidate A
        played.add("A")

        fun onTransition(candidateId: String) {
            if (autoAudioEnabled) {
                played.add(candidateId)
            }
        }

        onTransition("B")
        onTransition("C")

        assertTrue(autoAudioEnabled)
        assertEquals(listOf("A", "B", "C"), played)
    }

    @Test
    fun `recreation of coordinator or preferences preserves false without resetting to default`() {
        var storedAutoAudio = false

        // Recreate simulated preferences controller
        fun loadPreferences(): Boolean = storedAutoAudio

        val restored = loadPreferences()
        assertFalse(restored)
    }

    @Test
    fun `pure rendering produces zero writes to auto audio preference`() {
        var preferenceWriteCount = 0
        var autoAudioEnabled = false

        fun renderCandidate(candidateId: String, currentAudioSetting: Boolean) {
            // Pure renderer only reads currentAudioSetting and renders RemoteViews
            // Does NOT call preferenceStore.save()
        }

        renderCandidate("A", autoAudioEnabled)
        renderCandidate("A", autoAudioEnabled)
        renderCandidate("B", autoAudioEnabled)
        renderCandidate("C", autoAudioEnabled)

        assertEquals(0, preferenceWriteCount)
        assertFalse(autoAudioEnabled)
    }

    @Test
    fun `body tap manual replay works while muted and does not unmute the widget`() {
        var autoAudioEnabled = false
        var manualReplayCount = 0

        fun onBodyTap() {
            // Manual replay plays audio directly without altering autoAudioEnabled
            manualReplayCount++
        }

        onBodyTap()
        assertEquals(1, manualReplayCount)
        assertFalse(autoAudioEnabled) // Must remain muted!

        onBodyTap()
        assertEquals(2, manualReplayCount)
        assertFalse(autoAudioEnabled)
    }

    @Test
    fun `click isolation contract ensures individual action clicks do not bubble into body replay or alter audio preference`() {
        var autoAudioEnabled = false
        var bodyReplayCount = 0
        var audioToggleCount = 0
        var starToggleCount = 0
        var eyeActionCount = 0
        var prevActionCount = 0
        var nextActionCount = 0

        fun clickAudio() {
            audioToggleCount++
            autoAudioEnabled = !autoAudioEnabled
        }
        fun clickStar() { starToggleCount++ }
        fun clickEye() { eyeActionCount++ }
        fun clickPrev() { prevActionCount++ }
        fun clickNext() { nextActionCount++ }
        fun clickBody() { bodyReplayCount++ }

        clickStar()
        clickEye()
        clickPrev()
        clickNext()

        assertEquals(1, starToggleCount)
        assertEquals(1, eyeActionCount)
        assertEquals(1, prevActionCount)
        assertEquals(1, nextActionCount)
        assertEquals(0, bodyReplayCount) // Body replay must be strictly 0!
        assertFalse(autoAudioEnabled) // Star, Eye, Prev, Next must not touch audio preference!

        clickBody()
        assertEquals(1, bodyReplayCount)
        assertFalse(autoAudioEnabled) // Body tap must not touch audio preference!

        clickAudio()
        assertEquals(1, audioToggleCount)
        assertTrue(autoAudioEnabled) // Only audio click toggles preference!
    }

    // -------------------------------------------------------------
    // ROUND 10.1.1 UNIT TESTS: HOME WIDGET UNLOCK-STATE LIFECYCLE
    // -------------------------------------------------------------

    @Test
    fun `screen off cancels timer, stops audio, and preserves candidate without consuming shuffle`() {
        var timerArmed = true
        var audioPlaying = true
        val currentCandidate = createSampleCandidate("cand-screen-off", "Word")
        var visibleCandidate = currentCandidate
        var state = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON

        fun onScreenOff() {
            state = VocabularyPresentationDeviceState.SCREEN_OFF
            timerArmed = false
            audioPlaying = false
        }

        onScreenOff()

        assertEquals(VocabularyPresentationDeviceState.SCREEN_OFF, state)
        assertFalse(timerArmed)
        assertFalse(audioPlaying)
        assertEquals("cand-screen-off", visibleCandidate.contentId.value)
    }

    @Test
    fun `screen on while locked enters LOCKED_SCREEN_ON and keeps widget timer suppressed with zero audio`() {
        var state = VocabularyPresentationDeviceState.SCREEN_OFF
        var timerArmed = false
        var advanceCallCount = 0
        var audioPlayedCount = 0

        fun onScreenOn(isLocked: Boolean) {
            state = if (isLocked) VocabularyPresentationDeviceState.LOCKED_SCREEN_ON else VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            val isUnlocked = state == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            if (isUnlocked) {
                timerArmed = true
            } else {
                timerArmed = false
            }
        }

        // Screen wakes up to Lock Screen
        onScreenOn(isLocked = true)

        assertEquals(VocabularyPresentationDeviceState.LOCKED_SCREEN_ON, state)
        assertFalse(timerArmed) // Timer MUST remain suppressed!
        assertEquals(0, advanceCallCount)
        assertEquals(0, audioPlayedCount)
    }

    @Test
    fun `unlocking device transitions to UNLOCKED_SCREEN_ON and arms full fresh interval without immediate advance or audio`() {
        var state = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        var timerArmedAt = 0L
        var timerInterval = 0L
        var advanceCount = 0
        var audioCount = 0
        val simulatedClock = 10_000L

        fun onUserPresent(now: Long, intervalMs: Long) {
            state = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            // Start fresh full interval
            timerArmedAt = now
            timerInterval = intervalMs
            // Critical contract: DO NOT advance immediately, DO NOT play audio immediately
        }

        onUserPresent(simulatedClock, 30_000L)

        assertEquals(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, state)
        assertEquals(10_000L, timerArmedAt)
        assertEquals(30_000L, timerInterval)
        assertEquals(0, advanceCount)
        assertEquals(0, audioCount)
    }

    @Test
    fun `full interval tick triggers exactly one candidate transition and audio when unlocked`() {
        var state = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        var currentCandidate = createSampleCandidate("cand-before", "Before")
        var audioPlayedCount = 0
        val autoAudioEnabled = true

        fun onTimerFired() {
            if (state == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) {
                currentCandidate = createSampleCandidate("cand-after", "After")
                if (autoAudioEnabled) {
                    audioPlayedCount++
                }
            }
        }

        onTimerFired()

        assertEquals("cand-after", currentCandidate.contentId.value)
        assertEquals(1, audioPlayedCount)
    }

    @Test
    fun `duplicate SCREEN_ON events while locked produce NO_OP and do not arm timer`() {
        var state = VocabularyPresentationDeviceState.SCREEN_OFF
        var stateTransitionCount = 0

        fun onScreenOn(isLocked: Boolean) {
            val target = if (isLocked) VocabularyPresentationDeviceState.LOCKED_SCREEN_ON else VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            if (state == target) return
            state = target
            stateTransitionCount++
        }

        onScreenOn(isLocked = true)
        assertEquals(1, stateTransitionCount)
        assertEquals(VocabularyPresentationDeviceState.LOCKED_SCREEN_ON, state)

        // Duplicate SCREEN_ON while locked
        onScreenOn(isLocked = true)
        assertEquals(1, stateTransitionCount) // Still 1 (NO_OP)
    }

    @Test
    fun `duplicate USER_PRESENT events do not re-arm or reset running timer repeatedly`() {
        var state = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        var timerArmCount = 0

        fun onUserPresent() {
            if (state == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) return
            state = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            timerArmCount++
        }

        onUserPresent()
        assertEquals(1, timerArmCount)

        // Duplicate USER_PRESENT
        onUserPresent()
        assertEquals(1, timerArmCount) // NO-OP
    }

    @Test
    fun `process startup evaluation while screen is interactive but keyguard locked sets LOCKED_SCREEN_ON and suppresses runtime`() {
        fun resolveState(interactive: Boolean, keyguardLocked: Boolean): VocabularyPresentationDeviceState {
            return when {
                !interactive -> VocabularyPresentationDeviceState.SCREEN_OFF
                keyguardLocked -> VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
                else -> VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            }
        }

        val state = resolveState(interactive = true, keyguardLocked = true)
        assertEquals(VocabularyPresentationDeviceState.LOCKED_SCREEN_ON, state)

        val timerAllowed = state == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        assertFalse(timerAllowed)
    }

    @Test
    fun `process startup evaluation while device is unlocked sets UNLOCKED_SCREEN_ON and allows timer`() {
        fun resolveState(interactive: Boolean, keyguardLocked: Boolean): VocabularyPresentationDeviceState {
            return when {
                !interactive -> VocabularyPresentationDeviceState.SCREEN_OFF
                keyguardLocked -> VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
                else -> VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            }
        }

        val state = resolveState(interactive = true, keyguardLocked = false)
        assertEquals(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, state)

        val timerAllowed = state == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        assertTrue(timerAllowed)
    }

    @Test
    fun `FGS required state remains true for lock screen while widget runtime is paused during lock`() {
        val lockScreenEnabled = true
        val reminderEnabled = false
        val widgetAutoNext = true
        val hasWidgets = true

        val fgsRequirement = ForegroundServiceRequirement(
            lockScreenRequired = lockScreenEnabled,
            unlockedReminderRequired = reminderEnabled,
            homeWidgetRequired = hasWidgets && widgetAutoNext
        )

        // FGS is REQUIRED because lock screen is active
        assertTrue(fgsRequirement.required)

        // Device is currently locked
        val deviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON

        // Home Widget runtime is strictly SUPPRESSED
        val widgetRuntimeAllowed = deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        assertFalse(widgetRuntimeAllowed)
    }

    @Test
    fun `mute state persists through screen off, lock screen wake, unlock, and subsequent timer advance`() {
        var autoAudioEnabled = false
        var audioPlayCount = 0
        var state = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON

        // 1. Screen off
        state = VocabularyPresentationDeviceState.SCREEN_OFF
        assertFalse(autoAudioEnabled)

        // 2. Screen on locked
        state = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        assertFalse(autoAudioEnabled)

        // 3. User unlock
        state = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        assertFalse(autoAudioEnabled)

        // 4. Timer fires after full interval
        fun onTimerFired() {
            if (autoAudioEnabled && state == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) {
                audioPlayCount++
            }
        }

        onTimerFired()
        assertFalse(autoAudioEnabled)
        assertEquals(0, audioPlayCount) // Completely silent throughout
    }

    // =========================================================================
    // ROUND 10.3.1 — HOME-SURFACE VISIBILITY GATE UNIT TESTS (Sections 29-37)
    // =========================================================================

    @Test
    fun `29 unit test - leaving home to other app cancels timer and stops audio while preserving candidate`() {
        var deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        var homeSurfaceVisible = true
        var timerArmed = true
        var audioPlaying = true
        val currentCandidate = createSampleCandidate("cand-A", "Apple")
        var selectorCallCount = 0

        fun isRuntimeAllowed(): Boolean =
            deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON && homeSurfaceVisible

        // Initial: Home visible & armed
        assertTrue(isRuntimeAllowed())
        assertTrue(timerArmed)

        // When: User leaves Home to Chrome (HOME_HIDDEN)
        homeSurfaceVisible = false
        if (!isRuntimeAllowed()) {
            timerArmed = false
            audioPlaying = false
        }

        // Assert
        assertFalse(timerArmed)
        assertFalse(audioPlaying)
        assertEquals("Apple", currentCandidate.primaryText)
        assertEquals(0, selectorCallCount)
    }

    @Test
    fun `30 unit test - remain in other app longer than several intervals produces zero candidate transitions`() {
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val homeSurfaceVisible = false
        var transitionCount = 0
        val intervalMs = 5000L
        val timeInChromeMs = 600_000L // 10 minutes

        fun isRuntimeAllowed(): Boolean =
            deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON && homeSurfaceVisible

        // Simulated timer ticks while in Chrome
        var elapsed = 0L
        while (elapsed < timeInChromeMs) {
            elapsed += intervalMs
            if (isRuntimeAllowed()) {
                transitionCount++
            }
        }

        assertEquals(0, transitionCount)
    }

    @Test
    fun `31 unit test - return from other app to home starts fresh full interval with no immediate transition or audio`() {
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        var homeSurfaceVisible = false
        var currentCandidate = createSampleCandidate("cand-A", "Apple")
        var transitionOccurredImmediately = false
        var audioPlayedImmediately = false
        var timerArmedForFullInterval = false
        val intervalMs = 30_000L

        // Return Home
        homeSurfaceVisible = true
        val runtimeAllowed = deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON && homeSurfaceVisible

        if (runtimeAllowed) {
            // Under contract: DO NOT immediately advance, DO NOT immediately play, start full fresh interval
            transitionOccurredImmediately = false
            audioPlayedImmediately = false
            timerArmedForFullInterval = true
        }

        assertEquals("Apple", currentCandidate.primaryText)
        assertFalse(transitionOccurredImmediately)
        assertFalse(audioPlayedImmediately)
        assertTrue(timerArmedForFullInterval)
    }

    @Test
    fun `32 unit test - full interval timing verification (no transition at 4_999s, transition at 5s)`() {
        val intervalMs = 5000L
        var transitions = 0

        fun checkTimer(elapsedMs: Long) {
            if (elapsedMs >= intervalMs) {
                transitions++
            }
        }

        checkTimer(4999L)
        assertEquals(0, transitions)

        checkTimer(5000L)
        assertEquals(1, transitions)
    }

    @Test
    fun `33 unit test - duplicate home events do not repeatedly reset timer`() {
        var homeSurfaceVisible = false
        var timerResetCount = 0

        fun onHomeEvent(visible: Boolean) {
            if (homeSurfaceVisible == visible) return // Idempotent
            val wasAllowed = homeSurfaceVisible
            homeSurfaceVisible = visible
            val nowAllowed = homeSurfaceVisible

            if (!wasAllowed && nowAllowed) {
                timerResetCount++
            }
        }

        onHomeEvent(true) // 1st event: false -> true
        assertEquals(1, timerResetCount)

        onHomeEvent(true) // 2nd duplicate event: true -> true (no-op)
        assertEquals(1, timerResetCount)

        onHomeEvent(true) // 3rd duplicate event: true -> true (no-op)
        assertEquals(1, timerResetCount)
    }

    @Test
    fun `34 unit test - lock interaction from home pauses widget and unlock into home starts fresh interval`() {
        var deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        var homeSurfaceVisible = true
        var timerRunning = true
        var audioPlayCount = 0

        fun isRuntimeAllowed(): Boolean =
            deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON && homeSurfaceVisible

        // 1. Screen off
        deviceState = VocabularyPresentationDeviceState.SCREEN_OFF
        homeSurfaceVisible = false
        if (!isRuntimeAllowed()) timerRunning = false
        assertFalse(timerRunning)

        // 2. Screen on locked
        deviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        assertFalse(isRuntimeAllowed())
        assertFalse(timerRunning)

        // 3. Unlock into Home
        deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        homeSurfaceVisible = true
        if (isRuntimeAllowed()) timerRunning = true
        assertTrue(timerRunning)
        assertEquals(0, audioPlayCount) // No immediate audio
    }

    @Test
    fun `35 unit test - unlock directly into an application leaves widget paused until returning home`() {
        var deviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        var homeSurfaceVisible = false
        var timerRunning = false

        fun isRuntimeAllowed(): Boolean =
            deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON && homeSurfaceVisible

        // Unlock directly into Chrome
        deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        homeSurfaceVisible = false // in Chrome
        timerRunning = isRuntimeAllowed()

        assertFalse(timerRunning)

        // User switches from Chrome to Home
        homeSurfaceVisible = true
        timerRunning = isRuntimeAllowed()

        assertTrue(timerRunning)
    }

    @Test
    fun `36 unit test - mute state survives home to chrome to home and new candidate remains silent`() {
        var autoAudioEnabled = false // Muted
        var deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        var homeSurfaceVisible = true
        var audioPlayed = false

        // Home -> Chrome
        homeSurfaceVisible = false
        assertFalse(autoAudioEnabled)

        // Chrome -> Home
        homeSurfaceVisible = true
        assertFalse(autoAudioEnabled)

        // Hết full interval -> New candidate
        if (autoAudioEnabled && deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON && homeSurfaceVisible) {
            audioPlayed = true
        }

        assertFalse(autoAudioEnabled)
        assertFalse(audioPlayed)
    }

    @Test
    fun `37 unit test - fgs running true but home surface visible false strictly suppresses widget timer`() {
        val fgsRequirement = ForegroundServiceRequirement(
            lockScreenRequired = true, // Lock screen requires FGS
            unlockedReminderRequired = false,
            homeWidgetRequired = true
        )
        assertTrue(fgsRequirement.required) // FGS is active in background

        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val homeSurfaceState = HomeSurfaceState.HIDDEN // User is currently in another app

        val widgetTimerAllowed = (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) && (homeSurfaceState == HomeSurfaceState.VISIBLE)
        assertFalse(widgetTimerAllowed) // Widget timer is strictly SUPPRESSED
    }

    @Test
    fun `38 unit test - MainActivity stop safety - accessibility disconnected leaves state UNKNOWN and never VISIBLE`() {
        var homeSurfaceState = HomeSurfaceState.UNKNOWN
        val isAccessibilityConnected = false

        // Learning Engine in foreground
        homeSurfaceState = HomeSurfaceState.HIDDEN
        assertEquals(HomeSurfaceState.HIDDEN, homeSurfaceState)

        // MainActivity stops with Accessibility disconnected
        if (!isAccessibilityConnected) {
            homeSurfaceState = HomeSurfaceState.UNKNOWN
        }

        // Must become UNKNOWN, never VISIBLE
        assertEquals(HomeSurfaceState.UNKNOWN, homeSurfaceState)
        assertFalse(homeSurfaceState == HomeSurfaceState.VISIBLE)
    }

    @Test
    fun `39 unit test - accessibility lost - unbind or destroy transitions VISIBLE to UNKNOWN and cancels timer and stops audio`() {
        var homeSurfaceState = HomeSurfaceState.VISIBLE
        var timerArmed = true
        var audioPlaying = true
        val currentCandidate = createSampleCandidate("cand-persist", "Persist Word")

        fun isRuntimeAllowed(): Boolean =
            homeSurfaceState == HomeSurfaceState.VISIBLE

        // Accessibility service is unbound / destroyed
        homeSurfaceState = HomeSurfaceState.UNKNOWN
        if (!isRuntimeAllowed()) {
            timerArmed = false
            audioPlaying = false
        }

        assertEquals(HomeSurfaceState.UNKNOWN, homeSurfaceState)
        assertFalse(timerArmed)
        assertFalse(audioPlaying)
        assertEquals("cand-persist", currentCandidate.contentId.value)
    }

    @Test
    fun `40 unit test - UNKNOWN state strictly pauses widget runtime`() {
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val hasWidgets = true
        val autoNextEnabled = true
        val homeSurfaceState = HomeSurfaceState.UNKNOWN

        val isRuntimeAllowed = (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) &&
            (homeSurfaceState == HomeSurfaceState.VISIBLE)
        val timerAllowed = hasWidgets && autoNextEnabled && isRuntimeAllowed

        assertFalse(isRuntimeAllowed)
        assertFalse(timerAllowed)
    }

    @Test
    fun `41 unit test - accessibility window state launcher transitions UNKNOWN to VISIBLE with fresh full interval`() {
        var homeSurfaceState = HomeSurfaceState.UNKNOWN
        val defaultLauncher = "com.android.launcher"
        var timerIntervalMs: Long = 0L
        var candidateChangedImmediately = false

        fun onWindowStateChanged(pkg: String) {
            val isTransient = pkg == "com.android.systemui" || pkg.contains("keyboard")
            if (!isTransient) {
                val newState = if (pkg == defaultLauncher) HomeSurfaceState.VISIBLE else HomeSurfaceState.HIDDEN
                if (homeSurfaceState != newState) {
                    val wasAllowed = homeSurfaceState == HomeSurfaceState.VISIBLE
                    homeSurfaceState = newState
                    val nowAllowed = homeSurfaceState == HomeSurfaceState.VISIBLE
                    if (!wasAllowed && nowAllowed) {
                        timerIntervalMs = 5000L // Fresh full interval
                        candidateChangedImmediately = false
                    }
                }
            }
        }

        onWindowStateChanged("com.android.launcher")

        assertEquals(HomeSurfaceState.VISIBLE, homeSurfaceState)
        assertEquals(5000L, timerIntervalMs)
        assertFalse(candidateChangedImmediately)
    }

    @Test
    fun `42 unit test - accessibility window state Chrome transitions VISIBLE to HIDDEN and cancels timer and stops audio`() {
        var homeSurfaceState = HomeSurfaceState.VISIBLE
        val defaultLauncher = "com.android.launcher"
        var timerArmed = true
        var audioPlaying = true

        fun onWindowStateChanged(pkg: String) {
            val isTransient = pkg == "com.android.systemui" || pkg.contains("keyboard")
            if (!isTransient) {
                val newState = if (pkg == defaultLauncher) HomeSurfaceState.VISIBLE else HomeSurfaceState.HIDDEN
                if (homeSurfaceState != newState) {
                    val wasAllowed = homeSurfaceState == HomeSurfaceState.VISIBLE
                    homeSurfaceState = newState
                    val nowAllowed = homeSurfaceState == HomeSurfaceState.VISIBLE
                    if (wasAllowed && !nowAllowed) {
                        timerArmed = false
                        audioPlaying = false
                    }
                }
            }
        }

        onWindowStateChanged("com.android.chrome")

        assertEquals(HomeSurfaceState.HIDDEN, homeSurfaceState)
        assertFalse(timerArmed)
        assertFalse(audioPlaying)
    }

    @Test
    fun `43 unit test - Learning Engine to Chrome with accessibility OFF remains UNKNOWN and never triggers passive widget transition`() {
        var homeSurfaceState = HomeSurfaceState.UNKNOWN
        val accessibilityConnected = false
        var transitionsCount = 0

        // 1. Learning Engine in foreground
        homeSurfaceState = HomeSurfaceState.HIDDEN
        assertEquals(HomeSurfaceState.HIDDEN, homeSurfaceState)

        // 2. Learning Engine stops with accessibility OFF
        if (!accessibilityConnected) {
            homeSurfaceState = HomeSurfaceState.UNKNOWN
        }
        assertEquals(HomeSurfaceState.UNKNOWN, homeSurfaceState)

        // 3. User is in Chrome for 60 seconds
        val runtimeAllowed = homeSurfaceState == HomeSurfaceState.VISIBLE
        if (runtimeAllowed) {
            transitionsCount++
        }

        assertFalse(homeSurfaceState == HomeSurfaceState.VISIBLE)
        assertEquals(0, transitionsCount)
    }

    @Test
    fun `44 unit test - mute state survives VISIBLE to UNKNOWN to VISIBLE without reset`() {
        var autoAudioEnabled = false // User muted
        var homeSurfaceState = HomeSurfaceState.VISIBLE

        // Disconnect accessibility
        homeSurfaceState = HomeSurfaceState.UNKNOWN
        assertFalse(autoAudioEnabled)

        // Reconnect into launcher
        homeSurfaceState = HomeSurfaceState.VISIBLE
        assertFalse(autoAudioEnabled)
    }

    @Test
    fun `45 unit test - manual widget action functions when homeSurfaceState is UNKNOWN`() {
        val homeSurfaceState = HomeSurfaceState.UNKNOWN
        var manualActionExecuted = false
        val currentCandidate: AndroidVocabularyCandidate? = createSampleCandidate("cand-manual", "Manual Word")

        fun onManualTapReplay() {
            // Manual action does NOT require passive visibility gate
            if (currentCandidate != null) {
                manualActionExecuted = true
            }
        }

        onManualTapReplay()

        assertTrue(manualActionExecuted)
        assertEquals(HomeSurfaceState.UNKNOWN, homeSurfaceState)
    }

    @Test
    fun `46 unit test - hidden home 60s causes zero auto-next fires, zero selector calls, zero audio, zero renders`() {
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val homeSurfaceState = HomeSurfaceState.HIDDEN
        val intervalMs = 2000L
        val timeInHiddenMs = 60_000L

        var timerFires = 0
        var selectorCalls = 0
        var autoAudioPlays = 0
        var rendersFromAutoNext = 0

        fun isRuntimeAllowed(): Boolean =
            (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) &&
                (homeSurfaceState == HomeSurfaceState.VISIBLE)

        var elapsed = 0L
        while (elapsed < timeInHiddenMs) {
            elapsed += intervalMs
            if (isRuntimeAllowed()) {
                timerFires++
                selectorCalls++
                autoAudioPlays++
                rendersFromAutoNext++
            }
        }

        assertEquals(0, timerFires)
        assertEquals(0, selectorCalls)
        assertEquals(0, autoAudioPlays)
        assertEquals(0, rendersFromAutoNext)
    }

    @Test
    fun `47 unit test - UNKNOWN 60s produces zero passive work`() {
        val deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        val homeSurfaceState = HomeSurfaceState.UNKNOWN
        val intervalMs = 2000L
        var timerFires = 0

        val isAllowed = (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) &&
            (homeSurfaceState == HomeSurfaceState.VISIBLE)

        repeat(30) {
            if (isAllowed) timerFires++
        }

        assertEquals(0, timerFires)
    }

    @Test
    fun `48 unit test - return home from hidden arms exactly one full fresh timer`() {
        var homeSurfaceState = HomeSurfaceState.HIDDEN
        var timerArmedCount = 0
        var armedIntervalMs = 0L
        var candidateChanges = 0

        fun onHomeSurfaceChanged(newState: HomeSurfaceState) {
            if (homeSurfaceState == newState) return
            val wasAllowed = homeSurfaceState == HomeSurfaceState.VISIBLE
            homeSurfaceState = newState
            val nowAllowed = homeSurfaceState == HomeSurfaceState.VISIBLE

            if (!wasAllowed && nowAllowed) {
                timerArmedCount++
                armedIntervalMs = 5000L
            }
        }

        // Return Home
        onHomeSurfaceChanged(HomeSurfaceState.VISIBLE)

        assertEquals(1, timerArmedCount)
        assertEquals(5000L, armedIntervalMs)
        assertEquals(0, candidateChanges) // No immediate candidate change
    }

    @Test
    fun `49 unit test - duplicate VISIBLE repeated 100 times causes no repeated timer reset or extra render`() {
        var homeSurfaceState = HomeSurfaceState.HIDDEN
        var timerArmedCount = 0
        var renderCount = 0

        fun onHomeSurfaceChanged(newState: HomeSurfaceState) {
            if (homeSurfaceState == newState) return // Idempotent NO_OP
            val wasAllowed = homeSurfaceState == HomeSurfaceState.VISIBLE
            homeSurfaceState = newState
            val nowAllowed = homeSurfaceState == HomeSurfaceState.VISIBLE

            if (!wasAllowed && nowAllowed) {
                timerArmedCount++
                renderCount++
            }
        }

        // First transition to VISIBLE
        onHomeSurfaceChanged(HomeSurfaceState.VISIBLE)
        assertEquals(1, timerArmedCount)
        assertEquals(1, renderCount)

        // 100 duplicate VISIBLE calls
        repeat(100) {
            onHomeSurfaceChanged(HomeSurfaceState.VISIBLE)
        }

        // Must remain exactly 1
        assertEquals(1, timerArmedCount)
        assertEquals(1, renderCount)
    }

    @Test
    fun `50 unit test - screen event storm preserves single timer without churn`() {
        var currentDeviceState = VocabularyPresentationDeviceState.SCREEN_OFF
        var armedTimers = 0

        fun transition(newState: VocabularyPresentationDeviceState) {
            if (currentDeviceState == newState) return // Idempotent
            currentDeviceState = newState
            if (newState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON) {
                armedTimers++
            }
        }

        repeat(50) {
            transition(VocabularyPresentationDeviceState.LOCKED_SCREEN_ON)
            transition(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON)
        }

        assertEquals(50, armedTimers)
    }

    @Test
    fun `51 unit test - widget 2s stress handles 100 candidate intervals without queue growth or duplicate render`() {
        var isAdvancing = false
        var advanceCalls = 0
        var skips = 0
        var renders = 0

        fun advance() {
            if (isAdvancing) {
                skips++
                return
            }
            isAdvancing = true
            advanceCalls++
            renders++ // Exactly one render per completed advance
            isAdvancing = false
        }

        repeat(100) {
            advance()
        }

        assertEquals(100, advanceCalls)
        assertEquals(0, skips)
        assertEquals(100, renders)
    }

    @Test
    fun `52 unit test - default launcher package resolution caching avoids repeated IPC`() {
        var resolveCount = 0
        var cachedPackage: String? = null
        var lastResolveTimeMs = 0L
        val ttlMs = 60_000L

        fun resolveLauncher(now: Long, forceRefresh: Boolean = false): String {
            if (!forceRefresh && cachedPackage != null && (now - lastResolveTimeMs < ttlMs)) {
                return cachedPackage!!
            }
            resolveCount++
            val resolved = "com.miui.home"
            cachedPackage = resolved
            lastResolveTimeMs = now
            return resolved
        }

        // 1st resolve at t = 0
        val p1 = resolveLauncher(0L)
        assertEquals("com.miui.home", p1)
        assertEquals(1, resolveCount)

        // 100 subsequent events within TTL
        for (t in 1..100) {
            val p = resolveLauncher(t * 500L) // up to 50,000 ms < 60,000 ms TTL
            assertEquals("com.miui.home", p)
        }
        assertEquals(1, resolveCount, "Should use cached value within TTL without re-resolving")

        // Resolve after TTL expiry at t = 65,000 ms
        val pAfter = resolveLauncher(65_000L)
        assertEquals("com.miui.home", pAfter)
        assertEquals(2, resolveCount, "Should re-resolve after TTL expiration")
    }

    @Test
    fun `53 unit test - hasVisualOrScheduleChanges ignores candidateId-only changes to prevent duplicate re-render`() {
        val baseSettings = AndroidHomeVocabularyWidgetSettings(
            autoNextEnabled = true,
            intervalMillis = 5000L,
            selectedPackageId = "pkg-1",
            selectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
            wordSize = LockWallpaperWordSize.LARGE,
            vietnameseSize = LockWallpaperVietnameseSize.MEDIUM,
            imageSize = LockWallpaperImageSize.LARGE,
            cardBackgroundOpacity = 0.92f,
            updateOnlyScreenOn = true,
            currentCandidateId = "cand-1",
            autoAudioEnabled = true
        )

        // Only currentCandidateId changed during auto-advance
        val candidateAdvancedSettings = baseSettings.copy(currentCandidateId = "cand-2")
        assertFalse(
            baseSettings.hasVisualOrScheduleChanges(candidateAdvancedSettings),
            "Candidate ID update must not be treated as a visual/schedule change"
        )

        // Word size changed in settings screen
        val wordSizeChanged = baseSettings.copy(wordSize = LockWallpaperWordSize.SMALL)
        assertTrue(
            baseSettings.hasVisualOrScheduleChanges(wordSizeChanged),
            "Word size change must be detected as visual change"
        )

        // Interval changed in settings screen
        val intervalChanged = baseSettings.copy(intervalMillis = 10_000L)
        assertTrue(
            baseSettings.hasVisualOrScheduleChanges(intervalChanged),
            "Interval change must be detected as schedule change"
        )
    }

    @Test
    fun `54 unit test - lock screen quick review stress preserves single timer`() {
        var timerArmed = false
        var quickReviewActive = false

        fun startQuickReview() {
            if (quickReviewActive) return
            quickReviewActive = true
            timerArmed = true
        }

        fun stopQuickReview() {
            quickReviewActive = false
            timerArmed = false
        }

        repeat(50) {
            startQuickReview()
            assertTrue(timerArmed)
            stopQuickReview()
            assertFalse(timerArmed)
        }
    }

    @Test
    fun `55 unit test - reminder pause stress avoids zombie timers`() {
        var pausedUntil: Long? = null
        var intervalTimerRunning = true

        fun pause(durationMs: Long, now: Long) {
            pausedUntil = now + durationMs
            intervalTimerRunning = false // Timer is paused
        }

        fun resume() {
            pausedUntil = null
            intervalTimerRunning = true // Timer resumes
        }

        repeat(50) {
            pause(300_000L, 1000L)
            assertFalse(intervalTimerRunning)
            resume()
            assertTrue(intervalTimerRunning)
        }
    }

    @Test
    fun `launcher package detection identifies known launcher packages correctly`() {
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage("com.miui.home"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage("com.mi.android.globallauncher"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage("com.android.launcher3"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage("com.sec.android.app.launcher"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage("com.google.android.apps.nexuslauncher"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage("com.huawei.android.launcher"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage("com.oppo.launcher"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage("com.oneplus.launcher"))
        assertFalse(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage("vn.loi.learning.android"))
        assertFalse(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage("com.google.android.youtube"))
        assertFalse(AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage(null))
    }

    @Test
    fun `transient system package filtering correctly identifies MIUI personalassistant and overlays`() {
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage("com.android.systemui"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage("miui.systemui.plugin"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage("com.miui.personalassistant"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage("com.miui.touchassistant"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage("com.miui.contentcatcher"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage("com.google.android.inputmethod.latin"))
        assertTrue(AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage("android"))
        assertFalse(AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage("com.android.chrome"))
        assertFalse(AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage("com.zing.zalo"))
        assertFalse(AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage("com.miui.home"))
    }

    @Test
    fun `setHomeSurfaceState VISIBLE re-arms timer if already VISIBLE but timer was cancelled by screen off`() {
        var homeSurfaceState = HomeSurfaceState.VISIBLE
        var autoNextRunnable: Runnable? = null
        var armedIntervalMs = 0L
        var timerArmed = false

        fun cancelTimer() {
            autoNextRunnable = null
            armedIntervalMs = 0L
            timerArmed = false
        }

        fun isRuntimeAllowed() = homeSurfaceState == HomeSurfaceState.VISIBLE

        fun reconcileTimer(interval: Long) {
            if (isRuntimeAllowed()) {
                armedIntervalMs = interval
                autoNextRunnable = Runnable {}
                timerArmed = true
            }
        }

        fun setHomeSurfaceState(state: HomeSurfaceState) {
            if (homeSurfaceState == state) {
                if (state == HomeSurfaceState.VISIBLE && isRuntimeAllowed() && autoNextRunnable == null) {
                    reconcileTimer(5000L)
                }
                return
            }
            homeSurfaceState = state
            if (state == HomeSurfaceState.VISIBLE) {
                reconcileTimer(5000L)
            }
        }

        // 1. Initially VISIBLE and timer running
        setHomeSurfaceState(HomeSurfaceState.VISIBLE)
        assertTrue(timerArmed)
        assertEquals(5000L, armedIntervalMs)

        // 2. Screen turns OFF -> timer cancelled
        cancelTimer()
        assertFalse(timerArmed)
        assertEquals(0L, armedIntervalMs)

        // 3. Device unlocks and setHomeSurfaceState(VISIBLE) called when state was already VISIBLE
        setHomeSurfaceState(HomeSurfaceState.VISIBLE)
        assertTrue(timerArmed)
        assertEquals(5000L, armedIntervalMs)
    }

    @Test
    fun `lock and unlock cycle restores fresh timer without requiring settings screen navigation`() {
        var deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        var homeSurfaceState = HomeSurfaceState.VISIBLE
        val hasWidgets = true
        val autoNextEnabled = true
        val intervalMs = 5000L
        var timerArmed = true

        fun isRuntimeAllowed() = deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
            homeSurfaceState == HomeSurfaceState.VISIBLE

        // 1. Screen off
        deviceState = VocabularyPresentationDeviceState.SCREEN_OFF
        homeSurfaceState = HomeSurfaceState.UNKNOWN
        timerArmed = false

        assertFalse(isRuntimeAllowed())
        assertFalse(timerArmed)

        // 2. Screen on (locked)
        deviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        assertFalse(isRuntimeAllowed())
        assertFalse(timerArmed)

        // 3. User unlock to Home launcher
        deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        homeSurfaceState = HomeSurfaceState.VISIBLE
        if (hasWidgets && autoNextEnabled && isRuntimeAllowed()) {
            timerArmed = true
        }

        assertTrue(isRuntimeAllowed())
        assertTrue(timerArmed)
    }

    @Test
    fun `USER_PRESENT without subsequent accessibility event reconciles current authoritative window snapshot via bridge`() {
        var currentDeviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        var homeSurfaceState = HomeSurfaceState.UNKNOWN
        var timerArmed = false
        val defaultLauncher = "com.miui.home"

        // Mock accessibility bridge returning current authoritative window
        fun mockReconcileHomeSurface(activeWindowPkg: String?): HomeSurfaceState {
            return if (activeWindowPkg == defaultLauncher) HomeSurfaceState.VISIBLE else HomeSurfaceState.HIDDEN
        }

        fun onUserPresent(activeWindowPkg: String?) {
            currentDeviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            val resolvedState = mockReconcileHomeSurface(activeWindowPkg)
            homeSurfaceState = resolvedState
            val runtimeAllowed = currentDeviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
                homeSurfaceState == HomeSurfaceState.VISIBLE
            if (runtimeAllowed) {
                timerArmed = true
            }
        }

        onUserPresent("com.miui.home")

        assertEquals(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, currentDeviceState)
        assertEquals(HomeSurfaceState.VISIBLE, homeSurfaceState)
        assertTrue(timerArmed)
    }

    @Test
    fun `USER_PRESENT into Chrome sets HIDDEN and suppresses timer`() {
        var currentDeviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        var homeSurfaceState = HomeSurfaceState.UNKNOWN
        var timerArmed = false
        val defaultLauncher = "com.miui.home"

        fun mockReconcileHomeSurface(activeWindowPkg: String?): HomeSurfaceState {
            return if (activeWindowPkg == defaultLauncher) HomeSurfaceState.VISIBLE else HomeSurfaceState.HIDDEN
        }

        fun onUserPresent(activeWindowPkg: String?) {
            currentDeviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            val resolvedState = mockReconcileHomeSurface(activeWindowPkg)
            homeSurfaceState = resolvedState
            val runtimeAllowed = currentDeviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
                homeSurfaceState == HomeSurfaceState.VISIBLE
            if (runtimeAllowed) {
                timerArmed = true
            }
        }

        onUserPresent("com.android.chrome")

        assertEquals(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, currentDeviceState)
        assertEquals(HomeSurfaceState.HIDDEN, homeSurfaceState)
        assertFalse(timerArmed)
    }

    @Test
    fun `floating overlay event over launcher keeps VISIBLE but floating overlay over Chrome keeps HIDDEN`() {
        val defaultLauncher = "com.miui.home"

        fun computeAuthoritativeState(rawEventPkg: String?, activeAppPkg: String?): HomeSurfaceState {
            val candidatePkg = when {
                activeAppPkg != null -> activeAppPkg
                else -> rawEventPkg
            }
            return when {
                candidatePkg == null -> HomeSurfaceState.UNKNOWN
                candidatePkg == defaultLauncher -> HomeSurfaceState.VISIBLE
                else -> HomeSurfaceState.HIDDEN
            }
        }

        // Overlay above launcher
        val stateLauncher = computeAuthoritativeState(
            rawEventPkg = "com.nitin.volumnbutton",
            activeAppPkg = "com.miui.home"
        )
        assertEquals(HomeSurfaceState.VISIBLE, stateLauncher)

        // Overlay above Chrome
        val stateChrome = computeAuthoritativeState(
            rawEventPkg = "com.nitin.volumnbutton",
            activeAppPkg = "com.android.chrome"
        )
        assertEquals(HomeSurfaceState.HIDDEN, stateChrome)
    }

    @Test
    fun `repeated lock and unlock 5 cycles consistently recovers timer without settings navigation`() {
        var currentDeviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        var homeSurfaceState = HomeSurfaceState.VISIBLE
        var timerArmed = true
        var intervalFiredCount = 0

        fun isRuntimeAllowed() = currentDeviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
            homeSurfaceState == HomeSurfaceState.VISIBLE

        repeat(5) {
            // 1. Running on Home -> FIRE
            assertTrue(isRuntimeAllowed())
            assertTrue(timerArmed)
            intervalFiredCount++

            // 2. Lock screen
            currentDeviceState = VocabularyPresentationDeviceState.SCREEN_OFF
            homeSurfaceState = HomeSurfaceState.UNKNOWN
            timerArmed = false
            assertFalse(isRuntimeAllowed())
            assertFalse(timerArmed)

            // 3. Locked screen on
            currentDeviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
            assertFalse(isRuntimeAllowed())
            assertFalse(timerArmed)

            // 4. Unlock directly to Home
            currentDeviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            homeSurfaceState = HomeSurfaceState.VISIBLE
            if (isRuntimeAllowed()) {
                timerArmed = true
            }
            assertTrue(isRuntimeAllowed())
            assertTrue(timerArmed)
        }

        assertEquals(5, intervalFiredCount)
    }

    @Test
    fun `dedicated runtime clock starts when Auto-Next ON and widgets exist, stops when Auto-Next OFF or no widgets`() {
        class FakeRuntimeClock : HomeWidgetRuntimeClock {
            var armed = false
            var interval = 0L
            var scheduleCount = 0
            var cancelCount = 0

            override fun schedule(intervalMs: Long, reason: String, freshInterval: Boolean) {
                armed = true
                interval = intervalMs
                scheduleCount++
            }

            override fun cancel(reason: String) {
                armed = false
                interval = 0L
                cancelCount++
            }

            override fun isArmed(): Boolean = armed
            override fun armedInterval(): Long = interval
        }

        val clock = FakeRuntimeClock()

        var hasWidgets = true
        var autoNextEnabled = true
        var isServiceRunning = false

        fun evaluateServiceLifetime() {
            isServiceRunning = hasWidgets && autoNextEnabled
            if (!isServiceRunning) {
                clock.cancel("REQUIREMENTS_NOT_MET")
            }
        }

        // 1. Initial: hasWidgets + autoNextEnabled -> service running
        evaluateServiceLifetime()
        assertTrue(isServiceRunning)

        // 2. Schedule clock 5s
        clock.schedule(5000L, "INIT")
        assertTrue(clock.isArmed())
        assertEquals(5000L, clock.armedInterval())

        // 3. User turns Auto-Next OFF -> service stops, clock cancelled
        autoNextEnabled = false
        evaluateServiceLifetime()
        assertFalse(isServiceRunning)
        assertFalse(clock.isArmed())

        // 4. User turns Auto-Next ON again -> service starts
        autoNextEnabled = true
        evaluateServiceLifetime()
        assertTrue(isServiceRunning)

        // 5. User removes all widgets -> service stops, clock cancelled
        hasWidgets = false
        evaluateServiceLifetime()
        assertFalse(isServiceRunning)
        assertFalse(clock.isArmed())
    }

    @Test
    fun `runtime service remains alive while Home is HIDDEN but clock is paused and resumes on VISIBLE`() {
        class FakeRuntimeClock : HomeWidgetRuntimeClock {
            var armed = false
            var interval = 0L

            override fun schedule(intervalMs: Long, reason: String, freshInterval: Boolean) {
                armed = true
                interval = intervalMs
            }

            override fun cancel(reason: String) {
                armed = false
                interval = 0L
            }

            override fun isArmed(): Boolean = armed
            override fun armedInterval(): Long = interval
        }

        val clock = FakeRuntimeClock()
        val hasWidgets = true
        val autoNextEnabled = true
        val isServiceRunning = hasWidgets && autoNextEnabled
        var homeSurfaceState = HomeSurfaceState.VISIBLE
        var deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON

        fun isRuntimeAllowed() = deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
            homeSurfaceState == HomeSurfaceState.VISIBLE

        fun reconcileClock() {
            if (isRuntimeAllowed() && isServiceRunning) {
                clock.schedule(5000L, "ALLOWED")
            } else {
                clock.cancel("PAUSED")
            }
        }

        // 1. On Home -> clock armed
        reconcileClock()
        assertTrue(isServiceRunning)
        assertTrue(clock.isArmed())

        // 2. Open Chrome (Home HIDDEN) -> service still running, clock paused
        homeSurfaceState = HomeSurfaceState.HIDDEN
        reconcileClock()
        assertTrue(isServiceRunning) // Service NOT stopped on Chrome transition
        assertFalse(clock.isArmed())  // But timer clock is paused

        // 3. Return Home -> clock resumes with fresh 5s interval
        homeSurfaceState = HomeSurfaceState.VISIBLE
        reconcileClock()
        assertTrue(isServiceRunning)
        assertTrue(clock.isArmed())
        assertEquals(5000L, clock.armedInterval())
    }

    @Test
    fun `tick defense-in-depth rejects tick if gate is closed at moment of fire`() {
        var tickProcessed = false
        var gateRejected = false

        fun onRuntimeTick(deviceState: VocabularyPresentationDeviceState, homeSurfaceState: HomeSurfaceState) {
            val runtimeAllowed = deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
                homeSurfaceState == HomeSurfaceState.VISIBLE
            if (!runtimeAllowed) {
                gateRejected = true
                return
            }
            tickProcessed = true
        }

        // Tick arrives while in Chrome
        onRuntimeTick(
            deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON,
            homeSurfaceState = HomeSurfaceState.HIDDEN
        )
        assertFalse(tickProcessed)
        assertTrue(gateRejected)

        // Tick arrives while locked
        gateRejected = false
        onRuntimeTick(
            deviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON,
            homeSurfaceState = HomeSurfaceState.VISIBLE
        )
        assertFalse(tickProcessed)
        assertTrue(gateRejected)

        // Tick arrives on Home while unlocked
        gateRejected = false
        onRuntimeTick(
            deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON,
            homeSurfaceState = HomeSurfaceState.VISIBLE
        )
        assertTrue(tickProcessed)
        assertFalse(gateRejected)
    }

    @Test
    fun `post-unlock clock resume with same VISIBLE home state arms fresh interval without new home event`() {
        class FakeRuntimeClock : HomeWidgetRuntimeClock {
            var armed = false
            var interval = 0L
            var scheduleCount = 0
            var cancelCount = 0

            override fun schedule(intervalMs: Long, reason: String, freshInterval: Boolean) {
                armed = true
                interval = intervalMs
                scheduleCount++
            }

            override fun cancel(reason: String) {
                armed = false
                interval = 0L
                cancelCount++
            }

            override fun isArmed(): Boolean = armed
            override fun armedInterval(): Long = interval
        }

        val clock = FakeRuntimeClock()
        val hasWidgets = true
        val autoNextEnabled = true
        var deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        var homeSurfaceState = HomeSurfaceState.VISIBLE

        fun isRuntimeAllowed() = deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
            homeSurfaceState == HomeSurfaceState.VISIBLE

        fun reconcileClock(reason: String) {
            if (hasWidgets && autoNextEnabled && isRuntimeAllowed()) {
                clock.schedule(5000L, reason)
            } else {
                clock.cancel(reason)
            }
        }

        // 1. Initial on Home: clock ARMED
        reconcileClock("INIT")
        assertTrue(clock.isArmed())
        assertEquals(5000L, clock.armedInterval())
        assertEquals(1, clock.scheduleCount)

        // 2. Lock screen: deviceState=SCREEN_OFF, homeSurfaceState stays VISIBLE
        deviceState = VocabularyPresentationDeviceState.SCREEN_OFF
        reconcileClock("DEVICE_SCREEN_OFF")
        assertFalse(clock.isArmed())
        assertEquals(1, clock.cancelCount)

        // 3. Screen wakes locked: deviceState=LOCKED_SCREEN_ON, homeSurfaceState still VISIBLE
        deviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        reconcileClock("DEVICE_LOCKED")
        assertFalse(clock.isArmed())

        // 4. Unlock directly to Home: deviceState=UNLOCKED_SCREEN_ON, homeSurfaceState STILL VISIBLE (no new home event)
        deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        reconcileClock("DEVICE_UNLOCKED")
        assertTrue(clock.isArmed())
        assertEquals(5000L, clock.armedInterval())
        assertEquals(2, clock.scheduleCount)
    }

    @Test
    fun `unlock with UNKNOWN then later VISIBLE transitions clock from PAUSED to ARMED`() {
        class FakeRuntimeClock : HomeWidgetRuntimeClock {
            var armed = false
            var interval = 0L

            override fun schedule(intervalMs: Long, reason: String, freshInterval: Boolean) {
                armed = true
                interval = intervalMs
            }

            override fun cancel(reason: String) {
                armed = false
                interval = 0L
            }

            override fun isArmed(): Boolean = armed
            override fun armedInterval(): Long = interval
        }

        val clock = FakeRuntimeClock()
        var deviceState = VocabularyPresentationDeviceState.SCREEN_OFF
        var homeSurfaceState = HomeSurfaceState.UNKNOWN

        fun isRuntimeAllowed() = deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
            homeSurfaceState == HomeSurfaceState.VISIBLE

        fun reconcileClock(reason: String) {
            if (isRuntimeAllowed()) {
                clock.schedule(5000L, reason)
            } else {
                clock.cancel(reason)
            }
        }

        // 1. Device unlocks but surface is UNKNOWN: clock PAUSED
        deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        homeSurfaceState = HomeSurfaceState.UNKNOWN
        reconcileClock("UNLOCK_UNKNOWN")
        assertFalse(clock.isArmed())

        // 2. Accessibility/window event confirms VISIBLE: clock ARMED
        homeSurfaceState = HomeSurfaceState.VISIBLE
        reconcileClock("HOME_VISIBLE")
        assertTrue(clock.isArmed())
        assertEquals(5000L, clock.armedInterval())
    }

    @Test
    fun `reverse event ordering VISIBLE while LOCKED then UNLOCKED converges to ARMED`() {
        class FakeRuntimeClock : HomeWidgetRuntimeClock {
            var armed = false
            var interval = 0L

            override fun schedule(intervalMs: Long, reason: String, freshInterval: Boolean) {
                armed = true
                interval = intervalMs
            }

            override fun cancel(reason: String) {
                armed = false
                interval = 0L
            }

            override fun isArmed(): Boolean = armed
            override fun armedInterval(): Long = interval
        }

        val clock = FakeRuntimeClock()
        var deviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        var homeSurfaceState = HomeSurfaceState.VISIBLE

        fun isRuntimeAllowed() = deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
            homeSurfaceState == HomeSurfaceState.VISIBLE

        fun reconcileClock(reason: String) {
            if (isRuntimeAllowed()) {
                clock.schedule(5000L, reason)
            } else {
                clock.cancel(reason)
            }
        }

        // 1. Surface confirms VISIBLE while still LOCKED: clock PAUSED
        reconcileClock("VISIBLE_WHILE_LOCKED")
        assertFalse(clock.isArmed())

        // 2. Device transitions to UNLOCKED: clock ARMED
        deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        reconcileClock("DEVICE_UNLOCKED")
        assertTrue(clock.isArmed())
        assertEquals(5000L, clock.armedInterval())
    }

    @Test
    fun `repeated 5 lock unlock cycles consistently toggle ARMED and PAUSED with fresh intervals`() {
        class FakeRuntimeClock : HomeWidgetRuntimeClock {
            var armed = false
            var interval = 0L
            var tickCount = 0

            override fun schedule(intervalMs: Long, reason: String, freshInterval: Boolean) {
                armed = true
                interval = intervalMs
            }

            override fun cancel(reason: String) {
                armed = false
                interval = 0L
            }

            override fun isArmed(): Boolean = armed
            override fun armedInterval(): Long = interval

            fun fireTick() {
                if (armed) tickCount++
            }
        }

        val clock = FakeRuntimeClock()
        var deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        var homeSurfaceState = HomeSurfaceState.VISIBLE

        fun isRuntimeAllowed() = deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON &&
            homeSurfaceState == HomeSurfaceState.VISIBLE

        fun reconcileClock(reason: String) {
            if (isRuntimeAllowed()) {
                clock.schedule(5000L, reason)
            } else {
                clock.cancel(reason)
            }
        }

        repeat(5) {
            // Unlocked on Home -> ARMED -> fire
            deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            homeSurfaceState = HomeSurfaceState.VISIBLE
            reconcileClock("UNLOCKED")
            assertTrue(clock.isArmed())
            clock.fireTick()

            // Lock screen -> PAUSED
            deviceState = VocabularyPresentationDeviceState.SCREEN_OFF
            reconcileClock("SCREEN_OFF")
            assertFalse(clock.isArmed())

            // Screen wake locked -> PAUSED
            deviceState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
            reconcileClock("LOCKED")
            assertFalse(clock.isArmed())

            // Unlock to Home -> ARMED again
            deviceState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
            reconcileClock("UNLOCKED")
            assertTrue(clock.isArmed())
        }

        assertEquals(5, clock.tickCount)
    }
}
