package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.persistence.mapper.PackageCatalogRecordMapper
import vn.loi.learning.infrastructure.persistence.record.PackageCatalogRecord
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
            PackageCatalogRecordMapper.toRecord(
                catalog
            )

        val existingRecords =
            store.loadAll()

        val existingRecord =
            existingRecords.firstOrNull { record ->
                record.id == newRecord.id
            }

        if (existingRecord == newRecord) {
            return
        }

        val recordsById =
            linkedMapOf<String, PackageCatalogRecord>()

        existingRecords.forEach { record ->
            recordsById[record.id] =
                record
        }

        recordsById[newRecord.id] =
            newRecord

        store.saveAll(
            recordsById.values.toList()
        )
    }

    override fun deleteById(
        catalogId: PackageCatalogId
    ) {
        val id =
            catalogId.toString()

        val existingRecords =
            store.loadAll()

        val updatedRecords =
            existingRecords.filterNot { record ->
                record.id == id
            }

        if (updatedRecords.size == existingRecords.size) {
            return
        }

        store.saveAll(
            updatedRecords
        )
    }

    override fun findAll(): List<PackageCatalog> =
        store.loadAll()
            .map(
                PackageCatalogRecordMapper::toDomain
            )
}