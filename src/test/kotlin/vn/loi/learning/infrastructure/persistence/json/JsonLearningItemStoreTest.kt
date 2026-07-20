package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.infrastructure.persistence.record.LearningItemRecord

class JsonLearningItemStoreTest {

    @Test
    fun `saves and loads learning item records`() {
        val directory =
            Files.createTempDirectory(
                "json-learning-item-store-test"
            )

        val filePath =
            directory.resolve("learning-items.json")

        val store =
            JsonLearningItemStore(filePath)

        val records = listOf(
            LearningItemRecord(
                id = "item-hello",
                contentId = "content-hello",
                mode = "MEANING_RECOGNITION",
                isEnabled = true
            )
        )

        store.saveAll(records)

        val restored =
            store.loadAll()

        assertEquals(records, restored)
    }

    @Test
    fun `returns empty list when file does not exist`() {
        val directory =
            Files.createTempDirectory(
                "json-learning-item-store-missing-test"
            )

        val filePath =
            directory.resolve("missing.json")

        val store =
            JsonLearningItemStore(filePath)

        assertEquals(
            emptyList(),
            store.loadAll()
        )
    }
}
