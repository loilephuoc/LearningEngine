package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage as DomainInstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.mapper.InstalledPackageRecordMapper
import vn.loi.learning.infrastructure.persistence.record.InstalledPackageRecord
import vn.loi.learning.infrastructure.persistence.store.InstalledPackageStore

fun Installed_package.toRecord(): InstalledPackageRecord = InstalledPackageRecord(
    id = id,
    libraryId = libraryId,
    packageId = packageId,
    topicId = topicId,
    name = name,
    version = version,
    state = state,
    installedAt = installedAt,
    contentCount = contentCount.toInt(),
    learningItemCount = learningItemCount.toInt(),
    contentChecksum = contentChecksum
)

fun Installed_package.toDomain(): DomainInstalledPackage =
    InstalledPackageRecordMapper.toDomain(toRecord())

class SqliteInstalledPackageRepository(
    private val database: LearningEngineDatabase
) : InstalledPackageRepository, InstalledPackageStore {

    private val queries = database.packageQueries

    override fun findById(id: InstalledPackageId): DomainInstalledPackage? {
        return queries.selectInstalledPackageById(id.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findByPackageId(packageId: PackageId): DomainInstalledPackage? {
        return queries.selectInstalledPackageByPackageId(packageId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findByTopicId(topicId: TopicId): DomainInstalledPackage? {
        return queries.selectInstalledPackageByTopicId(topicId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findAllByState(state: PackageState): List<DomainInstalledPackage> {
        return queries.selectAllInstalledPackagesByState(state.name)
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findAll(): List<DomainInstalledPackage> {
        return queries.selectAllInstalledPackages()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findAllByLibraryId(libraryId: LibraryId): List<DomainInstalledPackage> {
        return queries.selectAllInstalledPackagesByLibraryId(libraryId.value)
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findAllByLibraryIdAndState(
        libraryId: LibraryId,
        state: PackageState
    ): List<DomainInstalledPackage> {
        return queries.selectAllInstalledPackagesByLibraryIdAndState(libraryId.value, state.name)
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun save(installedPackage: DomainInstalledPackage) {
        val record = InstalledPackageRecordMapper.toRecord(installedPackage)
        insertOrReplaceRecord(record)
    }

    override fun delete(id: InstalledPackageId) {
        queries.deleteInstalledPackageById(id.value)
    }

    override fun loadAll(): List<InstalledPackageRecord> {
        return queries.selectAllInstalledPackages()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<InstalledPackageRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: InstalledPackageRecord) {
        queries.insertOrReplaceInstalledPackage(
            id = record.id,
            libraryId = record.libraryId,
            packageId = record.packageId,
            topicId = record.topicId,
            name = record.name,
            version = record.version,
            state = record.state,
            installedAt = record.installedAt,
            contentCount = record.contentCount.toLong(),
            learningItemCount = record.learningItemCount.toLong(),
            contentChecksum = record.contentChecksum
        )
    }
}
