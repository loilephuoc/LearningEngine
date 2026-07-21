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
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryCollectionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class AttachPackageToLibraryCollectionUseCaseTest {

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
    fun `execute attaches existing package to collection`() {
        val collectionRepository =
            InMemoryLibraryCollectionRepository()

        val packageRepository =
            InMemoryContentPackageRepository()

        val original =
            createCollection()

        collectionRepository.save(
            original
        )

        packageRepository.save(
            createPackage()
        )

        val result =
            createUseCase(
                collectionRepository = collectionRepository,
                packageRepository = packageRepository
            ).execute(
                AttachPackageToLibraryCollectionCommand(
                    collectionId = collectionId,
                    packageId = packageId
                )
            )

        assertTrue(
            original.isEmpty
        )

        assertTrue(
            result.contains(
                packageId
            )
        )

        assertEquals(
            1,
            result.packageCount
        )

        assertEquals(
            result,
            collectionRepository.findById(
                collectionId
            )
        )
    }

    @Test
    fun `execute preserves collection identity and ownership`() {
        val collectionRepository =
            InMemoryLibraryCollectionRepository()

        val packageRepository =
            InMemoryContentPackageRepository()

        collectionRepository.save(
            createCollection()
        )

        packageRepository.save(
            createPackage()
        )

        val result =
            createUseCase(
                collectionRepository = collectionRepository,
                packageRepository = packageRepository
            ).execute(
                AttachPackageToLibraryCollectionCommand(
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
    fun `execute attaching existing package returns same instance`() {
        val collectionRepository =
            InMemoryLibraryCollectionRepository()

        val packageRepository =
            InMemoryContentPackageRepository()

        val collection =
            createCollection(
                packageIds = setOf(packageId)
            )

        collectionRepository.save(
            collection
        )

        packageRepository.save(
            createPackage()
        )

        val result =
            createUseCase(
                collectionRepository = collectionRepository,
                packageRepository = packageRepository
            ).execute(
                AttachPackageToLibraryCollectionCommand(
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
            collectionRepository.findById(
                collectionId
            )
        )
    }

    @Test
    fun `execute rejects missing collection`() {
        val collectionRepository =
            InMemoryLibraryCollectionRepository()

        val packageRepository =
            InMemoryContentPackageRepository()

        packageRepository.save(
            createPackage()
        )

        val exception =
            assertFailsWith<LibraryCollectionNotFoundException> {
                createUseCase(
                    collectionRepository = collectionRepository,
                    packageRepository = packageRepository
                ).execute(
                    AttachPackageToLibraryCollectionCommand(
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

    @Test
    fun `execute rejects missing package and preserves collection`() {
        val collectionRepository =
            InMemoryLibraryCollectionRepository()

        val packageRepository =
            InMemoryContentPackageRepository()

        val collection =
            createCollection()

        collectionRepository.save(
            collection
        )

        val exception =
            assertFailsWith<ContentPackageNotFoundException> {
                createUseCase(
                    collectionRepository = collectionRepository,
                    packageRepository = packageRepository
                ).execute(
                    AttachPackageToLibraryCollectionCommand(
                        collectionId = collectionId,
                        packageId = packageId
                    )
                )
            }

        assertEquals(
            packageId,
            exception.packageId
        )

        val stored =
            collectionRepository.findById(
                collectionId
            )

        assertEquals(
            collection,
            stored
        )

        assertFalse(
            stored!!.contains(
                packageId
            )
        )
    }

    private fun createCollection(
        packageIds: Set<PackageId> = emptySet()
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

    private fun createPackage(): ContentPackage =
        ContentPackage(
            id = packageId,
            descriptor =
                PackageDescriptor(
                    name = "Oxford 3000",
                    version = "1.0.0",
                    format = "opd3"
                )
        )

    private fun createUseCase(
        collectionRepository: InMemoryLibraryCollectionRepository,
        packageRepository: InMemoryContentPackageRepository
    ): AttachPackageToLibraryCollectionUseCase =
        AttachPackageToLibraryCollectionUseCase(
            libraryCollectionRepository = collectionRepository,
            contentPackageRepository = packageRepository,
            transactionRunner = InMemoryTransactionRunner()
        )
}