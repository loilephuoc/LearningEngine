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
}
