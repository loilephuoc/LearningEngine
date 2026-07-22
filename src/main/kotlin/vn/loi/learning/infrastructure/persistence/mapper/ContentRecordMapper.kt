package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentTextFormat
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.infrastructure.persistence.record.ContentRecord

object ContentRecordMapper {

    fun toRecord(
        content: Content
    ): ContentRecord =
        ContentRecord(
            id = content.id.toString(),
            type = content.type.name,
            primaryText = content.text.primaryText,
            translatedText = content.text.translatedText,
            pronunciation = content.text.pronunciation,
            exampleText = content.text.exampleText,
            exampleTranslation = content.text.exampleTranslation,
            group = content.metadata.group,
            section = content.metadata.section,
            lesson = content.metadata.lesson,
            primaryTextFormat = content.text.primaryFormat.name,
            translatedTextFormat = content.text.translatedFormat.name,
            exampleTextFormat = content.text.exampleFormat.name,
            exampleTranslationFormat = content.text.exampleTranslationFormat.name,
            primaryAudio = content.media.primaryAudio,
            translatedAudio = content.media.translatedAudio,
            image = content.media.image,
            exampleAudio = content.media.exampleAudio,
            exampleTranslatedAudio = content.media.exampleTranslatedAudio,
            title = content.metadata.title,
            tags = content.metadata.tags,
            source = content.metadata.source,
            customFields = content.customFields.fields.associate { field ->
                field.id.value to field.value
            }
        )

    fun toDomain(
        record: ContentRecord
    ): Content =
        mapPersistedRecord(
            recordType = "content",
            recordId = record.id
        ) {
            Content(
                id = ContentId(record.id),
                type = ContentType.valueOf(record.type),
                text = ContentText(
                    primaryText = record.primaryText,
                    translatedText = record.translatedText,
                    pronunciation = record.pronunciation,
                    exampleText = record.exampleText,
                    exampleTranslation = record.exampleTranslation,
                    primaryFormat = ContentTextFormat.valueOf(record.primaryTextFormat),
                    translatedFormat = ContentTextFormat.valueOf(record.translatedTextFormat),
                    exampleFormat = ContentTextFormat.valueOf(record.exampleTextFormat),
                    exampleTranslationFormat = ContentTextFormat.valueOf(record.exampleTranslationFormat)
                ),
                media = ContentMedia(
                    primaryAudio = record.primaryAudio,
                    translatedAudio = record.translatedAudio,
                    image = record.image,
                    exampleAudio = record.exampleAudio,
                    exampleTranslatedAudio = record.exampleTranslatedAudio
                ),
                metadata = ContentMetadata(
                    title = record.title,
                    group = record.group,
                    section = record.section,
                    lesson = record.lesson,
                    tags = record.tags,
                    source = record.source
                ),
                customFields = ContentCustomFields(
                    fields = record.customFields.map { (id, value) ->
                        ContentCustomField(
                            id = ContentFieldId(id),
                            value = value
                        )
                    }.toSet()
                )
            )
        }
}


