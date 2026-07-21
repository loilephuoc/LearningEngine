package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.infrastructure.persistence.record.LibraryCollectionRecord

class JsonLibraryCollectionStoreTest {

    @Test
    fun `saves and loads library collection records`() {
        val directory =
            Files.createTempDirectory(
                "json-library-collection-store-test"
            )

        val filePath =
            directory.resolve(
                "library-collections.json"
            )

        val store =
            JsonLibraryCollectionStore(
                filePath
            )

        val records =
            listOf(
                LibraryCollectionRecord(
                    id = "collection-english",
                    libraryId = "library-main",
                    name = "English",
                    packageIds =
                        setOf(
                            "package-vocabulary",
                            "package-grammar"
                        )
                ),
                LibraryCollectionRecord(
                    id = "collection-physics",
                    libraryId = "library-main",
                    name = "Physics",
                    packageIds = emptySet()
                )
            )

        store.saveAll(
            records
        )

        val restored =
            store.loadAll()

        assertEquals(
            records,
            restored
        )
    }

    @Test
    fun `returns empty list when file does not exist`() {
        val directory =
            Files.createTempDirectory(
                "json-library-collection-store-missing-test"
            )

        val filePath =
            directory.resolve(
                "missing.json"
            )

        val store =
            JsonLibraryCollectionStore(
                filePath
            )

        assertEquals(
            emptyList(),
            store.loadAll()
        )
    }

    @Test
    fun `loads legacy raw record list`() {
        val directory =
            Files.createTempDirectory(
                "json-library-collection-store-legacy-test"
            )

        val filePath =
            directory.resolve(
                "library-collections.json"
            )

        Files.writeString(
            filePath,
            """
            [
              {
                "id": "collection-english",
                "libraryId": "library-main",
                "name": "English",
                "packageIds": [
                  "package-vocabulary"
                ]
              }
            ]
            """.trimIndent()
        )

        val store =
            JsonLibraryCollectionStore(
                filePath
            )

        assertEquals(
            listOf(
                LibraryCollectionRecord(
                    id = "collection-english",
                    libraryId = "library-main",
                    name = "English",
                    packageIds =
                        setOf(
                            "package-vocabulary"
                        )
                )
            ),
            store.loadAll()
        )
    }

    @Test
    fun `saved records can be loaded by new store instance`() {
        val directory =
            Files.createTempDirectory(
                "json-library-collection-store-reopen-test"
            )

        val filePath =
            directory.resolve(
                "library-collections.json"
            )

        val records =
            listOf(
                LibraryCollectionRecord(
                    id = "collection-english",
                    libraryId = "library-main",
                    name = "English",
                    packageIds =
                        setOf(
                            "package-vocabulary"
                        )
                )
            )

        JsonLibraryCollectionStore(
            filePath
        ).saveAll(
            records
        )

        val reopenedStore =
            JsonLibraryCollectionStore(
                filePath
            )

        assertEquals(
            records,
            reopenedStore.loadAll()
        )
    }
}