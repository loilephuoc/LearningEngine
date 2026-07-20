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
    val customFields: Map<String, String> = emptyMap()
)



