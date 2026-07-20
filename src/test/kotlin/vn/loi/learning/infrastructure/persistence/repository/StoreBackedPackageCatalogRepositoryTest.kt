package vn.loi.learning.infrastructure.persistence.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.record.PackageCatalogRecord
import vn.loi.learning.infrastructure.persistence.store.PackageCatalogStore

class StoreBackedPackageCatalogRepositoryTest {

    @Test
    fun `saves and finds catalog by id`() {
        val repository =
            StoreBackedPackageCatalogRepository(
                CountingPackageCatalogStore()
            )

        val catalog =
            createCatalog(
                id = "catalog-main"
            )

        repository.save(catalog)

        assertEquals(
            catalog,
            repository.findById(catalog.id)
        )

        assertNull(
            repository.findById(
                PackageCatalogId("missing-catalog")
            )
        )
    }

    @Test
    fun `saving catalog with same id replaces previous record`() {
        val repository =
            StoreBackedPackageCatalogRepository(
                CountingPackageCatalogStore()
            )

        val original =
            createCatalog(
                id = "catalog-main"
            )

        val updated =
            createCatalog(
                id = "catalog-main",
                packageIds = setOf(
                    PackageId("package-english")
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
    fun `returns all catalogs in insertion order`() {
        val repository =
            StoreBackedPackageCatalogRepository(
                CountingPackageCatalogStore()
            )

        val first =
            createCatalog(
                id = "catalog-one"
            )

        val second =
            createCatalog(
                id = "catalog-two"
            )

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }

    @Test
    fun `saving identical catalog does not write store again`() {
        val store =
            CountingPackageCatalogStore()

        val repository =
            StoreBackedPackageCatalogRepository(
                store
            )

        val catalog =
            createCatalog(
                id = "catalog-main",
                packageIds = setOf(
                    PackageId("package-english")
                )
            )

        repository.save(catalog)
        repository.save(catalog)

        assertEquals(
            1,
            store.saveCount
        )
    }

    @Test
    fun `deleting missing catalog does not write store`() {
        val store =
            CountingPackageCatalogStore()

        val repository =
            StoreBackedPackageCatalogRepository(
                store
            )

        repository.deleteById(
            PackageCatalogId("missing-catalog")
        )

        assertEquals(
            0,
            store.saveCount
        )
    }

    @Test
    fun `deleting existing catalog writes once and removes it`() {
        val store =
            CountingPackageCatalogStore()

        val repository =
            StoreBackedPackageCatalogRepository(
                store
            )

        val catalog =
            createCatalog(
                id = "catalog-main"
            )

        repository.save(catalog)

        store.resetSaveCount()

        repository.deleteById(
            catalog.id
        )

        assertEquals(
            1,
            store.saveCount
        )

        assertNull(
            repository.findById(
                catalog.id
            )
        )
    }

    private fun createCatalog(
        id: String,
        packageIds: Set<PackageId> = emptySet()
    ): PackageCatalog =
        PackageCatalog(
            id = PackageCatalogId(id),
            packageIds = packageIds
        )

    private class CountingPackageCatalogStore(
        initialRecords: List<PackageCatalogRecord> = emptyList()
    ) : PackageCatalogStore {

        private var records =
            initialRecords.toList()

        var saveCount: Int = 0
            private set

        override fun loadAll(): List<PackageCatalogRecord> =
            records.toList()

        override fun saveAll(
            records: List<PackageCatalogRecord>
        ) {
            saveCount += 1
            this.records = records.toList()
        }

        fun resetSaveCount() {
            saveCount = 0
        }
    }
}