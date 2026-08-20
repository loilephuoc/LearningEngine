package vn.loi.learning.desktop.notification

import java.nio.file.Files
import java.time.Instant
import java.time.LocalTime
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.library.model.InstalledPackageId

class DesktopVocabularyReminderSettingsStoreTest {
    @Test
    fun `missing file loads safe defaults`() = withStore { store, _ ->
        assertEquals(DesktopVocabularyReminderSettings(), store.load())
    }

    @Test
    fun `save load and restart preserve deterministic settings including overnight and pause`() =
        withStore { store, file ->
            val settings = DesktopVocabularyReminderSettings(
                enabled = true,
                selectedPackageId = InstalledPackageId("unavailable-package-is-preserved"),
                selectionMode = DesktopVocabularyReminderSelectionMode.RANDOM_LEARNED,
                intervalMillis = 1_800_000L,
                activeStart = LocalTime.of(22, 0),
                activeEnd = LocalTime.of(6, 30),
                displayDurationMillis = 12_750,
                pausedUntil = Instant.parse("2026-08-14T00:00:00Z"),
                popupLocation = DesktopVocabularyReminderPopupLocation(
                    monitorId = "display-1",
                    normalizedX = 0.75,
                    normalizedY = 0.85,
                    customPosition = true
                ),
                popupLayout = DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL,
                playVietnameseAudio = true,
                vietnameseAudioDelayMillis = 2_500L,
                englishTextFontSizeSp = 26f,
                showPopupWhileAppForeground = false
            )
            store.save(settings)
            assertEquals(settings, store.load())
            assertEquals(settings, DesktopVocabularyReminderSettingsStore(file).load())
            assertEquals(
                """enabled=true
installed.package.id=unavailable-package-is-preserved
selection.mode=RANDOM_LEARNED
interval.millis=1800000
active.start=22:00
active.end=06:30
display.duration.millis=12750
audio.autoplay.pronunciation=false
paused.until.epoch.millis=1786665600000
popup.monitor.id=display-1
popup.position.custom=true
popup.position.x.normalized=0.75
popup.position.y.normalized=0.85
popup.layout=LARGE_IMAGE_VERTICAL
audio.vietnamese.enabled=true
audio.vietnamese.delay.millis=2500
text.english.font.size.sp=26.0
popup.show.while.app.foreground=false
""",
                file.readText()
            )
            assertTrue(Files.list(file.parent).use { files ->
                files.noneMatch { it.fileName.toString().endsWith(".tmp") }
            })
        }

    @Test
    fun `legacy properties file without layout vietnamese audio and font settings safely loads defaults`() = withStore { store, file ->
        file.writeText(
            """enabled=true
installed.package.id=test-pkg
"""
        )
        val loaded = store.load()
        assertEquals(DesktopVocabularyReminderPopupLayout.COMPACT, loaded.popupLayout)
        assertFalse(loaded.playVietnameseAudio)
        assertEquals(2_000L, loaded.vietnameseAudioDelayMillis)
        assertEquals(22f, loaded.englishTextFontSizeSp)
        assertTrue(loaded.showPopupWhileAppForeground)
    }

    @Test
    fun `malformed popup layout delay font size and foreground visibility fall back to safe defaults`() = withStore { store, file ->
        file.writeText(
            """popup.layout=INVALID_LAYOUT_NAME
audio.vietnamese.enabled=not-a-boolean
audio.vietnamese.delay.millis=999999999
text.english.font.size.sp=999.0
popup.show.while.app.foreground=invalid-boolean
"""
        )
        val loaded = store.load()
        assertEquals(DesktopVocabularyReminderPopupLayout.COMPACT, loaded.popupLayout)
        assertFalse(loaded.playVietnameseAudio)
        assertEquals(2_000L, loaded.vietnameseAudioDelayMillis)
        assertEquals(22f, loaded.englishTextFontSizeSp)
        assertTrue(loaded.showPopupWhileAppForeground)
    }

    @Test
    fun `english font size accepts values between 14sp and 48sp inclusive`() = withStore { store, file ->
        listOf(14f, 18f, 22f, 36f, 40f, 44f, 48f).forEach { size ->
            file.writeText("text.english.font.size.sp=$size\n")
            assertEquals(size, store.load().englishTextFontSizeSp)
        }
        listOf(13.9f, 48.1f, -1f, 100f).forEach { invalid ->
            file.writeText("text.english.font.size.sp=$invalid\n")
            assertEquals(22f, store.load().englishTextFontSizeSp)
        }
    }

    @Test
    fun `equal active times survive round trip as 24 hour window`() = withStore { store, _ ->
        val settings = DesktopVocabularyReminderSettings(activeStart = LocalTime.NOON, activeEnd = LocalTime.NOON)
        store.save(settings)
        assertEquals(settings, store.load())
    }

    @Test
    fun `phase two keys migrate without losing unrelated fields`() = withStore { store, file ->
        file.writeText(
            """enabled=true
installed.package.id=legacy-package
selection.mode=DUE
interval.minutes=15
active.start=08:00
active.end=22:00
display.duration.seconds=8
paused.until.epoch.millis=1786665600000
"""
        )
        val loaded = store.load()
        assertTrue(loaded.enabled)
        assertEquals(InstalledPackageId("legacy-package"), loaded.selectedPackageId)
        assertEquals(DesktopVocabularyReminderSelectionMode.DUE, loaded.selectionMode)
        assertEquals(15, loaded.intervalMinutes)
        assertEquals(900_000L, loaded.intervalMillis)
        assertEquals(8_000L, loaded.displayDurationMillis)
        assertEquals(Instant.parse("2026-08-14T00:00:00Z"), loaded.pausedUntil)
        store.save(loaded)
        assertTrue(file.readText().contains("display.duration.millis=8000"))
        assertFalse(file.readText().contains("display.duration.seconds"))
        assertTrue(file.readText().contains("interval.millis=900000"))
    }

    @Test
    fun `canonical interval millis takes precedence over legacy interval minutes`() = withStore { store, file ->
        file.writeText(
            """enabled=true
interval.millis=20000
interval.minutes=15
"""
        )
        val loaded = store.load()
        assertEquals(20_000L, loaded.intervalMillis)
        store.save(loaded)
        assertTrue(file.readText().contains("interval.millis=20000"))
    }

    @Test
    fun `popup location restores custom position and safely falls back on invalid coordinates`() = withStore { store, file ->
        file.writeText(
            """popup.monitor.id=test-mon
popup.position.custom=true
popup.position.x.normalized=0.25
popup.position.y.normalized=invalid-num
"""
        )
        val loaded = store.load()
        assertEquals("test-mon", loaded.popupLocation.monitorId)
        assertTrue(loaded.popupLocation.customPosition)
        assertEquals(0.25, loaded.popupLocation.normalizedX)
        assertNull(loaded.popupLocation.normalizedY)
    }

    @Test
    fun `autoplay defaults off and persists on off with malformed fallback`() = withStore { store, file ->
        file.writeText("audio.autoplay.pronunciation=invalid\n")
        assertFalse(store.load().autoPlayPronunciation)
        store.save(DesktopVocabularyReminderSettings(autoPlayPronunciation = true))
        assertTrue(store.load().autoPlayPronunciation)
        assertTrue(DesktopVocabularyReminderSettingsStore(file).load().autoPlayPronunciation)
        store.save(DesktopVocabularyReminderSettings(autoPlayPronunciation = false))
        assertFalse(store.load().autoPlayPronunciation)
        assertFalse(DesktopVocabularyReminderSettingsStore(file).load().autoPlayPronunciation)
    }

    @Test
    fun `each malformed field falls back independently while valid fields survive`() = withStore { store, file ->
        file.writeText(
            """enabled=not-a-boolean
installed.package.id=kept-package
selection.mode=unknown
interval.minutes=17
active.start=25:00
active.end=23:15
display.duration.seconds=1
paused.until.epoch.millis=not-a-long
"""
        )
        val loaded = store.load()
        assertFalse(loaded.enabled)
        assertEquals(InstalledPackageId("kept-package"), loaded.selectedPackageId)
        assertEquals(DesktopVocabularyReminderSelectionMode.AGAIN_HARD, loaded.selectionMode)
        assertEquals(17, loaded.intervalMinutes)
        assertEquals(17 * 60_000L, loaded.intervalMillis)
        assertEquals(LocalTime.of(8, 0), loaded.activeStart)
        assertEquals(LocalTime.of(23, 15), loaded.activeEnd)
        assertEquals(8_000, loaded.displayDurationMillis)
        assertNull(loaded.pausedUntil)
    }

    @Test
    fun `unreadable or syntactically broken properties degrade to defaults`() = withStore { store, file ->
        file.writeText("broken=\\uNOTHEX")
        assertEquals(DesktopVocabularyReminderSettings(), store.load())
    }

    private fun withStore(block: (DesktopVocabularyReminderSettingsStore, java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("desktop-reminder-settings")
        try {
            val file = directory.resolve(DesktopVocabularyReminderSettingsStore.FILE_NAME)
            block(DesktopVocabularyReminderSettingsStore(file), file)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
