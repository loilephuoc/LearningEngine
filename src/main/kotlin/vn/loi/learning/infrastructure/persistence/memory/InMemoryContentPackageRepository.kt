package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageId

class InMemoryContentPackageRepository : ContentPackageRepository {

    private val packages =
        linkedMapOf<PackageId, ContentPackage>()

    override fun findById(
        packageId: PackageId
    ): ContentPackage? =
        packages[packageId]

    override fun save(
        contentPackage: ContentPackage
    ) {
        packages[contentPackage.id] = contentPackage
    }

    override fun deleteById(
        packageId: PackageId
    ) {
        packages.remove(packageId)
    }

    override fun findAll(): List<ContentPackage> =
        packages.values.toList()

    fun count(): Int =
        packages.size

    fun clear() {
        packages.clear()
    }
}

