package vn.loi.learning.application.contentpackaging

import kotlinx.serialization.Serializable
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentTextFormat
import vn.loi.learning.domain.content.model.ContentType

/**
 * DTO JSON của một Content trong package export.
 */
@Serializable
data class PackageExportContentJson(
    val id: String,
    val type: String,
    val primaryText: String,
    val translatedText: String? = null,
    val pronunciation: String? = null,
    val exampleText: String? = null,
    val exampleTranslation: String? = null,
    val primaryAudio: String? = null,
    val translatedAudio: String? = null,
    val image: String? = null,
    val exampleAudio: String? = null,
    val exampleTranslatedAudio: String? = null,
    val title: String? = null,
    val group: String? = null,
    val section: String? = null,
    val lesson: String? = null,
    val tags: Set<String> = emptySet(),
    val source: String? = null,
    val customFields: Map<String, String> = emptyMap(),
    val primaryTextFormat: String = ContentTextFormat.PLAIN_TEXT.name,
    val translatedTextFormat: String = ContentTextFormat.PLAIN_TEXT.name,
    val exampleTextFormat: String = ContentTextFormat.PLAIN_TEXT.name,
    val exampleTranslationFormat: String = ContentTextFormat.PLAIN_TEXT.name
) {

    companion object {
        fun from(content: Content): PackageExportContentJson =
            PackageExportContentJson(
                id = content.id.value,
                type = content.type.name,
                primaryText = content.text.primaryText,
                translatedText = content.text.translatedText,
                pronunciation = content.text.pronunciation,
                exampleText = content.text.exampleText,
                exampleTranslation = content.text.exampleTranslation,
                primaryAudio = content.media.primaryAudio,
                translatedAudio = content.media.translatedAudio,
                image = content.media.image,
                exampleAudio = content.media.exampleAudio,
                exampleTranslatedAudio = content.media.exampleTranslatedAudio,
                title = content.metadata.title,
                group = content.metadata.group,
                section = content.metadata.section,
                lesson = content.metadata.lesson,
                tags = content.metadata.tags,
                source = content.metadata.source,
                customFields = content.customFields.fields.associate { field -> field.id.value to field.value },
                primaryTextFormat = content.text.primaryFormat.name,
                translatedTextFormat = content.text.translatedFormat.name,
                exampleTextFormat = content.text.exampleFormat.name,
                exampleTranslationFormat = content.text.exampleTranslationFormat.name
            )
    }

    fun toDomain(): Content =
        Content(
            id = ContentId(id),
            type = ContentType.valueOf(type),
            text = ContentText(
                primaryText = primaryText,
                translatedText = translatedText,
                pronunciation = pronunciation,
                exampleText = exampleText,
                exampleTranslation = exampleTranslation,
                primaryFormat = ContentTextFormat.valueOf(primaryTextFormat),
                translatedFormat = ContentTextFormat.valueOf(translatedTextFormat),
                exampleFormat = ContentTextFormat.valueOf(exampleTextFormat),
                exampleTranslationFormat = ContentTextFormat.valueOf(exampleTranslationFormat)
            ),
            media = ContentMedia(primaryAudio, translatedAudio, image, exampleAudio, exampleTranslatedAudio),
            metadata = ContentMetadata(title, group, section, lesson, tags, source),
            customFields = ContentCustomFields(customFields.entries.map { ContentCustomField(ContentFieldId(it.key), it.value) }.toSet())
        )
}
