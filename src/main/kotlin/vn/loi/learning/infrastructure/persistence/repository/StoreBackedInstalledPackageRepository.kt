package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.mapper.InstalledPackageRecordMapper
import vn.loi.learning.infrastructure.persistence.record.InstalledPackageRecord
import vn.loi.learning.infrastructure.persistence.store.InstalledPackageStore

/**
 * Persistent JSON-backed implementation of [InstalledPackageRepository].
 *
 * Follows the load-all / merge / save-all pattern used by other
 * StoreBackedXxxRepository implementations in this codebase.
 */
class StoreBackedInstalledPackageRepository(
    private val store: InstalledPackageStore
) : InstalledPackageRepository {

    override fun findById(id: InstalledPackageId): InstalledPackage? =
        store.loadAll()
            .firstOrNull { it.id == id.value }
            ?.let(InstalledPackageRecordMapper::toDomain)

    override fun findByPackageId(packageId: PackageId): InstalledPackage? =
        store.loadAll()
            .filter { it.state != PackageState.REMOVED.name }
            .firstOrNull { it.packageId == packageId.value }
            ?.let(InstalledPackageRecordMapper::toDomain)

    override fun findByTopicId(topicId: TopicId): InstalledPackage? =
        store.loadAll()
            .filter { it.state != PackageState.REMOVED.name }
            .firstOrNull { it.topicId == topicId.value }
            ?.let(InstalledPackageRecordMapper::toDomain)

    override fun findAllByState(state: PackageState): List<InstalledPackage> =
        store.loadAll()
            .filter { it.state == state.name }
            .map(InstalledPackageRecordMapper::toDomain)

    override fun findAll(): List<InstalledPackage> =
        store.loadAll()
            .map(InstalledPackageRecordMapper::toDomain)

    override fun findAllByLibraryId(libraryId: LibraryId): List<InstalledPackage> =
        store.loadAll()
            .filter { it.libraryId == libraryId.value }
            .map(InstalledPackageRecordMapper::toDomain)

    override fun findAllByLibraryIdAndState(
        libraryId: LibraryId,
        state: PackageState
    ): List<InstalledPackage> =
        store.loadAll()
            .filter { it.libraryId == libraryId.value && it.state == state.name }
            .map(InstalledPackageRecordMapper::toDomain)

    override fun save(installedPackage: InstalledPackage) {
        val existingRecords = store.loadAll()
        val recordsById = linkedMapOf<String, InstalledPackageRecord>()
        existingRecords.forEach { record -> recordsById[record.id] = record }

        val newRecord = InstalledPackageRecordMapper.toRecord(installedPackage)
        recordsById[newRecord.id] = newRecord

        val updatedRecords = recordsById.values.toList()
        if (updatedRecords == existingRecords) return
        store.saveAll(updatedRecords)
    }

    override fun delete(id: InstalledPackageId) {
        val existingRecords = store.loadAll()
        val updatedRecords = existingRecords.filterNot { it.id == id.value }
        if (updatedRecords.size == existingRecords.size) return
        store.saveAll(updatedRecords)
    }
}
