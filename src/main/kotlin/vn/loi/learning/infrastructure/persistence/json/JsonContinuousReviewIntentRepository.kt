package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.application.continuousreview.ContinuousReviewIntent
import vn.loi.learning.application.port.ContinuousReviewIntentRepository
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.persistence.record.ContinuousReviewIntentRecord

class JsonContinuousReviewIntentRepository(
    private val filePath: Path,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true },
    private val coordinateMutation: ((() -> Unit) -> Unit) = { it() }
) : ContinuousReviewIntentRepository {
    override fun findByLearner(learnerId: LearnerId): ContinuousReviewIntent? =
        loadAll().firstOrNull { it.learnerId == learnerId.value }?.toDomain()

    override fun save(intent: ContinuousReviewIntent) {
        coordinateMutation {
            val current = loadAll()
            val record = intent.toRecord()
            JsonFileWriter.write(
                filePath,
                JsonPersistenceCodec.encode(current.filterNot { it.learnerId == record.learnerId } + record) {
                    json.encodeToString(it)
                }
            )
        }
    }

    private fun loadAll(): List<ContinuousReviewIntentRecord> =
        JsonFileReader.read(filePath, emptyList(), "continuous review intent") { content ->
            JsonPersistenceCodec.decode(
                filePath,
                content,
                { json.decodeFromString<List<ContinuousReviewIntentRecord>>(it) },
                { json.decodeFromString<JsonPersistenceEnvelope<List<ContinuousReviewIntentRecord>>>(it) }
            )
        }

    private fun ContinuousReviewIntent.toRecord() = ContinuousReviewIntentRecord(
        learnerId.value, installedPackageId.value, topicId?.value, enabled, updatedAt.epochMillis,
        lastNoWorkPredecessorId?.value
    )

    private fun ContinuousReviewIntentRecord.toDomain() = ContinuousReviewIntent(
        LearnerId(learnerId), InstalledPackageId(installedPackageId), topicId?.let(::TopicId),
        enabled, Moment(updatedAtEpochMillis), lastNoWorkPredecessorId?.let(::SessionId)
    )
}
