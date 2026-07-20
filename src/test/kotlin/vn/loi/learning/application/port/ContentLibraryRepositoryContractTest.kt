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
            createLibrary(
                id = "library-1",
                name = "Vocabulary In Use"
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
            createLibrary(
                id = libraryId.value,
                name = "Original"
            )

        val updated =
            createLibrary(
                id = libraryId.value,
                name = "Updated",
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
    fun `saveAll stores multiple libraries`() {
        val repository =
            createRepository()

        val first =
            createLibrary(
                id = "library-1",
                name = "First"
            )

        val second =
            createLibrary(
                id = "library-2",
                name = "Second"
            )

        repository.saveAll(
            listOf(
                first,
                second
            )
        )

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }

    @Test
    fun `saveAll replaces existing library and preserves others`() {
        val repository =
            createRepository()

        val original =
            createLibrary(
                id = "library-1",
                name = "Original"
            )

        val preserved =
            createLibrary(
                id = "library-2",
                name = "Preserved"
            )

        val updated =
            createLibrary(
                id = "library-1",
                name = "Updated",
                contentIds = setOf(
                    ContentId("content-updated")
                )
            )

        repository.saveAll(
            listOf(
                original,
                preserved
            )
        )

        repository.saveAll(
            listOf(
                updated
            )
        )

        assertEquals(
            listOf(updated, preserved),
            repository.findAll()
        )
    }

    @Test
    fun `deleteAllById removes selected libraries and preserves others`() {
        val repository =
            createRepository()

        val first =
            createLibrary(
                id = "library-1",
                name = "First"
            )

        val second =
            createLibrary(
                id = "library-2",
                name = "Second"
            )

        val preserved =
            createLibrary(
                id = "library-3",
                name = "Preserved"
            )

        repository.saveAll(
            listOf(
                first,
                second,
                preserved
            )
        )

        repository.deleteAllById(
            setOf(
                first.id,
                second.id
            )
        )

        assertNull(
            repository.findById(first.id)
        )

        assertNull(
            repository.findById(second.id)
        )

        assertEquals(
            listOf(preserved),
            repository.findAll()
        )
    }

    @Test
    fun `deleteAllById accepts empty set without changing repository`() {
        val repository =
            createRepository()

        val library =
            createLibrary(
                id = "library-1",
                name = "Preserved"
            )

        repository.save(library)

        repository.deleteAllById(
            emptySet()
        )

        assertEquals(
            listOf(library),
            repository.findAll()
        )
    }

    @Test
    fun `findAll returns libraries in insertion order`() {
        val repository =
            createRepository()

        val first =
            createLibrary(
                id = "library-1",
                name = "First"
            )

        val second =
            createLibrary(
                id = "library-2",
                name = "Second"
            )

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }

    private fun createLibrary(
        id: String,
        name: String,
        contentIds: Set<ContentId> = emptySet()
    ): ContentLibrary =
        ContentLibrary(
            id = ContentLibraryId(id),
            descriptor = LibraryDescriptor(
                name = name
            ),
            contentIds = contentIds
        )
}