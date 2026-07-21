package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionDescriptor
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.record.LibraryCollectionRecord

class LibraryCollectionRecordMapperTest {

    @Test
    fun `toRecord maps collection to persistence record`() {
        val collection =
            createCollection()

        val result =
            LibraryCollectionRecordMapper.toRecord(
                collection
            )

        assertEquals(
            LibraryCollectionRecord(
                id = "collection-english",
                libraryId = "library-main",
                name = "English",
                packageIds =
                    setOf(
                        "package-oxford-3000",
                        "package-essential-grammar"
                    )
            ),
            result
        )
    }

    @Test
    fun `toDomain maps persistence record to collection`() {
        val record =
            LibraryCollectionRecord(
                id = "collection-english",
                libraryId = "library-main",
                name = "English",
                packageIds =
                    setOf(
                        "package-oxford-3000",
                        "package-essential-grammar"
                    )
            )

        val result =
            LibraryCollectionRecordMapper.toDomain(
                record
            )

        assertEquals(
            createCollection(),
            result
        )
    }

    @Test
    fun `mapper round trip preserves collection`() {
        val collection =
            createCollection()

        val result =
            LibraryCollectionRecordMapper.toDomain(
                LibraryCollectionRecordMapper.toRecord(
                    collection
                )
            )

        assertEquals(
            collection,
            result
        )
    }

    @Test
    fun `mapper preserves empty package set`() {
        val collection =
            LibraryCollection(
                id =
                    LibraryCollectionId(
                        "collection-empty"
                    ),
                libraryId =
                    ContentLibraryId(
                        "library-main"
                    ),
                descriptor =
                    LibraryCollectionDescriptor(
                        name = "Empty"
                    ),
                packageIds = emptySet()
            )

        val record =
            LibraryCollectionRecordMapper.toRecord(
                collection
            )

        val restored =
            LibraryCollectionRecordMapper.toDomain(
                record
            )

        assertEquals(
            emptySet(),
            record.packageIds
        )

        assertEquals(
            collection,
            restored
        )
    }

    private fun createCollection(): LibraryCollection =
        LibraryCollection(
            id =
                LibraryCollectionId(
                    "collection-english"
                ),
            libraryId =
                ContentLibraryId(
                    "library-main"
                ),
            descriptor =
                LibraryCollectionDescriptor(
                    name = "English"
                ),
            packageIds =
                setOf(
                    PackageId(
                        "package-oxford-3000"
                    ),
                    PackageId(
                        "package-essential-grammar"
                    )
                )
        )
}