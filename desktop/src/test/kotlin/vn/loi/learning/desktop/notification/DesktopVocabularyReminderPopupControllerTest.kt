package vn.loi.learning.desktop.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import java.util.concurrent.CountDownLatch
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class DesktopVocabularyReminderPopupControllerTest {
    @Test
    fun `autoplay starts once per generation and hover state updates never replay`() {
        val fixture = Fixture()
        fixture.controller.dispatch(candidate("audio", "audio/ref"), 8_000, true)
        assertEquals(listOf("audio/ref"), fixture.audio.starts)
        fixture.controller.pointerEntered()
        fixture.controller.pointerExited()
        assertEquals(listOf("audio/ref"), fixture.audio.starts)
    }

    @Test
    fun `audio off or missing stays silent and every hide path stops playback`() {
        val fixture = Fixture()
        fixture.controller.dispatch(candidate("off", "audio/ref"), 8_000, false)
        assertEquals(emptyList(), fixture.audio.starts)
        fixture.controller.closePopup()
        assertTrue(fixture.audio.stops >= 1)

        fixture.controller.dispatch(candidate("missing"), 8_000, true)
        assertEquals(emptyList(), fixture.audio.starts)
        fixture.timer.fireActive()
        val stopsAfterTimer = fixture.audio.stops
        fixture.controller.dispatch(candidate("invalidate", "audio/next"), 8_000, true)
        fixture.controller.invalidate()
        assertTrue(fixture.audio.stops > stopsAfterTimer)
        fixture.controller.close()
        assertTrue(fixture.audio.closed)
    }

    @Test
    fun `playback failure cannot prevent popup and next generation starts only after prior stop`() {
        val fixture = Fixture()
        fixture.audio.failStart = true
        fixture.controller.dispatch(candidate("failure", "bad/ref"), 8_000, true)
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        fixture.controller.closePopup()
        fixture.audio.failStart = false
        val stopsBeforeNext = fixture.audio.stops
        fixture.controller.dispatch(candidate("next", "good/ref"), 8_000, true)
        assertEquals(listOf("good/ref"), fixture.audio.starts)
        assertTrue(stopsBeforeNext >= 1)
    }

    @Test
    fun `manual audio play stop and difficult marker toggle update current popup only`() {
        val fixture = Fixture()
        fixture.controller.dispatch(candidate("actions", "audio/ref"), 8_000, false)
        var visible = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertTrue(visible.audioAvailable)
        assertFalse(visible.audioPlaying)
        assertFalse(visible.markedDifficult)
        fixture.controller.toggleAudio()
        visible = assertIs(fixture.controller.state.value)
        assertTrue(visible.audioPlaying)
        assertEquals(listOf("audio/ref"), fixture.audio.starts)
        fixture.controller.toggleAudio()
        assertFalse(assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value).audioPlaying)
        fixture.controller.toggleDifficultMarker()
        assertTrue(assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value).markedDifficult)
        fixture.controller.toggleDifficultMarker()
        assertFalse(assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value).markedDifficult)
    }

    @Test
    fun `quick mute persists global autoplay and next popup remains silent`() {
        val authority = FakeAutoPlayAuthority(true)
        val fixture = Fixture(authority)
        fixture.controller.dispatch(candidate("first", "audio/ref"), 8_000, true)
        assertTrue(assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value).autoPlayPronunciation)

        fixture.controller.toggleAudio()

        assertFalse(authority.enabled)
        assertEquals(1, authority.updates)
        assertTrue(fixture.audio.stops >= 1)
        assertFalse(assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value).autoPlayPronunciation)
        fixture.controller.closePopup()
        fixture.controller.dispatch(candidate("next", "audio/next"), 8_000, authority.enabled)
        assertEquals(listOf("audio/ref"), fixture.audio.starts)
    }

    @Test
    fun `quick unmute persists autoplay and plays current audio exactly once`() {
        val authority = FakeAutoPlayAuthority(false)
        val fixture = Fixture(authority)
        fixture.controller.dispatch(candidate("current", "audio/ref"), 8_000, false)

        fixture.controller.toggleAudio()
        fixture.controller.pointerEntered()
        fixture.controller.pointerExited()

        assertTrue(authority.enabled)
        assertEquals(listOf("audio/ref"), fixture.audio.starts)
        assertTrue(assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value).autoPlayPronunciation)
    }

    @Test
    fun `failed quick toggle keeps authoritative visual state and does not crash`() {
        val authority = FakeAutoPlayAuthority(true).apply { fail = true }
        val fixture = Fixture(authority)
        fixture.controller.dispatch(candidate("failure", "audio/ref"), 8_000, true)

        fixture.controller.toggleAudio()

        assertTrue(authority.enabled)
        assertTrue(assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value).autoPlayPronunciation)
        assertTrue(fixture.audio.stops >= 1)
    }

    @Test
    fun `missing audio still permits persistent global mute`() {
        val authority = FakeAutoPlayAuthority(true)
        val fixture = Fixture(authority)
        fixture.controller.dispatch(candidate("missing"), 8_000, true)
        fixture.controller.toggleAudio()
        assertFalse(authority.enabled)
        assertFalse(assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value).autoPlayPronunciation)
    }

    @Test
    fun `thumbnail opens one full viewer takes loop ownership and close stops it`() {
        val fixture = Fixture()
        fixture.controller.dispatch(candidate("viewer", "audio/ref").copy(imageReference = "image/ref"), 8_000, true)
        fixture.controller.openFullImage()
        val viewer = assertIs<DesktopVocabularyReminderPopupState.FullImage>(fixture.controller.state.value)
        assertEquals("image/ref", viewer.candidate.imageReference)
        assertEquals(listOf("audio/ref"), fixture.audio.loops)
        fixture.audio.reportPlaying(false)
        assertIs<DesktopVocabularyReminderPopupState.FullImage>(fixture.controller.state.value)
        fixture.timer.fireActiveEvenIfCancelled()
        assertIs<DesktopVocabularyReminderPopupState.FullImage>(fixture.controller.state.value)
        assertTrue(fixture.controller.isReminderActive)
        val starts = fixture.audio.starts.size
        fixture.controller.openFullImage()
        assertEquals(starts, fixture.audio.starts.size)
        fixture.controller.closeFullImage()
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
        assertTrue(fixture.audio.stops >= 2)
        assertFalse(fixture.controller.isReminderActive)
    }

    @Test
    fun `viewer opens without audio and global mute remains unchanged`() {
        val authority = FakeAutoPlayAuthority(false)
        val fixture = Fixture(authority)
        fixture.controller.dispatch(candidate("viewer").copy(imageReference = "image/ref"), 8_000, false)
        fixture.controller.openFullImage()
        assertIs<DesktopVocabularyReminderPopupState.FullImage>(fixture.controller.state.value)
        assertFalse(authority.enabled)
        assertTrue(fixture.audio.loops.isEmpty())
    }
    @Test
    fun `controller starts hidden and close is idempotent`() {
        val fixture = Fixture()
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
        fixture.controller.closePopup()
        fixture.controller.closePopup()
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `dispatch shows one candidate and overlap is rejected until hidden`() {
        val fixture = Fixture()

        fixture.controller.dispatch(candidate("first"), 10_000, false)
        fixture.controller.dispatch(candidate("overlap"), 10_000, false)

        val visible = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertEquals("first", visible.candidate.primaryText)
        assertEquals(10_000L, fixture.timer.active.single().delayMillis)
        assertTrue(fixture.controller.isReminderActive)

        fixture.timer.fireActive()
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
        assertFalse(fixture.controller.isReminderActive)
    }

    @Test
    fun `hover pauses remaining duration and repeated enter exit cycles resume it`() {
        val fixture = Fixture()
        fixture.controller.dispatch(candidate("word"), 10_000, false)
        fixture.clock.now = 4_000L

        fixture.controller.pointerEntered()
        var visible = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertEquals(6_000L, visible.remainingMillis)
        assertTrue(visible.hovered)
        assertEquals(0, fixture.timer.active.size)

        fixture.clock.now = 20_000L
        fixture.controller.pointerExited()
        assertEquals(6_000L, fixture.timer.active.single().delayMillis)
        fixture.clock.now = 22_500L
        fixture.controller.pointerEntered()
        visible = assertIs(fixture.controller.state.value)
        assertEquals(3_500L, visible.remainingMillis)

        fixture.controller.pointerExited()
        fixture.timer.fireActive()
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `manual close invalidation and shutdown cancel stale callbacks safely`() {
        val fixture = Fixture()
        fixture.controller.dispatch(candidate("old"), 10_000, false)
        val oldTask = fixture.timer.tasks.single()
        fixture.controller.closePopup()
        fixture.controller.dispatch(candidate("new"), 8_000, false)

        oldTask.fireEvenIfCancelled()
        assertEquals(
            "new",
            assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value).candidate.primaryText
        )

        fixture.controller.invalidate()
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
        fixture.controller.dispatch(candidate("after invalidate"), 7_000, false)
        fixture.controller.close()
        assertTrue(fixture.timer.closed)
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
        fixture.controller.dispatch(candidate("after close"), 7_000, false)
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `concurrent dispatch atomically admits exactly one candidate`() {
        val fixture = Fixture()
        val ready = CountDownLatch(16)
        val start = CountDownLatch(1)
        val threads = (1..16).map { index ->
            Thread {
                ready.countDown()
                start.await()
                fixture.controller.dispatch(candidate("word-$index"), 10_000, false)
            }.also(Thread::start)
        }
        ready.await()
        start.countDown()
        threads.forEach(Thread::join)

        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertEquals(1, fixture.timer.active.size)
    }

    @Test
    fun `leaving hover with no remaining time hides immediately`() {
        val fixture = Fixture()
        fixture.controller.dispatch(candidate("word"), 3_000, false)
        fixture.clock.now = 3_000L
        fixture.controller.pointerEntered()
        fixture.controller.pointerExited()
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `fractional duration and remaining hover time are not truncated`() {
        val fixture = Fixture()
        fixture.controller.dispatch(candidate("fractional"), 2_500, false)
        assertEquals(2_500L, fixture.timer.active.single().delayMillis)
        fixture.clock.now = 1_000L
        fixture.controller.pointerEntered()
        assertEquals(1_500L, assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value).remainingMillis)
        fixture.controller.pointerExited()
        assertEquals(1_500L, fixture.timer.active.single().delayMillis)
    }

    @Test
    fun `minimum and maximum duration boundaries schedule exactly`() {
        val minimum = Fixture()
        minimum.controller.dispatch(candidate("minimum"), 1_500, false)
        assertEquals(1_500L, minimum.timer.active.single().delayMillis)
        val maximum = Fixture()
        maximum.controller.dispatch(candidate("maximum"), 60_000, false)
        assertEquals(60_000L, maximum.timer.active.single().delayMillis)
    }

    private class Fixture(
        authority: FakeAutoPlayAuthority? = null,
        val layoutAuthority: FakeLayoutAuthority = FakeLayoutAuthority(),
        val snoozeAuthority: FakeSnoozeAuthority = FakeSnoozeAuthority()
    ) {
        val clock = FakeClock()
        val timer = FakeTimer()
        val audio = FakeAudio()
        val markers = FakeMarkers()
        val controller = DesktopVocabularyReminderPopupController(
            uiDispatcher = DesktopVocabularyReminderUiDispatcher { it() },
            clock = clock,
            timer = timer,
            audio = audio,
            difficultMarkers = markers,
            autoPlayAuthority = authority,
            layoutAuthority = layoutAuthority,
            snoozeAuthority = snoozeAuthority
        )
    }

    private class FakeLayoutAuthority : DesktopVocabularyReminderLayoutAuthority {
        val updates = mutableListOf<DesktopVocabularyReminderPopupLayout>()
        override fun update(layout: DesktopVocabularyReminderPopupLayout): Boolean {
            updates.add(layout)
            return true
        }
    }

    private class FakeSnoozeAuthority : DesktopVocabularyReminderSnoozeAuthority {
        val snoozes = mutableListOf<Int>()
        override fun snooze(durationMinutes: Int): Boolean {
            snoozes.add(durationMinutes)
            return true
        }
    }

    private class FakeAutoPlayAuthority(var enabled: Boolean) : DesktopVocabularyReminderAutoPlayAuthority {
        var updates = 0
        var fail = false
        override fun current() = enabled
        override fun update(enabled: Boolean): Boolean {
            updates++
            if (fail) return false
            this.enabled = enabled
            return true
        }
    }

    private class FakeMarkers : DesktopVocabularyReminderDifficultMarkers {
        private val ids = mutableSetOf<ContentId>()
        override fun isMarked(contentId: ContentId) = contentId in ids
        override fun markedContentIds(): Set<ContentId> = ids.toSet()
        override fun toggle(contentId: ContentId): Boolean = if (ids.add(contentId)) true else { ids.remove(contentId); false }
    }

    @Test
    fun `toggleLayout switches layout immediately and updates authority without restarting playback or candidate`() {
        val fixture = Fixture()
        fixture.controller.dispatch(
            candidate = candidate("toggle-word", audioReference = "en.mp3"),
            displayDurationMillis = 10_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT
        )
        val initial = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertEquals(DesktopVocabularyReminderPopupLayout.COMPACT, initial.popupLayout)
        assertEquals(1, fixture.audio.starts.size)

        fixture.controller.toggleLayout()

        val toggled = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertEquals(DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL, toggled.popupLayout)
        assertEquals("toggle-word", toggled.candidate.primaryText)
        assertEquals(listOf(DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL), fixture.layoutAuthority.updates)
        assertEquals(1, fixture.audio.starts.size, "Audio must not restart on layout toggle")

        fixture.controller.toggleLayout()

        val toggledBack = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertEquals(DesktopVocabularyReminderPopupLayout.COMPACT, toggledBack.popupLayout)
        assertEquals(listOf(DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL, DesktopVocabularyReminderPopupLayout.COMPACT), fixture.layoutAuthority.updates)
    }

    @Test
    fun `snooze triggers snoozeAuthority and immediately closes popup and stops audio`() {
        val fixture = Fixture()
        fixture.controller.dispatch(
            candidate = candidate("snooze-word", audioReference = "en.mp3"),
            displayDurationMillis = 10_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT
        )
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)

        fixture.controller.snooze(5)

        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
        assertEquals(listOf(5), fixture.snoozeAuthority.snoozes)
        assertTrue(fixture.audio.stops >= 1)

        fixture.controller.dispatch(
            candidate = candidate("snooze-word-2", audioReference = "en.mp3"),
            displayDurationMillis = 10_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT
        )
        fixture.controller.snooze(45)
        assertEquals(listOf(5, 45), fixture.snoozeAuthority.snoozes)
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `dragStarted pauses auto-hide countdown and dragEnded resumes remaining duration`() {
        val fixture = Fixture()
        fixture.clock.now = 1_000L
        fixture.controller.dispatch(
            candidate = candidate("drag-test"),
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = false,
            popupLocation = DesktopVocabularyReminderPopupLocation(customPosition = true)
        )
        val initialVisible = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertEquals(8_000L, initialVisible.remainingMillis)
        assertFalse(initialVisible.dragging)
        assertEquals(1, fixture.timer.active.size)

        // Advance clock by 3 seconds and start dragging
        fixture.clock.now = 4_000L
        fixture.controller.dragStarted()

        val draggingVisible = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertTrue(draggingVisible.dragging)
        assertEquals(5_000L, draggingVisible.remainingMillis)
        assertEquals(0, fixture.timer.active.size) // Hide task was cancelled

        // Finish dragging after another 2 seconds
        fixture.clock.now = 6_000L
        fixture.controller.dragEnded()

        val afterDragVisible = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertFalse(afterDragVisible.dragging)
        assertEquals(1, fixture.timer.active.size) // Rescheduled with remaining duration (5000ms)
        assertEquals(5_000L, fixture.timer.active.first().delayMillis)

        // Fire timer
        fixture.timer.fireActive()
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `case 1 english completes then delay fires then vietnamese starts`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        assertEquals(listOf("en.mp3"), fixture.audio.starts)
        // English completes
        fixture.audio.completeLatest()
        // Delay task is scheduled
        val delayTask = fixture.timer.active.first { it.delayMillis == 2_000L }
        delayTask.fireEvenIfCancelled()
        assertEquals(listOf("en.mp3", "vi.mp3"), fixture.audio.starts)
    }

    @Test
    fun `case 2 english off and vietnamese on starts delay immediately and plays vietnamese`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = false,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 1_500L
        )
        assertEquals(emptyList(), fixture.audio.starts)
        val delayTask = fixture.timer.active.first { it.delayMillis == 1_500L }
        delayTask.fireEvenIfCancelled()
        assertEquals(listOf("vi.mp3"), fixture.audio.starts)
    }

    @Test
    fun `case 3 english missing and vietnamese on starts delay immediately and plays vietnamese`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = null, translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        assertEquals(emptyList(), fixture.audio.starts)
        val delayTask = fixture.timer.active.first { it.delayMillis == 2_000L }
        delayTask.fireEvenIfCancelled()
        assertEquals(listOf("vi.mp3"), fixture.audio.starts)
    }

    @Test
    fun `case 4 vietnamese disabled never starts translated audio`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = false,
            vietnameseAudioDelayMillis = 2_000L
        )
        assertEquals(listOf("en.mp3"), fixture.audio.starts)
        fixture.audio.completeLatest()
        assertTrue(fixture.timer.active.none { it.delayMillis == 2_000L })
    }

    @Test
    fun `case 5 vietnamese reference missing fails silently with no translated playback`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = null)
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        assertEquals(listOf("en.mp3"), fixture.audio.starts)
        fixture.audio.completeLatest()
        assertTrue(fixture.timer.active.none { it.delayMillis == 2_000L })
    }

    @Test
    fun `case 6 popup closes during delay cancels vietnamese playback`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        fixture.audio.completeLatest()
        val delayTask = fixture.timer.active.first { it.delayMillis == 2_000L }
        fixture.controller.closePopup()
        assertTrue(delayTask.cancelled)
        delayTask.fireEvenIfCancelled()
        assertEquals(listOf("en.mp3"), fixture.audio.starts)
    }

    @Test
    fun `case 7 mute during delay cancels vietnamese playback`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        fixture.audio.completeLatest()
        val delayTask = fixture.timer.active.first { it.delayMillis == 2_000L }
        fixture.controller.toggleAudio() // Mute
        assertTrue(delayTask.cancelled)
        delayTask.fireEvenIfCancelled()
        assertEquals(listOf("en.mp3"), fixture.audio.starts)
    }

    @Test
    fun `case 8 old generation completion or delay firing after new generation does not play audio`() {
        val fixture = Fixture()
        val candidate1 = candidate("test1", audioReference = "en1.mp3", translatedAudioReference = "vi1.mp3")
        fixture.controller.dispatch(
            candidate = candidate1,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        fixture.controller.closePopup()

        val candidate2 = candidate("test2", audioReference = "en2.mp3", translatedAudioReference = "vi2.mp3")
        fixture.controller.dispatch(
            candidate = candidate2,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = false,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = false
        )
        // Fire old completion
        fixture.audio.completeLatest()
        assertEquals(listOf("en1.mp3"), fixture.audio.starts)
    }

    @Test
    fun `case 9 open full image before vietnamese fires cancels pending vietnamese`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3", imageReference = "img.png")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        fixture.audio.completeLatest()
        val delayTask = fixture.timer.active.first { it.delayMillis == 2_000L }
        fixture.controller.openFullImage()
        assertTrue(delayTask.cancelled)
        delayTask.fireEvenIfCancelled()
        assertEquals(listOf("en.mp3"), fixture.audio.starts)
        assertEquals(listOf("en.mp3"), fixture.audio.loops)
    }

    @Test
    fun `case 10 hover and drag do not cause duplicate audio`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        fixture.controller.pointerEntered()
        fixture.controller.pointerExited()
        fixture.controller.dragStarted()
        fixture.controller.dragEnded()
        assertEquals(listOf("en.mp3"), fixture.audio.starts)
    }

    @Test
    fun `case 11 unmute preserves english replay behavior but does not immediately trigger vietnamese`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = false,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        assertEquals(emptyList(), fixture.audio.starts)
        fixture.controller.toggleAudio() // Unmute
        assertEquals(listOf("en.mp3"), fixture.audio.starts)
        // Confirm no instant vietnamese
        assertFalse(fixture.audio.starts.contains("vi.mp3"))
    }

    @Test
    fun `auto-hide test A vietnamese playing when display timer expires defers hide until completion`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 5_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        // English plays and completes
        fixture.audio.reportPlaying(true)
        fixture.audio.completeLatest()
        fixture.audio.reportPlaying(false)

        // Delay timer fires, Vietnamese starts
        val delayTask = fixture.timer.active.first { it.delayMillis == 2_000L }
        delayTask.fireEvenIfCancelled()
        assertEquals(listOf("en.mp3", "vi.mp3"), fixture.audio.starts)
        fixture.audio.reportPlaying(true)

        // Display hide timer (5000ms) fires while Vietnamese is still playing
        val hideTask = fixture.timer.active.first { it.delayMillis == 5_000L }
        hideTask.fireEvenIfCancelled()

        // Popup MUST remain visible and audio MUST NOT stop
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
        assertEquals(0, fixture.audio.stops)

        // Vietnamese completes
        fixture.audio.completeLatest()
        fixture.audio.reportPlaying(false)

        // Popup now becomes Hidden
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `auto-hide test B vietnamese delay pending when display timer expires defers hide until completion`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 2_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 3_000L
        )
        // English completes
        fixture.audio.completeLatest()
        fixture.audio.reportPlaying(false)

        // Delay is pending (3000ms). Display timer (2000ms) fires
        val hideTask = fixture.timer.active.first { it.delayMillis == 2_000L }
        hideTask.fireEvenIfCancelled()

        // Popup remains visible
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)

        // Delay fires, Vietnamese starts
        val delayTask = fixture.timer.active.first { it.delayMillis == 3_000L }
        delayTask.fireEvenIfCancelled()
        assertEquals(listOf("en.mp3", "vi.mp3"), fixture.audio.starts)
        fixture.audio.reportPlaying(true)

        // Still visible
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)

        // Vietnamese completes
        fixture.audio.completeLatest()
        fixture.audio.reportPlaying(false)
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `auto-hide test C english still playing when display timer expires defers hide`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = null)
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 2_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = false
        )
        fixture.audio.reportPlaying(true)

        // Display timer fires while English is playing
        val hideTask = fixture.timer.active.first { it.delayMillis == 2_000L }
        hideTask.fireEvenIfCancelled()
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)

        // English completes
        fixture.audio.completeLatest()
        fixture.audio.reportPlaying(false)
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `auto-hide test D display duration longer than all audio hides on display timer`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 10_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 1_000L
        )
        // English completes
        fixture.audio.completeLatest()
        // Delay fires
        val delayTask = fixture.timer.active.first { it.delayMillis == 1_000L }
        delayTask.fireEvenIfCancelled()
        // Vietnamese completes
        fixture.audio.completeLatest()
        fixture.audio.reportPlaying(false)

        // Audio sequence is done at ~3s, but display duration is 10s: popup remains visible
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)

        // Display timer fires at 10s
        val hideTask = fixture.timer.active.first { it.delayMillis == 10_000L }
        hideTask.fireEvenIfCancelled()
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `auto-hide test E manual close while audio playing stops immediately and hides`() {
        val fixture = Fixture()
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 10_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        fixture.audio.reportPlaying(true)
        fixture.controller.closePopup()

        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
        assertTrue(fixture.audio.stops >= 1)
    }

    @Test
    fun `auto-hide test F mute during extended lifetime stops audio and hides immediately if deadline expired`() {
        val authority = FakeAutoPlayAuthority(true)
        val fixture = Fixture(authority)
        val candidate = candidate("test", audioReference = "en.mp3", translatedAudioReference = "vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 3_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        // English completes
        fixture.audio.completeLatest()
        // Display deadline expires (3000ms)
        val hideTask = fixture.timer.active.first { it.delayMillis == 3_000L }
        hideTask.fireEvenIfCancelled()
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)

        // Delay task fires, Vietnamese starts
        val delayTask = fixture.timer.active.first { it.delayMillis == 2_000L }
        delayTask.fireEvenIfCancelled()
        fixture.audio.reportPlaying(true)

        // User hits Mute
        fixture.controller.toggleAudio()
        assertTrue(fixture.audio.stops >= 1)
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    @Test
    fun `auto-hide test G old generation completion callback never hides newer popup`() {
        val fixture = Fixture()
        val candidate1 = candidate("test1", audioReference = "en1.mp3", translatedAudioReference = "vi1.mp3")
        fixture.controller.dispatch(
            candidate = candidate1,
            displayDurationMillis = 3_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        // Popup 1 closed manually
        fixture.controller.closePopup()

        // Popup 2 dispatched
        val candidate2 = candidate("test2", audioReference = "en2.mp3", translatedAudioReference = "vi2.mp3")
        fixture.controller.dispatch(
            candidate = candidate2,
            displayDurationMillis = 8_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = false
        )
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)

        // Old generation completion fires
        fixture.audio.completeLatest()
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.controller.state.value)
    }

    @Test
    fun `auto-hide test H playback start or resolve failure does not keep popup alive forever`() {
        val fixture = Fixture()
        fixture.audio.failStart = true
        val candidate = candidate("test", audioReference = "broken.mp3", translatedAudioReference = "broken_vi.mp3")
        fixture.controller.dispatch(
            candidate = candidate,
            displayDurationMillis = 4_000L,
            autoPlayPronunciation = true,
            popupLocation = DesktopVocabularyReminderPopupLocation(),
            popupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_000L
        )
        // Display hide timer fires
        val hideTask = fixture.timer.active.first { it.delayMillis == 4_000L }
        hideTask.fireEvenIfCancelled()

        // With failed audio, it must not hang forever
        val delayTask = fixture.timer.active.firstOrNull { it.delayMillis == 2_000L }
        delayTask?.fireEvenIfCancelled()
        assertEquals(DesktopVocabularyReminderPopupState.Hidden, fixture.controller.state.value)
    }

    private class FakeAudio : DesktopVocabularyReminderAudioLifecycle {
        val starts = mutableListOf<String>()
        val loops = mutableListOf<String>()
        val completions = mutableListOf<() -> Unit>()
        var stops = 0
        var closed = false
        var failStart = false
        private var listener: ((Boolean) -> Unit)? = null
        override fun listen(listener: (Boolean) -> Unit): AutoCloseable {
            this.listener = listener
            return AutoCloseable { this.listener = null }
        }
        fun reportPlaying(playing: Boolean) = listener?.invoke(playing) ?: Unit
        override fun start(audioReference: String?) {
            start(audioReference, onCompleted = null)
        }
        override fun start(audioReference: String?, onCompleted: (() -> Unit)?) {
            if (failStart) error("playback unavailable")
            audioReference?.let(starts::add)
            onCompleted?.let(completions::add)
        }
        fun completeLatest() {
            if (completions.isNotEmpty()) {
                val cb = completions.removeLast()
                cb.invoke()
            }
        }
        override fun startLoop(audioReference: String?) { audioReference?.let(loops::add) }
        override fun stop() { stops++ }
        override fun close() { closed = true }
    }

    private class FakeClock(var now: Long = 0L) : DesktopVocabularyReminderMonotonicClock {
        override fun nowMillis() = now
    }

    private class FakeTimer : DesktopVocabularyReminderPopupTimer, AutoCloseable {
        val tasks = mutableListOf<Task>()
        val active get() = tasks.filterNot(Task::cancelled)
        var closed = false

        override fun schedule(delayMillis: Long, action: () -> Unit): DesktopVocabularyReminderScheduledTask =
            Task(delayMillis, action).also(tasks::add)

        fun fireActive() = active.single().fireEvenIfCancelled()
        fun fireActiveEvenIfCancelled() = tasks.last().fireEvenIfCancelled()

        override fun close() {
            closed = true
            active.forEach { it.cancel() }
        }

        class Task(val delayMillis: Long, private val action: () -> Unit) : DesktopVocabularyReminderScheduledTask {
            var cancelled = false
            override fun cancel() { cancelled = true }
            fun fireEvenIfCancelled() = action()
        }
    }

    private fun candidate(
        text: String,
        audioReference: String? = null,
        translatedAudioReference: String? = null,
        imageReference: String? = null
    ) = DesktopVocabularyCandidate(
        ContentId("content-$text"), InstalledPackageId("package"), "Package", text,
        "answer", "translation", "/ipa/", "noun", imageReference, audioReference, translatedAudioReference, "Lesson", "Section"
    )
}
