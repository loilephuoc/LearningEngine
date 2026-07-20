package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.infrastructure.persistence.record.ContentRecord

class JsonContentStoreTest {

    @Test
    fun `saves and loads content records`() {
        val directory =
            Files.createTempDirectory(
                "json-content-store-test"
            )

        val filePath =
            directory.resolve("contents.json")

        val store =
            JsonContentStore(filePath)

        val records = listOf(
            ContentRecord(
                id = "content-hello",
                type = "SENTENCE",
                primaryText = "Hello.",
                translatedText = "Xin chào.",
                pronunciation = "/həˈləʊ/",
                exampleText = "Hello, everyone.",
                exampleTranslation = "Xin chào mọi người."
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
                "json-content-store-missing-test"
            )

        val filePath =
            directory.resolve("missing.json")

        val store =
            JsonContentStore(filePath)

        assertEquals(
            emptyList(),
            store.loadAll()
        )
    }
}
