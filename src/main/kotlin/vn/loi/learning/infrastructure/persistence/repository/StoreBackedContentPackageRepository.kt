package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.mapper.PackageRecordMapper
import vn.loi.learning.infrastructure.persistence.record.PackageRecord
import vn.loi.learning.infrastructure.persistence.store.ContentPackageStore

class StoreBackedContentPackageRepository(
    private val store: ContentPackageStore
) : ContentPackageRepository {

    override fun findById(
        packageId: PackageId
    ): ContentPackage? =
        store.loadAll()
            .firstOrNull { record ->
                record.id == packageId.toString()
            }
            ?.let(
                PackageRecordMapper::toDomain
            )

    override fun save(
        contentPackage: ContentPackage
    ) {
        val newRecord =
            PackageRecordMapper.toRecord(
                contentPackage
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
            linkedMapOf<String, PackageRecord>()

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
        packageId: PackageId
    ) {
        val id =
            packageId.toString()

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

    override fun findAll(): List<ContentPackage> =
        store.loadAll()
            .map(
                PackageRecordMapper::toDomain
            )
}