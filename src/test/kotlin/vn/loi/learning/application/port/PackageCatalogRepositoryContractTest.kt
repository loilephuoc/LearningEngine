package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository

class PackageCatalogRepositoryContractTest {

    private fun createRepository(): PackageCatalogRepository =
        InMemoryPackageCatalogRepository()

    private fun createCatalog(
        id: String,
        packageIds: Set<PackageId> = emptySet()
    ): PackageCatalog =
        PackageCatalog(
            id = PackageCatalogId(id),
            packageIds = packageIds
        )

    @Test
    fun `findById returns null when catalog does not exist`() {
        val repository =
            createRepository()

        assertNull(
            repository.findById(
                PackageCatalogId("missing-catalog")
            )
        )
    }

    @Test
    fun `save stores catalog and findById returns it`() {
        val repository =
            createRepository()

        val catalog =
            createCatalog(
                id = "catalog-1"
            )

        repository.save(
            catalog
        )

        assertEquals(
            catalog,
            repository.findById(
                catalog.id
            )
        )
    }

    @Test
    fun `save replaces catalog with the same ID`() {
        val repository =
            createRepository()

        val original =
            createCatalog(
                id = "catalog-1"
            )

        val updated =
            createCatalog(
                id = "catalog-1",
                packageIds = setOf(
                    PackageId("package-1")
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
    fun `findAll returns catalogs in insertion order`() {
        val repository =
            createRepository()

        val first =
            createCatalog(
                id = "catalog-1"
            )

        val second =
            createCatalog(
                id = "catalog-2"
            )

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first, second),
            repository.findAll()
        )
    }

    @Test
    fun `saved catalog is not affected by changes to source mutable set`() {
        val repository =
            createRepository()

        val sourcePackageIds =
            mutableSetOf(
                PackageId("package-1")
            )

        val catalog =
            createCatalog(
                id = "catalog-1",
                packageIds = sourcePackageIds.toSet()
            )

        repository.save(catalog)

        sourcePackageIds.add(
            PackageId("package-2")
        )

        assertEquals(
            setOf(
                PackageId("package-1")
            ),
            repository.findById(
                catalog.id
            )?.packageIds
        )
    }
}
