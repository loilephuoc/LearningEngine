package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.infrastructure.persistence.record.PackageRecord

class JsonContentPackageStoreTest {

    @Test
    fun `saves and loads package records`() {
        val directory =
            Files.createTempDirectory(
                "json-content-package-store-test"
            )

        val filePath =
            directory.resolve("packages.json")

        val store =
            JsonContentPackageStore(filePath)

        val records = listOf(
            PackageRecord(
                id = "package-english",
                name = "English Elementary",
                version = "1.0.0",
                format = "OPD3",
                topicId = "topic-english",
                libraryIds = setOf(
                    "library-vocabulary",
                    "library-conversations"
                )
            )
        )

        store.saveAll(records)

        val restored =
            store.loadAll()

        assertEquals(records, restored)
    }

    @Test
    fun `loads legacy package json without topic identity`() {
        val directory =
            Files.createTempDirectory(
                "json-content-package-store-legacy-topic"
            )
        val filePath =
            directory.resolve(
                "packages.json"
            )
        Files.writeString(
            filePath,
            """
            [
              {
                "id": "legacy-package",
                "name": "Legacy Topic",
                "version": "1",
                "format": "OPD3"
              }
            ]
            """.trimIndent()
        )

        val restored =
            JsonContentPackageStore(
                filePath
            ).loadAll()

        assertEquals(1, restored.size)
        assertEquals(null, restored.single().topicId)
    }

    @Test
    fun `returns empty list when file does not exist`() {
        val directory =
            Files.createTempDirectory(
                "json-content-package-store-missing-test"
            )

        val filePath =
            directory.resolve("missing.json")

        val store =
            JsonContentPackageStore(filePath)

        assertEquals(
            emptyList(),
            store.loadAll()
        )
    }
}
