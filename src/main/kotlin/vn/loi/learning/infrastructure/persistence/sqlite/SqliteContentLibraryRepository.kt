package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.domain.content.library.model.ContentLibrary as DomainContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.infrastructure.persistence.mapper.ContentLibraryRecordMapper
import vn.loi.learning.infrastructure.persistence.record.ContentLibraryRecord
import vn.loi.learning.infrastructure.persistence.store.ContentLibraryStore

fun Content_library.toRecord(): ContentLibraryRecord = ContentLibraryRecord(
    id = id,
    name = name,
    contentIds = SqliteJsonUtils.decodeOrDefault(contentIdsJson, emptySet())
)

fun Content_library.toDomain(): DomainContentLibrary =
    ContentLibraryRecordMapper.toDomain(toRecord())

class SqliteContentLibraryStore(
    private val database: LearningEngineDatabase
) : ContentLibraryStore {

    private val queries = database.libraryQueries

    override fun loadAll(): List<ContentLibraryRecord> {
        return queries.selectAllContentLibraries()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<ContentLibraryRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: ContentLibraryRecord) {
        queries.insertOrReplaceContentLibrary(
            id = record.id,
            name = record.name,
            contentIdsJson = SqliteJsonUtils.encode(record.contentIds)
        )
    }
}

class SqliteContentLibraryRepository(
    private val database: LearningEngineDatabase
) : ContentLibraryRepository {

    private val queries = database.libraryQueries
    val store: ContentLibraryStore = SqliteContentLibraryStore(database)

    override fun findById(libraryId: ContentLibraryId): DomainContentLibrary? {
        return queries.selectContentLibraryById(libraryId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findAll(): List<DomainContentLibrary> {
        return queries.selectAllContentLibraries()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun save(library: DomainContentLibrary) {
        val record = ContentLibraryRecordMapper.toRecord(library)
        insertOrReplaceRecord(record)
    }

    override fun saveAll(libraries: List<DomainContentLibrary>) {
        if (libraries.isEmpty()) return
        database.transaction {
            for (lib in libraries) {
                save(lib)
            }
        }
    }

    override fun deleteById(libraryId: ContentLibraryId) {
        queries.deleteContentLibraryById(libraryId.value)
    }

    override fun deleteAllById(libraryIds: Set<ContentLibraryId>) {
        if (libraryIds.isEmpty()) return
        queries.deleteContentLibrariesByIds(libraryIds.map { it.value })
    }

    private fun insertOrReplaceRecord(record: ContentLibraryRecord) {
        queries.insertOrReplaceContentLibrary(
            id = record.id,
            name = record.name,
            contentIdsJson = SqliteJsonUtils.encode(record.contentIds)
        )
    }
}
