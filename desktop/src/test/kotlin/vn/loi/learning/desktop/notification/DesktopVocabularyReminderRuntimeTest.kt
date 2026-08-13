package vn.loi.learning.desktop.notification

import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class DesktopVocabularyReminderRuntimeTest {
    @Test
    fun `disabled has no schedule while enabled dispatches once after interval on fixed delay cadence`() {
        val fixture = RuntimeFixture(DesktopVocabularyReminderSettings())
        fixture.runtime.start()
        assertEquals(0, fixture.scheduler.activeCount)
        fixture.runtime.updateSettings(fixture.enabledSettings(intervalMinutes = 5))
        assertEquals(listOf(300_000L), fixture.scheduler.activeDelays)

        fixture.scheduler.fireNext()
        assertEquals(1, fixture.sink.candidates.size)
        assertEquals(listOf(300_000L), fixture.scheduler.activeDelays)
        fixture.scheduler.fireNext()
        assertEquals(2, fixture.sink.candidates.size)
    }

    @Test
    fun `outside active hours paused active sink and empty candidate each skip without immediate retry`() {
        val fixture = RuntimeFixture(
            DesktopVocabularyReminderSettings(
                enabled = true,
                selectedPackageId = InstalledPackageId("package"),
                activeStart = LocalTime.of(12, 0),
                activeEnd = LocalTime.of(13, 0)
            )
        )
        fixture.runtime.start()
        fixture.scheduler.fireNext()
        assertEquals(0, fixture.selector.calls.size)
        assertEquals(1, fixture.scheduler.activeCount)

        fixture.runtime.updateSettings(fixture.enabledSettings(pausedUntil = fixture.clock.instant().plusSeconds(60)))
        fixture.scheduler.fireNext()
        assertEquals(0, fixture.selector.calls.size)

        fixture.clock.advanceSeconds(61)
        fixture.sink.active = true
        fixture.scheduler.fireNext()
        assertEquals(0, fixture.selector.calls.size)

        fixture.sink.active = false
        fixture.selector.result = DesktopVocabularyCandidateSelectionResult.NoCandidate(
            DesktopVocabularyCandidateSelectionResult.Reason.NO_ELIGIBLE_CANDIDATE
        )
        fixture.scheduler.fireNext()
        assertEquals(1, fixture.selector.calls.size)
        assertEquals(0, fixture.sink.candidates.size)
        assertEquals(1, fixture.scheduler.activeCount)
    }

    @Test
    fun `pause expiry automatically resumes and pause commands persist without disabling`() {
        val fixture = RuntimeFixture(fixtureSettings())
        fixture.runtime.start()
        fixture.runtime.pauseFor30Minutes()
        assertTrue(fixture.runtime.settings.enabled)
        assertEquals(fixture.clock.instant().plusSeconds(1_800), fixture.store.saved.last().pausedUntil)
        fixture.scheduler.fireNext()
        assertEquals(0, fixture.sink.candidates.size)
        fixture.clock.advanceSeconds(1_800)
        fixture.scheduler.fireNext()
        assertEquals(1, fixture.sink.candidates.size)

        fixture.runtime.pauseForOneHour()
        assertEquals(fixture.clock.instant().plusSeconds(3_600), fixture.store.saved.last().pausedUntil)
        fixture.runtime.pauseToday()
        assertEquals(
            Instant.parse("2026-08-14T00:00:00Z"),
            fixture.store.saved.last().pausedUntil
        )
    }

    @Test
    fun `settings changes cancel prior delay and future selection receives new package mode and interval`() {
        val fixture = RuntimeFixture(fixtureSettings())
        fixture.runtime.start()
        val changed = fixture.enabledSettings(
            packageId = "package-two",
            mode = DesktopVocabularyReminderSelectionMode.RANDOM_LEARNED,
            intervalMinutes = 60
        )
        fixture.runtime.updateSettings(changed)
        assertEquals(listOf(3_600_000L), fixture.scheduler.activeDelays)
        fixture.scheduler.fireNext()
        assertEquals(changed, fixture.selector.calls.single())
    }

    @Test
    fun `disable cancels pending schedule and re-enable starts a fresh cycle`() {
        val fixture = RuntimeFixture(fixtureSettings())
        fixture.runtime.start()
        fixture.runtime.updateSettings(fixture.runtime.settings.copy(enabled = false))
        assertEquals(0, fixture.scheduler.activeCount)
        fixture.runtime.updateSettings(fixture.enabledSettings())
        assertEquals(1, fixture.scheduler.activeCount)
        fixture.scheduler.fireNext()
        assertEquals(1, fixture.sink.candidates.size)
    }

    @Test
    fun `missing package selection and selector unavailable result do not dispatch`() {
        val fixture = RuntimeFixture(fixtureSettings().copy(selectedPackageId = null))
        fixture.runtime.start()
        fixture.scheduler.fireNext()
        assertEquals(0, fixture.selector.calls.size)
        fixture.runtime.updateSettings(fixture.enabledSettings())
        fixture.selector.result = DesktopVocabularyCandidateSelectionResult.NoCandidate(
            DesktopVocabularyCandidateSelectionResult.Reason.PACKAGE_UNAVAILABLE
        )
        fixture.scheduler.fireNext()
        assertEquals(0, fixture.sink.candidates.size)
    }

    @Test
    fun `large clock jump and zone changes are evaluated once at next tick without catch-up`() {
        val fixture = RuntimeFixture(
            fixtureSettings().copy(activeStart = LocalTime.of(8, 0), activeEnd = LocalTime.of(22, 0))
        )
        fixture.runtime.start()
        fixture.clock.advanceSeconds(48 * 3_600L)
        fixture.zone = ZoneId.of("Pacific/Honolulu")
        fixture.scheduler.fireNext()
        assertTrue(fixture.selector.calls.size <= 1)
        assertEquals(1, fixture.scheduler.activeCount)
    }

    @Test
    fun `shutdown is idempotent and cancels every future tick`() {
        val fixture = RuntimeFixture(fixtureSettings())
        fixture.runtime.start()
        fixture.runtime.close()
        fixture.runtime.close()
        assertEquals(0, fixture.scheduler.activeCount)
        assertTrue(fixture.scheduler.closed)
        assertFalse(fixture.scheduler.fireNextIfPresent())
        assertEquals(0, fixture.sink.candidates.size)
    }

    @Test
    fun `load and tick failures degrade safely and retain normal cadence`() {
        val failures = mutableListOf<Throwable>()
        val fixture = RuntimeFixture(fixtureSettings(), loadFailure = IllegalStateException("bad config"), failures = failures)
        assertEquals(DesktopVocabularyReminderSettings(), fixture.runtime.settings)
        fixture.runtime.updateSettings(fixture.enabledSettings())
        fixture.selector.failure = IllegalStateException("selection failed")
        fixture.runtime.start()
        fixture.scheduler.fireNext()
        assertEquals(2, failures.size)
        assertEquals(1, fixture.scheduler.activeCount)
    }

    private class RuntimeFixture(
        initial: DesktopVocabularyReminderSettings,
        loadFailure: Throwable? = null,
        failures: MutableList<Throwable> = mutableListOf()
    ) {
        val clock = MutableClock(Instant.parse("2026-08-13T10:00:00Z"))
        var zone: ZoneId = ZoneOffset.UTC
        val store = FakeSettingsRepository(initial, loadFailure)
        val scheduler = FakeDelayScheduler()
        val selector = FakeSelector(selectedCandidate())
        val sink = FakeSink()
        val runtime = DesktopVocabularyReminderRuntime(
            store,
            selector,
            sink,
            scheduler,
            clock,
            { zone },
            failures::add
        )

        fun enabledSettings(
            packageId: String = "package",
            mode: DesktopVocabularyReminderSelectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_ALL,
            intervalMinutes: Int = 15,
            pausedUntil: Instant? = null
        ) = DesktopVocabularyReminderSettings(
            enabled = true,
            selectedPackageId = InstalledPackageId(packageId),
            selectionMode = mode,
            intervalMinutes = intervalMinutes,
            activeStart = LocalTime.MIDNIGHT,
            activeEnd = LocalTime.MIDNIGHT,
            pausedUntil = pausedUntil
        )
    }

    private class FakeSettingsRepository(
        private val initial: DesktopVocabularyReminderSettings,
        private val loadFailure: Throwable?
    ) : DesktopVocabularyReminderSettingsRepository {
        val saved = mutableListOf<DesktopVocabularyReminderSettings>()
        override fun load(): DesktopVocabularyReminderSettings = loadFailure?.let { throw it } ?: initial
        override fun save(settings: DesktopVocabularyReminderSettings) {
            saved += settings
        }
    }

    private class FakeSelector(candidate: DesktopVocabularyCandidate) : DesktopVocabularyReminderSelectionSource {
        val calls = mutableListOf<DesktopVocabularyReminderSettings>()
        var result: DesktopVocabularyCandidateSelectionResult =
            DesktopVocabularyCandidateSelectionResult.Selected(candidate)
        var failure: Throwable? = null
        override fun select(settings: DesktopVocabularyReminderSettings): DesktopVocabularyCandidateSelectionResult {
            calls += settings
            failure?.let { throw it }
            return result
        }
    }

    private class FakeSink : DesktopVocabularyReminderSink {
        var active = false
        val candidates = mutableListOf<DesktopVocabularyCandidate>()
        override val isReminderActive: Boolean get() = active
        override fun dispatch(candidate: DesktopVocabularyCandidate) {
            candidates += candidate
        }
    }

    private class FakeDelayScheduler : DesktopVocabularyReminderDelayScheduler, AutoCloseable {
        private data class Entry(val delay: Long, val action: () -> Unit, var cancelled: Boolean = false)
        private val entries = mutableListOf<Entry>()
        var closed = false
        val activeCount get() = entries.count { !it.cancelled }
        val activeDelays get() = entries.filterNot { it.cancelled }.map(Entry::delay)
        override fun schedule(delayMillis: Long, action: () -> Unit): DesktopVocabularyReminderScheduledTask {
            val entry = Entry(delayMillis, action)
            entries += entry
            return DesktopVocabularyReminderScheduledTask { entry.cancelled = true }
        }
        fun fireNext() {
            check(fireNextIfPresent())
        }
        fun fireNextIfPresent(): Boolean {
            val entry = entries.firstOrNull { !it.cancelled } ?: return false
            entry.cancelled = true
            entry.action()
            return true
        }
        override fun close() {
            closed = true
            entries.forEach { it.cancelled = true }
        }
    }

    private class MutableClock(private var current: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = current
        fun advanceSeconds(seconds: Long) {
            current = current.plusSeconds(seconds)
        }
    }

    companion object {
        private fun fixtureSettings() = DesktopVocabularyReminderSettings(
            enabled = true,
            selectedPackageId = InstalledPackageId("package"),
            selectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_ALL,
            activeStart = LocalTime.MIDNIGHT,
            activeEnd = LocalTime.MIDNIGHT
        )

        private fun selectedCandidate() = DesktopVocabularyCandidate(
            ContentId("content"),
            InstalledPackageId("package"),
            "Package",
            "Word",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
    }
}
