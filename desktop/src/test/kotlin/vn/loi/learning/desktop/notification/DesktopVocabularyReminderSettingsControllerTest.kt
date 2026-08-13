package vn.loi.learning.desktop.notification

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DesktopVocabularyReminderSettingsControllerTest {
    @Test
    fun `draft accepts arbitrary whole minutes fractional durations and active hours`() {
        listOf(1, 2, 7, 12, 20, 45, 90, 120).forEach { minutes ->
            listOf("1.5" to 1_500L, "2.5" to 2_500L, "4.2" to 4_200L, "12.75" to 12_750L, "60" to 60_000L).forEach { (text, millis) ->
                val valid = assertIs<DesktopVocabularyReminderDraftValidation.Valid>(draft(minutes.toString(), text).validate(null))
                assertEquals(minutes, valid.settings.intervalMinutes)
                assertEquals(millis, valid.settings.displayDurationMillis)
                assertEquals(java.time.LocalTime.of(22, 0), valid.settings.activeStart)
                assertEquals(java.time.LocalTime.of(6, 30), valid.settings.activeEnd)
            }
        }
    }

    @Test
    fun `draft rejects malformed nonpositive interval and duration outside bounds`() {
        listOf("0", "-1", "NaN", "Infinity", "1.5").forEach {
            assertIs<DesktopVocabularyReminderDraftValidation.Invalid>(draft(it, "8").validate(null))
        }
        listOf("bad", "1.49", "60.001", "NaN", "Infinity").forEach {
            assertIs<DesktopVocabularyReminderDraftValidation.Invalid>(draft("7", it).validate(null))
        }
    }

    @Test
    fun `apply persists and immediately reschedules while preview uses draft without saving`() {
        val fixture = Fixture()
        fixture.runtime.start()
        val draft = draft("7", "1.5")

        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.preview(draft))
        assertEquals(0, fixture.store.saves)
        assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.popup.state.value)
        fixture.popup.closePopup()

        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.apply(draft))
        assertEquals(1, fixture.store.saves)
        assertEquals(420_000L, fixture.scheduler.lastDelay)
        assertEquals(1_500L, fixture.runtime.settings.displayDurationMillis)
    }

    @Test
    fun `persistence failure preserves runtime and reports failure`() {
        val fixture = Fixture()
        val before = fixture.runtime.settings
        fixture.store.fail = true
        assertIs<DesktopVocabularyReminderActionResult.Failure>(fixture.controller.apply(draft("12", "2.5")))
        assertEquals(before, fixture.runtime.settings)
    }

    @Test
    fun `unavailable selected package and active popup produce deterministic preview feedback`() {
        val fixture = Fixture()
        val unavailable = draft("7", "8").copy(selectedPackageId = InstalledPackageId("missing"))
        assertEquals(
            DesktopVocabularyReminderActionResult.Failure("Selected package is unavailable."),
            fixture.controller.apply(unavailable)
        )
        fixture.popup.dispatch(candidate(), 8_000, false)
        assertEquals(
            DesktopVocabularyReminderActionResult.Failure("A reminder popup is already visible."),
            fixture.controller.preview(draft("7", "8"))
        )
    }

    @Test
    fun `pause and resume persist without disabling`() {
        val fixture = Fixture()
        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.pause30Minutes())
        assertTrue(fixture.runtime.settings.enabled)
        assertEquals(Instant.parse("2026-08-13T10:30:00Z"), fixture.runtime.settings.pausedUntil)
        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.resumeNow())
        assertEquals(null, fixture.runtime.settings.pausedUntil)
    }

    @Test
    fun `apply and preview respect draft autoplay without implicit preview persistence`() {
        val fixture = Fixture()
        val enabledAudio = draft("7", "8").copy(autoPlayPronunciation = true)
        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.preview(enabledAudio))
        assertEquals(listOf("audio/ref"), fixture.audio.starts)
        assertEquals(0, fixture.store.saves)
        fixture.popup.closePopup()
        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.preview(enabledAudio.copy(autoPlayPronunciation = false)))
        assertEquals(listOf("audio/ref"), fixture.audio.starts)
        fixture.popup.closePopup()
        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.apply(enabledAudio))
        assertTrue(fixture.runtime.settings.autoPlayPronunciation)
    }

    @Test
    fun `popup quick toggle and configure apply share one autoplay authority`() {
        val fixture = Fixture()
        fixture.popup.dispatch(candidate(), 8_000, false)
        fixture.popup.toggleAudio()
        assertTrue(fixture.runtime.settings.autoPlayPronunciation)
        assertTrue(fixture.controller.load().settings.autoPlayPronunciation)

        assertEquals(
            DesktopVocabularyReminderActionResult.Success,
            fixture.controller.apply(
                draft("7", "8").copy(
                    selectionMode = fixture.runtime.settings.selectionMode,
                    autoPlayPronunciation = false
                )
            )
        )
        assertFalse(assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.popup.state.value).autoPlayPronunciation)
        assertFalse(fixture.controller.load().settings.autoPlayPronunciation)
    }

    private class Fixture {
        val store = Store()
        val scheduler = Scheduler()
        val audio = FakeAudio()
        lateinit var runtime: DesktopVocabularyReminderRuntime
        val popup = DesktopVocabularyReminderPopupController(
            DesktopVocabularyReminderUiDispatcher { it() },
            DesktopVocabularyReminderMonotonicClock { 0L },
            DesktopVocabularyReminderPopupTimer { _, _ -> DesktopVocabularyReminderScheduledTask {} },
            audio,
            autoPlayAuthority = object : DesktopVocabularyReminderAutoPlayAuthority {
                override fun current() = runtime.settings.autoPlayPronunciation
                override fun update(enabled: Boolean) =
                    runtime.updateSettings(runtime.settings.copy(autoPlayPronunciation = enabled))
            }
        )
        lateinit var controller: DesktopVocabularyReminderSettingsController
        init {
            runtime = DesktopVocabularyReminderRuntime(
                store,
                DesktopVocabularyReminderSelectionSource { DesktopVocabularyCandidateSelectionResult.Selected(candidate()) },
                popup,
                scheduler,
                Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneOffset.UTC),
                { ZoneOffset.UTC }
            )
            controller = DesktopVocabularyReminderSettingsController(
                runtime,
                DesktopVocabularyReminderSelectionSource { DesktopVocabularyCandidateSelectionResult.Selected(candidate()) },
                popup,
                LearningApplicationFactory.createInMemory().installedPackages
            )
        }
    }

    private class FakeAudio : DesktopVocabularyReminderAudioLifecycle {
        val starts = mutableListOf<String>()
        override fun start(audioReference: String?) { audioReference?.let(starts::add) }
        override fun stop() = Unit
        override fun close() = Unit
    }

    private class Store : DesktopVocabularyReminderSettingsRepository {
        var current = DesktopVocabularyReminderSettings(enabled = true)
        var saves = 0
        var fail = false
        override fun load() = current
        override fun save(settings: DesktopVocabularyReminderSettings) {
            if (fail) error("persistence failure")
            saves++
            current = settings
        }
    }

    private class Scheduler : DesktopVocabularyReminderDelayScheduler {
        var lastDelay: Long? = null
        override fun schedule(delayMillis: Long, action: () -> Unit): DesktopVocabularyReminderScheduledTask {
            lastDelay = delayMillis
            return DesktopVocabularyReminderScheduledTask {}
        }
    }

    companion object {
        private fun draft(interval: String, duration: String) = DesktopVocabularyReminderDraft(
            true, null, DesktopVocabularyReminderSelectionMode.RANDOM_ALL,
            interval, "22:00", "06:30", duration, false
        )
        private fun candidate() = DesktopVocabularyCandidate(
            ContentId("content"), InstalledPackageId("package"), "Package", "Word",
            null, null, null, null, null, "audio/ref", null, null
        )
    }
}
