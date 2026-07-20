package vn.loi.learning.infrastructure.persistence.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.store.InMemoryContentPackageStore

class StoreBackedContentPackageRepositoryTest {

    @Test
    fun `saves and finds package by id`() {
        val repository =
            StoreBackedContentPackageRepository(
                InMemoryContentPackageStore()
            )

        val contentPackage =
            createPackage(
                id = "package-english",
                version = "1.0.0"
            )

        repository.save(contentPackage)

        assertEquals(
            contentPackage,
            repository.findById(contentPackage.id)
        )
        assertNull(
            repository.findById(
                PackageId("package-missing")
            )
        )
    }

    @Test
    fun `saving same package id replaces previous record`() {
        val repository =
            StoreBackedContentPackageRepository(
                InMemoryContentPackageStore()
            )

        val original =
            createPackage(
                id = "package-english",
                version = "1.0.0"
            )

        val updated =
            createPackage(
                id = "package-english",
                version = "2.0.0",
                libraryIds = setOf(
                    ContentLibraryId("library-updated")
                )
            )

        repository.save(original)
        repository.save(updated)

        assertEquals(
            updated,
            repository.findById(updated.id)
        )
        assertEquals(
            listOf(updated),
            repository.findAll()
        )
    }

    @Test
    fun `returns all stored packages`() {
        val repository =
            StoreBackedContentPackageRepository(
                InMemoryContentPackageStore()
            )

        val first =
            createPackage(
                id = "package-one",
                version = "1.0.0"
            )

        val second =
            createPackage(
                id = "package-two",
                version = "2.0.0"
            )

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }

    private fun createPackage(
        id: String,
        version: String,
        libraryIds: Set<ContentLibraryId> = emptySet()
    ): ContentPackage =
        ContentPackage(
            id = PackageId(id),
            descriptor = PackageDescriptor(
                name = "English Package",
                version = version,
                format = "OPD3"
            ),
            libraryIds = libraryIds
        )
}
