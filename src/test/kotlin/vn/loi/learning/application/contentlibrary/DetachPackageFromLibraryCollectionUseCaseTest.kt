package vn.loi.learning.application.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionDescriptor
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryCollectionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class DetachPackageFromLibraryCollectionUseCaseTest {

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

    private val retainedPackageId =
        PackageId(
            "package-essential-grammar"
        )

    @Test
    fun `execute detaches package from collection`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        val original =
            createCollection(
                packageIds =
                    setOf(
                        packageId,
                        retainedPackageId
                    )
            )

        repository.save(
            original
        )

        val result =
            createUseCase(
                repository
            ).execute(
                DetachPackageFromLibraryCollectionCommand(
                    collectionId = collectionId,
                    packageId = packageId
                )
            )

        assertTrue(
            original.contains(
                packageId
            )
        )

        assertFalse(
            result.contains(
                packageId
            )
        )

        assertTrue(
            result.contains(
                retainedPackageId
            )
        )

        assertEquals(
            1,
            result.packageCount
        )

        assertEquals(
            result,
            repository.findById(
                collectionId
            )
        )
    }

    @Test
    fun `execute preserves collection identity ownership and descriptor`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        repository.save(
            createCollection(
                packageIds = setOf(packageId)
            )
        )

        val result =
            createUseCase(
                repository
            ).execute(
                DetachPackageFromLibraryCollectionCommand(
                    collectionId = collectionId,
                    packageId = packageId
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
    }

    @Test
    fun `execute detaching missing package returns same instance`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        val collection =
            createCollection(
                packageIds = setOf(retainedPackageId)
            )

        repository.save(
            collection
        )

        val result =
            createUseCase(
                repository
            ).execute(
                DetachPackageFromLibraryCollectionCommand(
                    collectionId = collectionId,
                    packageId = packageId
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
    fun `execute can make collection empty`() {
        val repository =
            InMemoryLibraryCollectionRepository()

        repository.save(
            createCollection(
                packageIds = setOf(packageId)
            )
        )

        val result =
            createUseCase(
                repository
            ).execute(
                DetachPackageFromLibraryCollectionCommand(
                    collectionId = collectionId,
                    packageId = packageId
                )
            )

        assertTrue(
            result.isEmpty
        )

        assertEquals(
            0,
            result.packageCount
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
                    DetachPackageFromLibraryCollectionCommand(
                        collectionId = collectionId,
                        packageId = packageId
                    )
                )
            }

        assertEquals(
            collectionId,
            exception.collectionId
        )
    }

    private fun createCollection(
        packageIds: Set<PackageId>
    ): LibraryCollection =
        LibraryCollection(
            id = collectionId,
            libraryId = libraryId,
            descriptor =
                LibraryCollectionDescriptor(
                    name = "English"
                ),
            packageIds = packageIds
        )

    private fun createUseCase(
        repository: InMemoryLibraryCollectionRepository
    ): DetachPackageFromLibraryCollectionUseCase =
        DetachPackageFromLibraryCollectionUseCase(
            libraryCollectionRepository = repository,
            transactionRunner = InMemoryTransactionRunner()
        )
}