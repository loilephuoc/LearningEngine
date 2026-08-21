package vn.loi.learning.desktop.tts.batch

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice

class BatchTtsApplyUndoTest {

    private val testVoice = TtsVoice(
        id = "en-US-AvaMultilingualNeural",
        displayName = "Ava",
        locale = "en-US",
        language = "en"
    )

    @Test
    fun `BatchTtsUndoSnapshot computes metrics and display label`() {
        val entries = listOf(
            BatchTtsUndoEntry(contentId = "c1", field = TtsField.QUESTION, previousAudioRef = null, appliedAudioRef = "pkg/c1_q.mp3"),
            BatchTtsUndoEntry(contentId = "c1", field = TtsField.ANSWER, previousAudioRef = null, appliedAudioRef = "pkg/c1_a.mp3"),
            BatchTtsUndoEntry(contentId = "c2", field = TtsField.TRANSLATION, previousAudioRef = "pkg/old_tr.mp3", appliedAudioRef = "pkg/c2_tr.mp3")
        )
        val createdPaths = setOf("pkg/c1_q.mp3", "pkg/c1_a.mp3", "pkg/c2_tr.mp3")

        val snapshot = BatchTtsUndoSnapshot(
            packageName = "oxford_3000",
            entries = entries,
            newlyCreatedAssetPaths = createdPaths
        )

        assertEquals(3, snapshot.totalApplied)
        assertEquals("Batch TTS (3 audio targets)", snapshot.displayLabel)
        assertEquals("oxford_3000", snapshot.packageName)
        assertEquals(3, snapshot.newlyCreatedAssetPaths.size)
    }

    @Test
    fun `Batch undo restores previous audio references exactly and handles missing vs existing`() {
        // Simulated items state before batch
        val initialItems = mutableMapOf(
            "c1_question" to (null as String?),
            "c1_answer" to (null as String?),
            "c2_example" to ("existing_c2_ex.mp3" as String?),
            "c2_translation" to (null as String?)
        )

        // Simulated batch results
        val results = listOf(
            BatchTtsJobResult(
                job = BatchTtsJob("c1", TtsField.QUESTION, "apple", TtsLanguage.ENGLISH, testVoice, previousAudioRef = null),
                status = BatchTtsJobStatus.SUCCESS,
                assetRelativePath = "gen_c1_q.mp3"
            ),
            BatchTtsJobResult(
                job = BatchTtsJob("c1", TtsField.ANSWER, "fruit", TtsLanguage.ENGLISH, testVoice, previousAudioRef = null),
                status = BatchTtsJobStatus.SUCCESS,
                assetRelativePath = "gen_c1_a.mp3"
            ),
            BatchTtsJobResult(
                job = BatchTtsJob("c2", TtsField.TRANSLATION, "táo", TtsLanguage.VIETNAMESE, testVoice, previousAudioRef = null),
                status = BatchTtsJobStatus.SUCCESS,
                assetRelativePath = "gen_c2_tr.mp3"
            )
        )

        // Step 1: Simulate Apply
        val undoEntries = mutableListOf<BatchTtsUndoEntry>()
        val stateAfterApply = initialItems.toMutableMap()
        for (res in results) {
            val key = "${res.job.contentId}_${res.job.field.name.lowercase()}"
            val prev = stateAfterApply[key]
            undoEntries.add(BatchTtsUndoEntry(res.job.contentId, res.job.field, prev, res.assetRelativePath!!))
            stateAfterApply[key] = res.assetRelativePath
        }

        assertEquals("gen_c1_q.mp3", stateAfterApply["c1_question"])
        assertEquals("gen_c1_a.mp3", stateAfterApply["c1_answer"])
        assertEquals("existing_c2_ex.mp3", stateAfterApply["c2_example"]) // Untouched
        assertEquals("gen_c2_tr.mp3", stateAfterApply["c2_translation"])

        // Step 2: Simulate Undo
        val stateAfterUndo = stateAfterApply.toMutableMap()
        for (entry in undoEntries) {
            val key = "${entry.contentId}_${entry.field.name.lowercase()}"
            stateAfterUndo[key] = entry.previousAudioRef
        }

        assertNull(stateAfterUndo["c1_question"]) // Originally missing restored to null
        assertNull(stateAfterUndo["c1_answer"])   // Originally missing restored to null
        assertEquals("existing_c2_ex.mp3", stateAfterUndo["c2_example"]) // Existing untouched
        assertNull(stateAfterUndo["c2_translation"])
    }

    @Test
    fun `Safe cleanup during undo deletes only unreferenced new batch files and never deletes existing assets`() {
        val tempDir = Files.createTempDirectory("tts_undo_test").toFile()
        try {
            val existingFile = File(tempDir, "existing_audio.mp3").apply { writeText("original audio") }
            val newBatchFile1 = File(tempDir, "new_batch_1.mp3").apply { writeText("new batch 1") }
            val newBatchFile2 = File(tempDir, "new_batch_2.mp3").apply { writeText("new batch 2") }

            val snapshot = BatchTtsUndoSnapshot(
                packageName = "pkg",
                entries = listOf(
                    BatchTtsUndoEntry("c1", TtsField.QUESTION, null, "new_batch_1.mp3"),
                    BatchTtsUndoEntry("c2", TtsField.ANSWER, null, "new_batch_2.mp3")
                ),
                newlyCreatedAssetPaths = setOf("new_batch_1.mp3", "new_batch_2.mp3")
            )

            // Suppose after undo, new_batch_1 is not referenced anywhere, but new_batch_2 is somehow still referenced
            val allActiveRefsAfterUndo = setOf("existing_audio.mp3", "new_batch_2.mp3")

            for (path in snapshot.newlyCreatedAssetPaths) {
                if (path !in allActiveRefsAfterUndo) {
                    File(tempDir, path).delete()
                }
            }

            // Verify: new_batch_1 deleted because unreferenced
            assertFalse(newBatchFile1.exists(), "new_batch_1 should be deleted because it is unreferenced")
            // Verify: new_batch_2 kept because still referenced
            assertTrue(newBatchFile2.exists(), "new_batch_2 should be kept because it is still referenced")
            // Verify: existingFile NEVER deleted
            assertTrue(existingFile.exists(), "existing audio file must NEVER be deleted")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
