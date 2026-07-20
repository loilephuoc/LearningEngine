package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.mapper.PackageRecordMapper
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

        val updatedRecords =
            store.loadAll()
                .filterNot { record ->
                    record.id == newRecord.id
                } + newRecord

        store.saveAll(updatedRecords)
    }

    override fun deleteById(
        packageId: PackageId
    ) {
        val updatedRecords =
            store.loadAll()
                .filterNot { record ->
                    record.id == packageId.toString()
                }

        store.saveAll(updatedRecords)
    }

    override fun findAll(): List<ContentPackage> =
        store.loadAll()
            .map(
                PackageRecordMapper::toDomain
            )
}

