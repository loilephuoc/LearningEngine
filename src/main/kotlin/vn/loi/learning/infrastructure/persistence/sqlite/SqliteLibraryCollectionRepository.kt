package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.LibraryCollectionRepository
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection as DomainLibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.infrastructure.persistence.mapper.LibraryCollectionRecordMapper
import vn.loi.learning.infrastructure.persistence.record.LibraryCollectionRecord
import vn.loi.learning.infrastructure.persistence.store.LibraryCollectionStore

fun Library_collection.toRecord(): LibraryCollectionRecord = LibraryCollectionRecord(
    id = id,
    libraryId = libraryId,
    name = name,
    packageIds = SqliteJsonUtils.decodeOrDefault(packageIdsJson, emptySet())
)

fun Library_collection.toDomain(): DomainLibraryCollection =
    LibraryCollectionRecordMapper.toDomain(toRecord())

class SqliteLibraryCollectionStore(
    private val database: LearningEngineDatabase
) : LibraryCollectionStore {

    private val queries = database.libraryQueries

    override fun loadAll(): List<LibraryCollectionRecord> {
        return queries.selectAllLibraryCollections()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<LibraryCollectionRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: LibraryCollectionRecord) {
        queries.insertOrReplaceLibraryCollection(
            id = record.id,
            libraryId = record.libraryId,
            name = record.name,
            packageIdsJson = SqliteJsonUtils.encode(record.packageIds)
        )
    }
}

class SqliteLibraryCollectionRepository(
    private val database: LearningEngineDatabase
) : LibraryCollectionRepository {

    private val queries = database.libraryQueries
    val store: LibraryCollectionStore = SqliteLibraryCollectionStore(database)

    override fun findById(collectionId: LibraryCollectionId): DomainLibraryCollection? {
        return queries.selectLibraryCollectionById(collectionId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findAllByLibraryId(libraryId: ContentLibraryId): List<DomainLibraryCollection> {
        return queries.selectAllLibraryCollectionsByLibraryId(libraryId.value)
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findAll(): List<DomainLibraryCollection> {
        return queries.selectAllLibraryCollections()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun save(collection: DomainLibraryCollection) {
        val record = LibraryCollectionRecordMapper.toRecord(collection)
        insertOrReplaceRecord(record)
    }

    override fun saveAll(collections: List<DomainLibraryCollection>) {
        if (collections.isEmpty()) return
        database.transaction {
            for (col in collections) {
                save(col)
            }
        }
    }

    override fun deleteById(collectionId: LibraryCollectionId) {
        queries.deleteLibraryCollectionById(collectionId.value)
    }

    override fun deleteAllById(collectionIds: Set<LibraryCollectionId>) {
        if (collectionIds.isEmpty()) return
        queries.deleteLibraryCollectionsByIds(collectionIds.map { it.value })
    }

    private fun insertOrReplaceRecord(record: LibraryCollectionRecord) {
        queries.insertOrReplaceLibraryCollection(
            id = record.id,
            libraryId = record.libraryId,
            name = record.name,
            packageIdsJson = SqliteJsonUtils.encode(record.packageIds)
        )
    }
}
