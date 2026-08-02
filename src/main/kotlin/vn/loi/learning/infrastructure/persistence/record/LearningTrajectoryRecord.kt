package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

@Serializable
data class LearningTrajectoryRecord(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val learnerId: String,
    val contentId: String,
    val chains: List<EvidenceChainRecord>
) { companion object { const val CURRENT_SCHEMA_VERSION = 1 } }

@Serializable
data class EvidenceChainRecord(
    val stage: String,
    val anchorReason: String,
    val entries: List<EvidenceEntryRecord>,
    val closedBy: String? = null
)

@Serializable
data class EvidenceEntryRecord(
    val kind: String,
    val reviewEventId: String? = null,
    val timestampMillis: Long,
    val rating: String,
    val result: String? = null,
    val sessionId: String,
    val sessionPolicy: String? = null,
    val provenance: String,
    val revealUsed: Boolean = false,
    val schedulerDueMillis: Long? = null,
    val typingLatencyMillis: Long? = null,
    val origin: String? = null,
    val commitStatus: String? = null
)
