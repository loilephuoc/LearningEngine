package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

class InMemoryPackageCatalogRepository : PackageCatalogRepository {

    private val catalogs =
        linkedMapOf<PackageCatalogId, PackageCatalog>()

    override fun findById(
        catalogId: PackageCatalogId
    ): PackageCatalog? =
        catalogs[catalogId]

    override fun save(
        catalog: PackageCatalog
    ) {
        catalogs[catalog.id] = catalog
    }

    override fun deleteById(
        catalogId: PackageCatalogId
    ) {
        catalogs.remove(catalogId)
    }

    override fun findAll(): List<PackageCatalog> =
        catalogs.values.toList()

    fun count(): Int =
        catalogs.size

    fun clear() {
        catalogs.clear()
    }
}

