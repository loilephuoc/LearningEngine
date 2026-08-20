package vn.loi.learning.android.platform

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.android.recording.VoiceRecordingItem

class AndroidPortableBackupSnapshotTest {
    @Test
    fun `typed preferences preserve durable values and exclude transient runtime state`() {
        val encoded = AndroidPortableBackupSnapshot.encodePreferences(
            mapOf(
                "learning-engine-study" to mapOf("daily.new" to 42, "unknown.runtime" to "drop"),
                "learning_engine_reminder_difficult_prefs" to mapOf("difficult.content.ids" to setOf("c-2", "c-1")),
                "learning_engine_reminder_prefs" to mapOf(
                    "reminder.enabled" to true,
                    "reminder.paused_until_epoch_millis" to 99L,
                    "reminder.unlocked_paused_until_epoch_millis" to 88L,
                    "home_widget.current_candidate_id" to "candidate"
                ),
                "learning-engine-autoplay" to mapOf("autoplay.direction" to "FORWARD", "autoplay.sleep_timer_minutes" to 30)
            ),
            hasBackground = true
        ).toString()

        assertContains(encoded, "daily.new")
        assertContains(encoded, "difficult.content.ids")
        assertContains(encoded, "c-1")
        assertContains(encoded, "reminder.enabled")
        assertContains(encoded, AndroidPortableBackupSnapshot.BACKGROUND_PATH)
        listOf(
            "unknown.runtime", "paused_until_epoch_millis", "unlocked_paused_until_epoch_millis",
            "current_candidate_id", "sleep_timer_minutes", "shuffle"
        ).forEach { assertFalse(it in encoded, it) }
    }

    @Test
    fun `portable recording index preserves metadata without device absolute path`() {
        val deviceRoot = createTempDirectory("device-A-recording-")
        try {
            val absolute = deviceRoot.resolve("quick_voice/voice.m4a").toAbsolutePath().toString()
            val encoded = AndroidPortableBackupSnapshot.encodeRecordingIndex(
                listOf(VoiceRecordingItem("recording-id", "voice.m4a", absolute, 1234L, 5678L, 90L))
            ).toString()
            assertContains(encoded, "recording-id")
            assertContains(encoded, "files/voice.m4a")
            assertContains(encoded, "1234")
            assertContains(encoded, "5678")
            assertContains(encoded, "90")
            assertFalse(absolute in encoded)
            assertTrue("filePath" !in encoded)
        } finally {
            deviceRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun `recording repository snapshots completed files safely and coordinates lock`() {
        val deviceRoot = createTempDirectory("device-repo-test-")
        try {
            val repoDir = deviceRoot.resolve("app_files")
            val targetStaging = deviceRoot.resolve("staging_target")
            val repository = vn.loi.learning.android.recording.QuickVoiceRecordingRepository.forTesting(repoDir.toFile())
            val audioFile = repoDir.resolve("recordings/quick_voice/rec_1.m4a")
            Files.createDirectories(audioFile.parent)
            Files.write(audioFile, byteArrayOf(1, 2, 3, 4, 5))

            kotlinx.coroutines.runBlocking {
                repository.save(VoiceRecordingItem("r-1", "rec_1.m4a", audioFile.toAbsolutePath().toString(), 100L, 500L, 5L))
            }

            val snapshotted = repository.snapshotCompletedForBackup(targetStaging)
            kotlin.test.assertEquals(1, snapshotted.size)
            kotlin.test.assertEquals("rec_1.m4a", snapshotted[0].filename)
            assertTrue(Files.exists(targetStaging.resolve("rec_1.m4a")))
            kotlin.test.assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), Files.readAllBytes(targetStaging.resolve("rec_1.m4a")))
        } finally {
            deviceRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun `preference snapshot gate coordinates exclusive snapshot access`() {
        var counter = 0
        val result = AndroidPreferenceSnapshotGate.snapshot {
            counter += 1
            AndroidPreferenceSnapshotGate.mutation {
                counter += 2
            }
            counter
        }
        kotlin.test.assertEquals(3, result)
    }
}
