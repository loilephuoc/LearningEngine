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
                assertEquals(minutes * 60_000L, valid.settings.intervalMillis)
                assertEquals(millis, valid.settings.displayDurationMillis)
                assertEquals(java.time.LocalTime.of(22, 0), valid.settings.activeStart)
                assertEquals(java.time.LocalTime.of(6, 30), valid.settings.activeEnd)
            }
        }
    }

    @Test
    fun `draft accepts custom seconds intervals at or above minimum five seconds`() {
        listOf("5" to 5_000L, "10" to 10_000L, "20" to 20_000L, "30" to 30_000L, "45" to 45_000L).forEach { (secText, millis) ->
            val draft = DesktopVocabularyReminderDraft(
                enabled = true,
                selectedPackageId = null,
                selectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_ALL,
                intervalValueText = secText,
                intervalUnit = DesktopVocabularyReminderIntervalUnit.SECONDS,
                activeStartText = "08:00",
                activeEndText = "22:00",
                displayDurationText = "8",
                autoPlayPronunciation = false
            )
            val valid = assertIs<DesktopVocabularyReminderDraftValidation.Valid>(draft.validate(null))
            assertEquals(millis, valid.settings.intervalMillis)
        }
    }

    @Test
    fun `draft rejects malformed nonpositive interval below minimum and duration outside bounds`() {
        listOf("0", "-1", "NaN", "Infinity", "4").forEach {
            val secondsDraft = DesktopVocabularyReminderDraft(
                enabled = true,
                selectedPackageId = null,
                selectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_ALL,
                intervalValueText = it,
                intervalUnit = DesktopVocabularyReminderIntervalUnit.SECONDS,
                activeStartText = "08:00",
                activeEndText = "22:00",
                displayDurationText = "8",
                autoPlayPronunciation = false
            )
            assertIs<DesktopVocabularyReminderDraftValidation.Invalid>(secondsDraft.validate(null))
        }
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
    fun `twenty second interval schedules twenty thousand milliseconds delay`() {
        val fixture = Fixture()
        fixture.runtime.start()
        val secDraft = DesktopVocabularyReminderDraft(
            enabled = true,
            selectedPackageId = null,
            selectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_ALL,
            intervalValueText = "20",
            intervalUnit = DesktopVocabularyReminderIntervalUnit.SECONDS,
            activeStartText = "08:00",
            activeEndText = "22:00",
            displayDurationText = "8",
            autoPlayPronunciation = false
        )
        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.apply(secDraft))
        assertEquals(20_000L, fixture.scheduler.lastDelay)
        assertEquals(20_000L, fixture.runtime.settings.intervalMillis)
    }

    @Test
    fun `updatePopupLocation and resetPopupPosition update and persist correctly`() {
        val fixture = Fixture()
        val location = DesktopVocabularyReminderPopupLocation(
            monitorId = "display-2",
            normalizedX = 0.5,
            normalizedY = 0.5,
            customPosition = true
        )
        assertTrue(fixture.controller.updatePopupLocation(location))
        assertEquals(location, fixture.runtime.settings.popupLocation)
        assertEquals(1, fixture.store.saves)

        assertTrue(fixture.controller.resetPopupPosition())
        assertFalse(fixture.runtime.settings.popupLocation.customPosition)
        assertEquals("display-2", fixture.runtime.settings.popupLocation.monitorId)
        assertEquals(2, fixture.store.saves)
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
    fun `draft from settings maps layout and vietnamese audio delay correctly`() {
        val settings = DesktopVocabularyReminderSettings(
            enabled = true,
            popupLayout = DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL,
            playVietnameseAudio = true,
            vietnameseAudioDelayMillis = 2_500L
        )
        val draft = DesktopVocabularyReminderDraft.from(settings)
        assertEquals(DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL, draft.popupLayout)
        assertTrue(draft.playVietnameseAudio)
        assertEquals("2.5", draft.vietnameseAudioDelaySecondsText)
    }

    @Test
    fun `draft accepts valid decimal vietnamese delay and rejects out of bounds or invalid text`() {
        listOf("0" to 0L, "0.0" to 0L, "0.5" to 500L, "2.0" to 2_000L, "2.5" to 2_500L, "30" to 30_000L, "30.0" to 30_000L).forEach { (text, millis) ->
            val draft = DesktopVocabularyReminderDraft(
                enabled = true,
                selectedPackageId = null,
                selectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_ALL,
                intervalValueText = "15",
                intervalUnit = DesktopVocabularyReminderIntervalUnit.MINUTES,
                activeStartText = "08:00",
                activeEndText = "22:00",
                displayDurationText = "8",
                autoPlayPronunciation = false,
                popupLayout = DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL,
                playVietnameseAudio = true,
                vietnameseAudioDelaySecondsText = text
            )
            val valid = assertIs<DesktopVocabularyReminderDraftValidation.Valid>(draft.validate(null))
            assertEquals(millis, valid.settings.vietnameseAudioDelayMillis)
            assertEquals(DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL, valid.settings.popupLayout)
            assertTrue(valid.settings.playVietnameseAudio)
        }

        listOf("-0.1", "-1", "30.001", "35", "NaN", "Infinity", "invalid").forEach { invalidText ->
            val draft = DesktopVocabularyReminderDraft(
                enabled = true,
                selectedPackageId = null,
                selectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_ALL,
                intervalValueText = "15",
                intervalUnit = DesktopVocabularyReminderIntervalUnit.MINUTES,
                activeStartText = "08:00",
                activeEndText = "22:00",
                displayDurationText = "8",
                autoPlayPronunciation = false,
                popupLayout = DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL,
                playVietnameseAudio = true,
                vietnameseAudioDelaySecondsText = invalidText
            )
            assertIs<DesktopVocabularyReminderDraftValidation.Invalid>(draft.validate(null))
        }
    }

    @Test
    fun `preview forwards draft layout and vietnamese audio to popup controller`() {
        val fixture = Fixture()
        val draft = DesktopVocabularyReminderDraft(
            enabled = true,
            selectedPackageId = null,
            selectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_ALL,
            intervalValueText = "15",
            intervalUnit = DesktopVocabularyReminderIntervalUnit.MINUTES,
            activeStartText = "08:00",
            activeEndText = "22:00",
            displayDurationText = "8",
            autoPlayPronunciation = false,
            popupLayout = DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL,
            playVietnameseAudio = true,
            vietnameseAudioDelaySecondsText = "3.5"
        )
        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.preview(draft))
        val visible = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.popup.state.value)
        assertEquals(DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL, visible.popupLayout)
        assertEquals(0, fixture.store.saves)
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
    fun `draft validates english font size bounds and apply persists english font size and foreground visibility`() {
        val fixture = Fixture()
        val validDraft = draft("7", "8").copy(
            englishTextFontSizeSp = 28f,
            showPopupWhileAppForeground = false
        )
        val valid = assertIs<DesktopVocabularyReminderDraftValidation.Valid>(validDraft.validate(null))
        assertEquals(28f, valid.settings.englishTextFontSizeSp)
        assertFalse(valid.settings.showPopupWhileAppForeground)

        val belowMin = validDraft.copy(englishTextFontSizeSp = 13f)
        assertIs<DesktopVocabularyReminderDraftValidation.Invalid>(belowMin.validate(null))

        val atMax = validDraft.copy(englishTextFontSizeSp = 48f)
        assertIs<DesktopVocabularyReminderDraftValidation.Valid>(atMax.validate(null))

        val aboveMax = validDraft.copy(englishTextFontSizeSp = 49f)
        assertIs<DesktopVocabularyReminderDraftValidation.Invalid>(aboveMax.validate(null))

        // Preview receives draft font size immediately without persisting
        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.preview(validDraft))
        assertEquals(0, fixture.store.saves)
        val visible = assertIs<DesktopVocabularyReminderPopupState.Visible>(fixture.popup.state.value)
        assertEquals(28f, visible.englishTextFontSizeSp)
        fixture.popup.closePopup()

        // Apply persists
        assertEquals(DesktopVocabularyReminderActionResult.Success, fixture.controller.apply(validDraft))
        assertEquals(1, fixture.store.saves)
        assertEquals(28f, fixture.runtime.settings.englishTextFontSizeSp)
        assertFalse(fixture.runtime.settings.showPopupWhileAppForeground)
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
        var runtime: DesktopVocabularyReminderRuntime
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
        var controller: DesktopVocabularyReminderSettingsController
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
            null, null, null, null, null, "audio/ref", null, null, null
        )
    }
}
