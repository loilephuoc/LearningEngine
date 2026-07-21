package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
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
                    exampleTranslation = record.exampleTranslation
                ),
                metadata = ContentMetadata(
                    group = record.group,
                    section = record.section,
                    lesson = record.lesson
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



