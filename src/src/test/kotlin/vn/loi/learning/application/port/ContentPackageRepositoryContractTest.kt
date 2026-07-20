package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class ContentPackageRepositoryContractTest {

    private fun createRepository(): ContentPackageRepository =
        InMemoryContentPackageRepository()

    private fun createPackage(
        id: String,
        name: String = "Package $id",
        libraryIds: Set<ContentLibraryId> = emptySet()
    ): ContentPackage =
        ContentPackage(
            id = PackageId(id),
            descriptor = PackageDescriptor(
                name = name,
                version = "1.0.0",
                format = "OPD3"
            ),
            libraryIds = libraryIds
        )

    @Test
    fun `findById returns null when package does not exist`() {
        val repository =
            createRepository()

        assertNull(
            repository.findById(
                PackageId("missing-package")
            )
        )
    }

    @Test
    fun `save stores package and findById returns it`() {
        val repository =
            createRepository()

        val contentPackage =
            createPackage(
                id = "package-1"
            )

        repository.save(
            contentPackage
        )

        assertEquals(
            contentPackage,
            repository.findById(
                contentPackage.id
            )
        )
    }

    @Test
    fun `save replaces package with the same ID`() {
        val repository =
            createRepository()

        val original =
            createPackage(
                id = "package-1",
                name = "Original Package"
            )

        val updated =
            createPackage(
                id = "package-1",
                name = "Updated Package",
                libraryIds = setOf(
                    ContentLibraryId("library-1")
                )
            )

        repository.save(original)
        repository.save(updated)

        assertEquals(
            updated,
            repository.findById(
                updated.id
            )
        )

        assertEquals(
            listOf(updated),
            repository.findAll()
        )
    }

    @Test
    fun `findAll returns packages in insertion order`() {
        val repository =
            createRepository()

        val first =
            createPackage(
                id = "package-1"
            )

        val second =
            createPackage(
                id = "package-2"
            )

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }

    @Test
    fun `saved package is not affected by changes to source mutable set`() {
        val repository =
            createRepository()

        val sourceLibraryIds =
            mutableSetOf(
                ContentLibraryId("library-1")
            )

        val contentPackage =
            createPackage(
                id = "package-1",
                libraryIds = sourceLibraryIds.toSet()
            )

        repository.save(contentPackage)

        sourceLibraryIds.add(
            ContentLibraryId("library-2")
        )

        assertEquals(
            setOf(
                ContentLibraryId("library-1")
            ),
            repository.findById(
                contentPackage.id
            )?.libraryIds
        )
    }
}
