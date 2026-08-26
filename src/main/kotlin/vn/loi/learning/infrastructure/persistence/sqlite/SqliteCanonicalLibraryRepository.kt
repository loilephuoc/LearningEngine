package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.repository.LibraryRepository
import vn.loi.learning.infrastructure.persistence.mapper.CanonicalLibraryRecordMapper
import vn.loi.learning.infrastructure.persistence.record.CanonicalLibraryRecord
import vn.loi.learning.infrastructure.persistence.record.LibraryEntryRecord
import vn.loi.learning.infrastructure.persistence.store.CanonicalLibraryStore

fun Canonical_library.toRecord(): CanonicalLibraryRecord = CanonicalLibraryRecord(
    id = id,
    name = name,
    entries = SqliteJsonUtils.decodeOrDefault(entriesJson, emptyList<LibraryEntryRecord>()),
    activePackageId = activePackageId,
    createdAt = createdAt
)

fun Canonical_library.toDomain(): Library =
    CanonicalLibraryRecordMapper.toDomain(toRecord())

class SqliteCanonicalLibraryRepository(
    private val database: LearningEngineDatabase
) : LibraryRepository, CanonicalLibraryStore {

    private val queries = database.libraryQueries

    override fun findById(id: LibraryId): Library? {
        return queries.selectCanonicalLibraryById(id.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun save(library: Library) {
        val record = CanonicalLibraryRecordMapper.toRecord(library)
        insertOrReplaceRecord(record)
    }

    override fun existsById(id: LibraryId): Boolean {
        return queries.selectCanonicalLibraryById(id.value).executeAsOneOrNull() != null
    }

    override fun loadAll(): List<CanonicalLibraryRecord> {
        return queries.selectAllCanonicalLibraries()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<CanonicalLibraryRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: CanonicalLibraryRecord) {
        queries.insertOrReplaceCanonicalLibrary(
            id = record.id,
            name = record.name,
            entriesJson = SqliteJsonUtils.encode(record.entries),
            activePackageId = record.activePackageId,
            createdAt = record.createdAt
        )
    }
}
