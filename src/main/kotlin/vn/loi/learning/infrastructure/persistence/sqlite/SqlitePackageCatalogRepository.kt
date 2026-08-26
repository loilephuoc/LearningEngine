package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.domain.content.packaging.model.PackageCatalog as DomainPackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.persistence.mapper.PackageCatalogRecordMapper
import vn.loi.learning.infrastructure.persistence.record.PackageCatalogRecord
import vn.loi.learning.infrastructure.persistence.store.PackageCatalogStore

fun Package_catalog.toRecord(): PackageCatalogRecord = PackageCatalogRecord(
    id = id,
    packageIds = SqliteJsonUtils.decodeOrDefault(packageIdsJson, emptyList())
)

fun Package_catalog.toDomain(): DomainPackageCatalog =
    PackageCatalogRecordMapper.toDomain(toRecord())

class SqlitePackageCatalogRepository(
    private val database: LearningEngineDatabase
) : PackageCatalogRepository, PackageCatalogStore {

    private val queries = database.packageQueries

    override fun findById(catalogId: PackageCatalogId): DomainPackageCatalog? {
        return queries.selectPackageCatalogById(catalogId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findAll(): List<DomainPackageCatalog> {
        return queries.selectAllPackageCatalogs()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun save(catalog: DomainPackageCatalog) {
        val record = PackageCatalogRecordMapper.toRecord(catalog)
        insertOrReplaceRecord(record)
    }

    override fun deleteById(catalogId: PackageCatalogId) {
        queries.deletePackageCatalogById(catalogId.value)
    }

    override fun loadAll(): List<PackageCatalogRecord> {
        return queries.selectAllPackageCatalogs()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<PackageCatalogRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: PackageCatalogRecord) {
        queries.insertOrReplacePackageCatalog(
            id = record.id,
            packageIdsJson = SqliteJsonUtils.encode(record.packageIds)
        )
    }
}
