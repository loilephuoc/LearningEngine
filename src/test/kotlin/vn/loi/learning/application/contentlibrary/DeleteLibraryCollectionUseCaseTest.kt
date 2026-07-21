package vn.loi.learning.application.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionDescriptor
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryCollectionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class DeleteLibraryCollectionUseCaseTest {

    private val collectionId =
        LibraryCollectionId(
            "collection-english"
        )

    private val retainedCollectionId =
        LibraryCollectionId(
            "collection-mathematics"
        )

    private val libraryId =
        ContentLibraryId(
            "library-main"
        )

    private val packageId =
        PackageId(
            "package-oxford-3000"
        )

    @Test
    fun `execute deletes existing collection`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        repository.save(
            createCollection(
                collectionId = collectionId,
                name = "English"
            )
        )

        createUseCase(
            repository
        ).execute(
            DeleteLibraryCollectionCommand(
                collectionId = collectionId
            )
        )

        assertNull(
            repository.findById(
                collectionId
            )
        )

        assertEquals(
            0,
            repository.count()
        )
    }

    @Test
    fun `execute deletes only requested collection`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        val retainedCollection =
            createCollection(
                collectionId = retainedCollectionId,
                name = "Mathematics"
            )

        repository.saveAll(
            listOf(
                createCollection(
                    collectionId = collectionId,
                    name = "English"
                ),
                retainedCollection
            )
        )

        createUseCase(
            repository
        ).execute(
            DeleteLibraryCollectionCommand(
                collectionId = collectionId
            )
        )

        assertNull(
            repository.findById(
                collectionId
            )
        )

        assertEquals(
            retainedCollection,
            repository.findById(
                retainedCollectionId
            )
        )

        assertEquals(
            1,
            repository.count()
        )
    }

    @Test
    fun `execute rejects missing collection`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        val exception =
            assertFailsWith<LibraryCollectionNotFoundException> {
                createUseCase(
                    repository
                ).execute(
                    DeleteLibraryCollectionCommand(
                        collectionId = collectionId
                    )
                )
            }

        assertEquals(
            collectionId,
            exception.collectionId
        )

        assertEquals(
            0,
            repository.count()
        )
    }

    private fun createCollection(
        collectionId: LibraryCollectionId,
        name: String
    ): LibraryCollection =
        LibraryCollection(
            id = collectionId,
            libraryId = libraryId,
            descriptor =
                LibraryCollectionDescriptor(
                    name = name
                ),
            packageIds =
                setOf(
                    packageId
                )
        )

    private fun createUseCase(
        repository: InMemoryLibraryCollectionRepository
    ): DeleteLibraryCollectionUseCase =
        DeleteLibraryCollectionUseCase(
            libraryCollectionRepository = repository,
            transactionRunner = InMemoryTransactionRunner()
        )
}