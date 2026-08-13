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
                intervalMinutes = 30,
                activeStart = LocalTime.of(22, 0),
                activeEnd = LocalTime.of(6, 30),
                displayDurationSeconds = 12,
                pausedUntil = Instant.parse("2026-08-14T00:00:00Z")
            )
            store.save(settings)
            assertEquals(settings, store.load())
            assertEquals(settings, DesktopVocabularyReminderSettingsStore(file).load())
            assertEquals(
                """enabled=true
installed.package.id=unavailable-package-is-preserved
selection.mode=RANDOM_LEARNED
interval.minutes=30
active.start=22:00
active.end=06:30
display.duration.seconds=12
paused.until.epoch.millis=1786665600000
""",
                file.readText()
            )
            assertTrue(Files.list(file.parent).use { files ->
                files.noneMatch { it.fileName.toString().endsWith(".tmp") }
            })
        }

    @Test
    fun `equal active times survive round trip as 24 hour window`() = withStore { store, _ ->
        val settings = DesktopVocabularyReminderSettings(activeStart = LocalTime.NOON, activeEnd = LocalTime.NOON)
        store.save(settings)
        assertEquals(settings, store.load())
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
display.duration.seconds=2
paused.until.epoch.millis=not-a-long
"""
        )
        val loaded = store.load()
        assertFalse(loaded.enabled)
        assertEquals(InstalledPackageId("kept-package"), loaded.selectedPackageId)
        assertEquals(DesktopVocabularyReminderSelectionMode.AGAIN_HARD, loaded.selectionMode)
        assertEquals(15, loaded.intervalMinutes)
        assertEquals(LocalTime.of(8, 0), loaded.activeStart)
        assertEquals(LocalTime.of(23, 15), loaded.activeEnd)
        assertEquals(8, loaded.displayDurationSeconds)
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
