package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedPackageCatalogRepository
import vn.loi.learning.infrastructure.persistence.store.InMemoryPackageCatalogStore

class PackageCatalogRepositoryPersistentContractTest {

    private val repository: PackageCatalogRepository =
        StoreBackedPackageCatalogRepository(
            store = InMemoryPackageCatalogStore()
        )

    @Test
    fun `findById returns null when catalog does not exist`() {
        assertNull(
            repository.findById(
                PackageCatalogId("missing-catalog")
            )
        )
    }

    @Test
    fun `save stores catalog and findById returns it`() {
        val catalog = PackageCatalog(
            id = PackageCatalogId("catalog-1")
        )

        repository.save(catalog)

        assertEquals(
            catalog,
            repository.findById(catalog.id)
        )
    }

    @Test
    fun `save replaces catalog with same ID`() {
        val id = PackageCatalogId("catalog-1")

        val original = PackageCatalog(id = id)

        val updated = PackageCatalog(
            id = id,
            packageIds = setOf(PackageId("package-1"))
        )

        repository.save(original)
        repository.save(updated)

        assertEquals(
            updated,
            repository.findById(id)
        )
    }

    @Test
    fun `findAll returns catalogs in insertion order`() {
        val first = PackageCatalog(
            id = PackageCatalogId("catalog-1")
        )

        val second = PackageCatalog(
            id = PackageCatalogId("catalog-2")
        )

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }
}
