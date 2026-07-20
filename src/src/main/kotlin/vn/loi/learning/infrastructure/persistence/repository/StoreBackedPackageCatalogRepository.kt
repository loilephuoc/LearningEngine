package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.persistence.mapper.PackageCatalogRecordMapper
import vn.loi.learning.infrastructure.persistence.store.PackageCatalogStore

class StoreBackedPackageCatalogRepository(
    private val store: PackageCatalogStore
 ) : PackageCatalogRepository {

    override fun findById(
        catalogId: PackageCatalogId
    ): PackageCatalog? =
        store.loadAll()
            .firstOrNull { record ->
                record.id == catalogId.toString()
            }
            ?.let(
                PackageCatalogRecordMapper::toDomain
            )

    override fun save(
        catalog: PackageCatalog
    ) {
        val newRecord =
            PackageCatalogRecordMapper.toRecord(catalog)

        val updatedRecords =
            store.loadAll()
                .filterNot { record ->
                    record.id == newRecord.id
                } + newRecord

        store.saveAll(updatedRecords)
    }

    override fun deleteById(
        catalogId: PackageCatalogId
    ) {
        val updatedRecords =
            store.loadAll()
                .filterNot { record ->
                    record.id == catalogId.toString()
                }

        store.saveAll(updatedRecords)
    }

    override fun findAll(): List<PackageCatalog> =
        store.loadAll()
            .map(
                PackageCatalogRecordMapper::toDomain
            )
}

