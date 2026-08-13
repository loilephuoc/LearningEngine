package vn.loi.learning.desktop.notification

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId

class DesktopVocabularyReminderDifficultStoreTest {
    @Test
    fun `default empty toggle is idempotent and restart preserves deterministic markers`() {
        val directory = Files.createTempDirectory("reminder-difficult")
        try {
            val file = directory.resolve(DesktopVocabularyReminderDifficultStore.FILE_NAME)
            val store = DesktopVocabularyReminderDifficultStore(file)
            val a = ContentId("a")
            val b = ContentId("b/value")
            assertEquals(emptySet(), store.markedContentIds())
            assertTrue(store.toggle(a))
            assertTrue(store.toggle(b))
            assertTrue(store.isMarked(a))
            assertEquals(setOf(a, b), DesktopVocabularyReminderDifficultStore(file).markedContentIds())
            assertFalse(store.toggle(a))
            assertFalse(store.isMarked(a))
            assertEquals(setOf(b), DesktopVocabularyReminderDifficultStore(file).markedContentIds())
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test
    fun `malformed file safely falls back to empty`() {
        val directory = Files.createTempDirectory("reminder-difficult-malformed")
        try {
            val file = directory.resolve(DesktopVocabularyReminderDifficultStore.FILE_NAME)
            file.writeText("content.id.base64=%%%broken%%%\n")
            assertEquals(emptySet(), DesktopVocabularyReminderDifficultStore(file).markedContentIds())
        } finally { directory.toFile().deleteRecursively() }
    }
}
