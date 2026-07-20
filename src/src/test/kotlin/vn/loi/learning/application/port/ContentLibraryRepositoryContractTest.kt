package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository

class ContentLibraryRepositoryContractTest {

    private fun createRepository(): ContentLibraryRepository =
        InMemoryContentLibraryRepository()

    @Test
    fun `findById returns null when library does not exist`() {
        val repository =
            createRepository()

        assertNull(
            repository.findById(
                ContentLibraryId("missing-library")
            )
        )
    }

    @Test
    fun `saved library can be found by ID`() {
        val repository =
            createRepository()

        val library =
            ContentLibrary(
                id = ContentLibraryId("library-1"),
                descriptor = LibraryDescriptor(
                    name = "Vocabulary In Use"
                )
            )

        repository.save(library)

        assertEquals(
            library,
            repository.findById(library.id)
        )
    }

    @Test
    fun `saving same ID replaces previous aggregate`() {
        val repository =
            createRepository()

        val libraryId =
            ContentLibraryId("library-1")

        val original =
            ContentLibrary(
                id = libraryId,
                descriptor = LibraryDescriptor(
                    name = "Original"
                )
            )

        val updated =
            ContentLibrary(
                id = libraryId,
                descriptor = LibraryDescriptor(
                    name = "Updated"
                ),
                contentIds = setOf(
                    ContentId("content-1")
                )
            )

        repository.save(original)
        repository.save(updated)

        assertEquals(
            updated,
            repository.findById(libraryId)
        )

        assertEquals(
            listOf(updated),
            repository.findAll()
        )
    }

    @Test
    fun `findAll returns libraries in insertion order`() {
        val repository =
            createRepository()

        val first =
            ContentLibrary(
                id = ContentLibraryId("library-1"),
                descriptor = LibraryDescriptor(
                    name = "First"
                )
            )

        val second =
            ContentLibrary(
                id = ContentLibraryId("library-2"),
                descriptor = LibraryDescriptor(
                    name = "Second"
                )
            )

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }
}
