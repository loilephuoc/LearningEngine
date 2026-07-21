package vn.loi.learning.application.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionDescriptor
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryCollectionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class RenameLibraryCollectionUseCaseTest {

    private val collectionId =
        LibraryCollectionId(
            "collection-english"
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
    fun `execute renames and saves existing collection`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        val original =
            createCollection(
                name = "English"
            )

        repository.save(
            original
        )

        val useCase =
            createUseCase(
                repository
            )

        val result =
            useCase.execute(
                RenameLibraryCollectionCommand(
                    collectionId = collectionId,
                    name = "English Language"
                )
            )

        assertEquals(
            collectionId,
            result.id
        )

        assertEquals(
            libraryId,
            result.libraryId
        )

        assertEquals(
            "English Language",
            result.name
        )

        assertEquals(
            setOf(packageId),
            result.packageIds
        )

        assertEquals(
            result,
            repository.findById(
                collectionId
            )
        )
    }

    @Test
    fun `execute preserves original collection value`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        val original =
            createCollection(
                name = "English"
            )

        repository.save(
            original
        )

        val result =
            createUseCase(
                repository
            ).execute(
                RenameLibraryCollectionCommand(
                    collectionId = collectionId,
                    name = "English Language"
                )
            )

        assertEquals(
            "English",
            original.name
        )

        assertEquals(
            "English Language",
            result.name
        )
    }

    @Test
    fun `execute with unchanged name returns same instance`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        val collection =
            createCollection(
                name = "English"
            )

        repository.save(
            collection
        )

        val result =
            createUseCase(
                repository
            ).execute(
                RenameLibraryCollectionCommand(
                    collectionId = collectionId,
                    name = "English"
                )
            )

        assertSame(
            collection,
            result
        )

        assertSame(
            collection,
            repository.findById(
                collectionId
            )
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
                    RenameLibraryCollectionCommand(
                        collectionId = collectionId,
                        name = "English Language"
                    )
                )
            }

        assertEquals(
            collectionId,
            exception.collectionId
        )
    }

    @Test
    fun `execute rejects blank name and preserves stored collection`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        val original =
            createCollection(
                name = "English"
            )

        repository.save(
            original
        )

        assertFailsWith<IllegalArgumentException> {
            createUseCase(
                repository
            ).execute(
                RenameLibraryCollectionCommand(
                    collectionId = collectionId,
                    name = " "
                )
            )
        }

        assertEquals(
            original,
            repository.findById(
                collectionId
            )
        )
    }

    private fun createCollection(
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
    ): RenameLibraryCollectionUseCase =
        RenameLibraryCollectionUseCase(
            libraryCollectionRepository = repository,
            transactionRunner = InMemoryTransactionRunner()
        )
}