package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

@Serializable
data class ContentRecord(
    val id: String,
    val type: String,
    val primaryText: String,
    val translatedText: String?,
    val pronunciation: String?,
    val exampleText: String?,
    val exampleTranslation: String?,
    val group: String? = null,
    val section: String? = null,
    val lesson: String? = null,
    val customFields: Map<String, String> = emptyMap(),
    val primaryTextFormat: String = "PLAIN_TEXT",
    val translatedTextFormat: String = "PLAIN_TEXT",
    val exampleTextFormat: String = "PLAIN_TEXT",
    val exampleTranslationFormat: String = "PLAIN_TEXT",
    val primaryAudio: String? = null,
    val translatedAudio: String? = null,
    val image: String? = null,
    val exampleAudio: String? = null,
    val exampleTranslatedAudio: String? = null,
    val title: String? = null,
    val tags: Set<String> = emptySet(),
    val source: String? = null
)



