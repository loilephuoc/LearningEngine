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

    private class Fixture(authority: FakeAutoPlayAuthority? = null) {
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
            autoPlayAuthority = authority
        )
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

    private class FakeAudio : DesktopVocabularyReminderAudioLifecycle {
        val starts = mutableListOf<String>()
        val loops = mutableListOf<String>()
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
            if (failStart) error("playback unavailable")
            audioReference?.let(starts::add)
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

    private fun candidate(text: String, audioReference: String? = null) = DesktopVocabularyCandidate(
        ContentId("content-$text"), InstalledPackageId("package"), "Package", text,
        "answer", "translation", "/ipa/", "noun", null, audioReference, "Lesson", "Section"
    )
}
