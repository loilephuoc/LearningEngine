package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.infrastructure.persistence.record.ContentLibraryRecord

class JsonContentLibraryStoreTest {

    @Test
    fun `saves and loads content library records`() {
        val directory =
            Files.createTempDirectory(
                "json-content-library-store-test"
            )

        val filePath =
            directory.resolve("content-libraries.json")

        val store =
            JsonContentLibraryStore(filePath)

        val records = listOf(
            ContentLibraryRecord(
                id = "library-vocabulary",
                name = "Vocabulary",
                contentIds = setOf(
                    "content-hello",
                    "content-goodbye"
                )
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
                "json-content-library-store-missing-test"
            )

        val filePath =
            directory.resolve("missing.json")

        val store =
            JsonContentLibraryStore(filePath)

        assertEquals(
            emptyList(),
            store.loadAll()
        )
    }
}
