package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

class FailingPackageCatalogRepository(
    private val delegate: PackageCatalogRepository
) : PackageCatalogRepository {

    override fun findById(
        catalogId: PackageCatalogId
    ): PackageCatalog? =
        delegate.findById(catalogId)

    override fun save(
        catalog: PackageCatalog
    ) {
        throw IllegalStateException("catalog persistence failed")
    }

    override fun deleteById(
        catalogId: PackageCatalogId
    ) {
        delegate.deleteById(catalogId)
    }

    override fun findAll(): List<PackageCatalog> =
        delegate.findAll()
}

