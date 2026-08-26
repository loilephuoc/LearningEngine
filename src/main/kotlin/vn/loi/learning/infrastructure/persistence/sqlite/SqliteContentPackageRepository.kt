package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage as DomainContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.mapper.PackageRecordMapper
import vn.loi.learning.infrastructure.persistence.record.PackageDependencyRecord
import vn.loi.learning.infrastructure.persistence.record.PackageRecord
import vn.loi.learning.infrastructure.persistence.store.ContentPackageStore

fun Content_package.toRecord(): PackageRecord = PackageRecord(
    id = id,
    name = name,
    version = version,
    format = format,
    libraryIds = SqliteJsonUtils.decodeOrDefault(libraryIdsJson, emptySet()),
    schemaVersion = schemaVersion.toInt(),
    minimumEngineVersion = minimumEngineVersion,
    maximumEngineVersion = maximumEngineVersion,
    dependencies = SqliteJsonUtils.decodeOrDefault(dependenciesJson, emptySet<PackageDependencyRecord>()),
    topicId = topicId
)

fun Content_package.toDomain(): DomainContentPackage =
    PackageRecordMapper.toDomain(toRecord())

class SqliteContentPackageRepository(
    private val database: LearningEngineDatabase
) : ContentPackageRepository, ContentPackageStore {

    private val queries = database.packageQueries

    override fun findById(packageId: PackageId): DomainContentPackage? {
        return queries.selectContentPackageById(packageId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findAll(): List<DomainContentPackage> {
        return queries.selectAllContentPackages()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun save(contentPackage: DomainContentPackage) {
        val record = PackageRecordMapper.toRecord(contentPackage)
        insertOrReplaceRecord(record)
    }

    override fun deleteById(packageId: PackageId) {
        queries.deleteContentPackageById(packageId.value)
    }

    override fun loadAll(): List<PackageRecord> {
        return queries.selectAllContentPackages()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<PackageRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: PackageRecord) {
        queries.insertOrReplaceContentPackage(
            id = record.id,
            name = record.name,
            version = record.version,
            format = record.format,
            libraryIdsJson = SqliteJsonUtils.encode(record.libraryIds),
            schemaVersion = record.schemaVersion.toLong(),
            minimumEngineVersion = record.minimumEngineVersion,
            maximumEngineVersion = record.maximumEngineVersion,
            dependenciesJson = SqliteJsonUtils.encode(record.dependencies),
            topicId = record.topicId
        )
    }
}
