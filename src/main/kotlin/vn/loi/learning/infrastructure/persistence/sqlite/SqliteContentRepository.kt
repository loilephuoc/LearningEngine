package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.Content as DomainContent
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.infrastructure.persistence.mapper.ContentRecordMapper
import vn.loi.learning.infrastructure.persistence.record.ContentRecord
import vn.loi.learning.infrastructure.persistence.store.ContentStore

fun Content.toRecord(): ContentRecord = ContentRecord(
    id = id,
    type = type,
    primaryText = primaryText,
    translatedText = translatedText,
    pronunciation = pronunciation,
    exampleText = exampleText,
    exampleTranslation = exampleTranslation,
    group = groupName,
    section = section,
    lesson = lesson,
    customFields = SqliteJsonUtils.decodeOrDefault(customFields, emptyMap()),
    primaryTextFormat = primaryTextFormat,
    translatedTextFormat = translatedTextFormat,
    exampleTextFormat = exampleTextFormat,
    exampleTranslationFormat = exampleTranslationFormat,
    primaryAudio = primaryAudio,
    translatedAudio = translatedAudio,
    image = image,
    exampleAudio = exampleAudio,
    exampleTranslatedAudio = exampleTranslatedAudio,
    title = title,
    tags = SqliteJsonUtils.decodeOrDefault(tags, emptySet()),
    source = source
)

fun Content.toDomain(): DomainContent =
    ContentRecordMapper.toDomain(toRecord())

class SqliteContentStore(
    private val database: LearningEngineDatabase
) : ContentStore {

    private val queries = database.contentQueries

    override fun loadAll(): List<ContentRecord> {
        return queries.selectAll().executeAsList().map { it.toRecord() }
    }

    override fun saveAll(records: List<ContentRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: ContentRecord) {
        queries.insertOrReplace(
            id = record.id,
            type = record.type,
            primaryText = record.primaryText,
            translatedText = record.translatedText,
            pronunciation = record.pronunciation,
            exampleText = record.exampleText,
            exampleTranslation = record.exampleTranslation,
            groupName = record.group,
            section = record.section,
            lesson = record.lesson,
            customFields = SqliteJsonUtils.encode(record.customFields),
            primaryTextFormat = record.primaryTextFormat,
            translatedTextFormat = record.translatedTextFormat,
            exampleTextFormat = record.exampleTextFormat,
            exampleTranslationFormat = record.exampleTranslationFormat,
            primaryAudio = record.primaryAudio,
            translatedAudio = record.translatedAudio,
            image = record.image,
            exampleAudio = record.exampleAudio,
            exampleTranslatedAudio = record.exampleTranslatedAudio,
            title = record.title,
            tags = SqliteJsonUtils.encode(record.tags),
            source = record.source
        )
    }
}

class SqliteContentRepository(
    private val database: LearningEngineDatabase
) : ContentRepository {

    private val queries = database.contentQueries
    val store: ContentStore = SqliteContentStore(database)

    override fun findById(contentId: ContentId): DomainContent? {
        return queries.selectById(contentId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findByIds(contentIds: Collection<ContentId>): List<DomainContent> {
        if (contentIds.isEmpty()) return emptyList()
        val distinctIds = contentIds.distinct()
        val rows = queries.selectByIds(distinctIds.map { it.value }).executeAsList()
        val byId = rows.associate { it.id to it.toDomain() }
        return distinctIds.mapNotNull { byId[it.value] }
    }

    override fun save(content: DomainContent) {
        val record = ContentRecordMapper.toRecord(content)
        insertOrReplaceRecord(record)
    }

    override fun saveAll(contents: List<DomainContent>) {
        if (contents.isEmpty()) return
        database.transaction {
            for (content in contents) {
                save(content)
            }
        }
    }

    override fun deleteById(contentId: ContentId) {
        queries.deleteById(contentId.value)
    }

    override fun deleteAllById(contentIds: Set<ContentId>) {
        if (contentIds.isEmpty()) return
        queries.deleteByIds(contentIds.map { it.value })
    }

    override fun findAll(): List<DomainContent> {
        return queries.selectAll().executeAsList().map { it.toDomain() }
    }

    private fun insertOrReplaceRecord(record: ContentRecord) {
        queries.insertOrReplace(
            id = record.id,
            type = record.type,
            primaryText = record.primaryText,
            translatedText = record.translatedText,
            pronunciation = record.pronunciation,
            exampleText = record.exampleText,
            exampleTranslation = record.exampleTranslation,
            groupName = record.group,
            section = record.section,
            lesson = record.lesson,
            customFields = SqliteJsonUtils.encode(record.customFields),
            primaryTextFormat = record.primaryTextFormat,
            translatedTextFormat = record.translatedTextFormat,
            exampleTextFormat = record.exampleTextFormat,
            exampleTranslationFormat = record.exampleTranslationFormat,
            primaryAudio = record.primaryAudio,
            translatedAudio = record.translatedAudio,
            image = record.image,
            exampleAudio = record.exampleAudio,
            exampleTranslatedAudio = record.exampleTranslatedAudio,
            title = record.title,
            tags = SqliteJsonUtils.encode(record.tags),
            source = record.source
        )
    }
}
