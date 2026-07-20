package vn.loi.learning.infrastructure.persistence.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.record.PackageRecord
import vn.loi.learning.infrastructure.persistence.store.ContentPackageStore

class StoreBackedContentPackageRepositoryTest {

    @Test
    fun `saves and finds package by id`() {
        val repository =
            StoreBackedContentPackageRepository(
                CountingContentPackageStore()
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
                CountingContentPackageStore()
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
    fun `returns all stored packages in insertion order`() {
        val repository =
            StoreBackedContentPackageRepository(
                CountingContentPackageStore()
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

    @Test
    fun `saving identical package does not write store again`() {
        val store =
            CountingContentPackageStore()

        val repository =
            StoreBackedContentPackageRepository(
                store
            )

        val contentPackage =
            createPackage(
                id = "package-english",
                version = "1.0.0"
            )

        repository.save(contentPackage)
        repository.save(contentPackage)

        assertEquals(
            1,
            store.saveCount
        )
    }

    @Test
    fun `deleting missing package does not write store`() {
        val store =
            CountingContentPackageStore()

        val repository =
            StoreBackedContentPackageRepository(
                store
            )

        repository.deleteById(
            PackageId("missing-package")
        )

        assertEquals(
            0,
            store.saveCount
        )
    }

    @Test
    fun `deleting existing package writes once and removes it`() {
        val store =
            CountingContentPackageStore()

        val repository =
            StoreBackedContentPackageRepository(
                store
            )

        val contentPackage =
            createPackage(
                id = "package-english",
                version = "1.0.0"
            )

        repository.save(contentPackage)

        store.resetSaveCount()

        repository.deleteById(
            contentPackage.id
        )

        assertEquals(
            1,
            store.saveCount
        )

        assertNull(
            repository.findById(
                contentPackage.id
            )
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

    private class CountingContentPackageStore(
        initialRecords: List<PackageRecord> = emptyList()
    ) : ContentPackageStore {

        private var records =
            initialRecords.toList()

        var saveCount: Int = 0
            private set

        override fun loadAll(): List<PackageRecord> =
            records.toList()

        override fun saveAll(
            records: List<PackageRecord>
        ) {
            saveCount += 1
            this.records = records.toList()
        }

        fun resetSaveCount() {
            saveCount = 0
        }
    }
}