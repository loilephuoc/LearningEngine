package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.LearningTrajectoryRecord
import vn.loi.learning.infrastructure.persistence.store.LearningTrajectoryStore

class JsonLearningTrajectoryStore(private val filePath: Path) : LearningTrajectoryStore {
    private val json = Json { prettyPrint = true; prettyPrintIndent = "  "; ignoreUnknownKeys = true; encodeDefaults = true }
    override fun loadAll(): List<LearningTrajectoryRecord> = JsonFileReader.read(
        filePath, emptyList(), "learning trajectory"
    ) { content -> JsonPersistenceCodec.decode(filePath, content,
        decodeLegacy = { json.decodeFromString<List<LearningTrajectoryRecord>>(it) },
        decodeEnvelope = { json.decodeFromString<JsonPersistenceEnvelope<List<LearningTrajectoryRecord>>>(it) }) }
    override fun saveAll(records: List<LearningTrajectoryRecord>) = JsonFileWriter.write(
        filePath, JsonPersistenceCodec.encode(records) { json.encodeToString(it) })
}
