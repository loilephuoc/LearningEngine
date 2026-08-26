package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.repository.CollectionRepository
import vn.loi.learning.infrastructure.persistence.mapper.CanonicalCollectionRecordMapper
import vn.loi.learning.infrastructure.persistence.record.CanonicalCollectionRecord
import vn.loi.learning.infrastructure.persistence.store.CanonicalCollectionStore

fun Canonical_collection.toRecord(): CanonicalCollectionRecord = CanonicalCollectionRecord(
    id = id,
    libraryId = libraryId,
    name = name,
    description = description,
    assignedPackageIds = SqliteJsonUtils.decodeOrDefault(assignedPackageIdsJson, emptyList()),
    state = state,
    createdAt = createdAt
)

fun Canonical_collection.toDomain(): Collection =
    CanonicalCollectionRecordMapper.toDomain(toRecord())

class SqliteCanonicalCollectionRepository(
    private val database: LearningEngineDatabase
) : CollectionRepository, CanonicalCollectionStore {

    private val queries = database.libraryQueries

    override fun findById(id: CollectionId): Collection? {
        return queries.selectCanonicalCollectionById(id.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findByName(libraryId: LibraryId, name: CollectionName): Collection? {
        return queries.selectAllCanonicalCollectionsByLibraryId(libraryId.value)
            .executeAsList()
            .firstOrNull { it.name == name.trimmedValue }
            ?.toDomain()
    }

    override fun findAllByLibraryId(libraryId: LibraryId): List<Collection> {
        return queries.selectAllCanonicalCollectionsByLibraryId(libraryId.value)
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findAllByLibraryIdAndState(
        libraryId: LibraryId,
        state: CollectionState
    ): List<Collection> {
        return queries.selectAllCanonicalCollectionsByLibraryId(libraryId.value)
            .executeAsList()
            .filter { it.state == state.name }
            .map { it.toDomain() }
    }

    override fun save(collection: Collection) {
        val record = CanonicalCollectionRecordMapper.toRecord(collection)
        insertOrReplaceRecord(record)
    }

    override fun delete(id: CollectionId) {
        queries.deleteCanonicalCollectionById(id.value)
    }

    override fun existsByName(libraryId: LibraryId, name: CollectionName): Boolean {
        return queries.selectAllCanonicalCollectionsByLibraryId(libraryId.value)
            .executeAsList()
            .any { it.name == name.trimmedValue && it.state == CollectionState.ACTIVE.name }
    }

    override fun loadAll(): List<CanonicalCollectionRecord> {
        return queries.selectAllCanonicalCollections()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<CanonicalCollectionRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: CanonicalCollectionRecord) {
        queries.insertOrReplaceCanonicalCollection(
            id = record.id,
            libraryId = record.libraryId,
            name = record.name,
            description = record.description,
            assignedPackageIdsJson = SqliteJsonUtils.encode(record.assignedPackageIds),
            state = record.state,
            createdAt = record.createdAt
        )
    }
}
