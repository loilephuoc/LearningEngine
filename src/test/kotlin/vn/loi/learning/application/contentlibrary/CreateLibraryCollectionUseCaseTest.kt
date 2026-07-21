package vn.loi.learning.application.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollectionDescriptor
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryCollectionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class CreateLibraryCollectionUseCaseTest {

    private val libraryId =
        ContentLibraryId(
            "library-main"
        )

    private val collectionId =
        LibraryCollectionId(
            "collection-english"
        )

    @Test
    fun `execute creates and saves collection`() {
        val contentLibraryRepository =
            InMemoryContentLibraryRepository()

        val collectionRepository =
            InMemoryLibraryCollectionRepository()

        contentLibraryRepository.save(
            createLibrary()
        )

        val useCase =
            createUseCase(
                contentLibraryRepository = contentLibraryRepository,
                collectionRepository = collectionRepository
            )

        val result =
            useCase.execute(
                CreateLibraryCollectionCommand(
                    collectionId = collectionId,
                    libraryId = libraryId,
                    name = "English"
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
            "English",
            result.name
        )

        assertEquals(
            result,
            collectionRepository.findById(
                collectionId
            )
        )
    }

    @Test
    fun `execute rejects missing content library`() {
        val contentLibraryRepository =
            InMemoryContentLibraryRepository()

        val collectionRepository =
            InMemoryLibraryCollectionRepository()

        val useCase =
            createUseCase(
                contentLibraryRepository = contentLibraryRepository,
                collectionRepository = collectionRepository
            )

        val exception =
            assertFailsWith<ContentLibraryNotFoundException> {
                useCase.execute(
                    CreateLibraryCollectionCommand(
                        collectionId = collectionId,
                        libraryId = libraryId,
                        name = "English"
                    )
                )
            }

        assertEquals(
            libraryId,
            exception.libraryId
        )

        assertNull(
            collectionRepository.findById(
                collectionId
            )
        )
    }

    @Test
    fun `execute rejects duplicate collection ID`() {
        val contentLibraryRepository =
            InMemoryContentLibraryRepository()

        val collectionRepository =
            InMemoryLibraryCollectionRepository()

        contentLibraryRepository.save(
            createLibrary()
        )

        collectionRepository.save(
            vn.loi.learning.domain.content.library.model.LibraryCollection(
                id = collectionId,
                libraryId = libraryId,
                descriptor =
                    LibraryCollectionDescriptor(
                        name = "Existing"
                    )
            )
        )

        val useCase =
            createUseCase(
                contentLibraryRepository = contentLibraryRepository,
                collectionRepository = collectionRepository
            )

        val exception =
            assertFailsWith<LibraryCollectionAlreadyExistsException> {
                useCase.execute(
                    CreateLibraryCollectionCommand(
                        collectionId = collectionId,
                        libraryId = libraryId,
                        name = "Replacement"
                    )
                )
            }

        assertEquals(
            collectionId,
            exception.collectionId
        )

        assertEquals(
            "Existing",
            collectionRepository.findById(
                collectionId
            )?.name
        )
    }

    @Test
    fun `execute rejects blank collection name`() {
        val contentLibraryRepository =
            InMemoryContentLibraryRepository()

        val collectionRepository =
            InMemoryLibraryCollectionRepository()

        contentLibraryRepository.save(
            createLibrary()
        )

        val useCase =
            createUseCase(
                contentLibraryRepository = contentLibraryRepository,
                collectionRepository = collectionRepository
            )

        assertFailsWith<IllegalArgumentException> {
            useCase.execute(
                CreateLibraryCollectionCommand(
                    collectionId = collectionId,
                    libraryId = libraryId,
                    name = " "
                )
            )
        }

        assertNull(
            collectionRepository.findById(
                collectionId
            )
        )
    }

    private fun createLibrary(): ContentLibrary =
        ContentLibrary(
            id = libraryId,
            descriptor =
                LibraryDescriptor(
                    name = "Main Library"
                )
        )

    private fun createUseCase(
        contentLibraryRepository: InMemoryContentLibraryRepository,
        collectionRepository: InMemoryLibraryCollectionRepository
    ): CreateLibraryCollectionUseCase =
        CreateLibraryCollectionUseCase(
            contentLibraryRepository = contentLibraryRepository,
            libraryCollectionRepository = collectionRepository,
            transactionRunner = InMemoryTransactionRunner()
        )
}
