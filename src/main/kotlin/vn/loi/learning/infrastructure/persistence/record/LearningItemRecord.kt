package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

@Serializable
data class LearningItemRecord(
    val id: String,
    val contentId: String,
    val mode: String,
    val isEnabled: Boolean
 )
