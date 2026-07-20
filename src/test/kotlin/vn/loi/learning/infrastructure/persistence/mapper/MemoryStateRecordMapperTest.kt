package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord
import vn.loi.learning.testing.fixtures.MemoryFixtures

class MemoryStateRecordMapperTest {

    @Test
    fun `new memory state survives record round trip`() {
        val original = MemoryFixtures.newState(
            learnerId = LearnerId("learner-round-trip"),
            learningItemId = LearningItemId("item-round-trip"),
            availableAt = Moment(1_000L)
        )

        val record =
            MemoryStateRecordMapper.toRecord(original)

        val restored =
            MemoryStateRecordMapper.toDomain(record)

        assertEquals(
            expected = original,
            actual = restored
        )
    }

    @Test
    fun `reviewed memory state survives record round trip`() {
        val original = MemoryState(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1"),
            stage = LearningStage.REVIEW,
            difficulty = 4.25,
            stabilityDays = 12.5,
            dueAt = Moment(50_000L),
            lastReviewedAt = Moment(10_000L),
            reviewCount = 7,
            lapseCount = 2
        )

        val record =
            MemoryStateRecordMapper.toRecord(original)

        val restored =
            MemoryStateRecordMapper.toDomain(record)

        assertEquals(
            expected = original,
            actual = restored
        )
    }

    @Test
    fun `toRecord uses current schema version`() {
        val state = MemoryFixtures.newState()

        val record =
            MemoryStateRecordMapper.toRecord(state)

        assertEquals(
            expected =
                MemoryStateRecord.CURRENT_SCHEMA_VERSION,
            actual = record.schemaVersion
        )
    }

    @Test
    fun `toDomain rejects unsupported schema version`() {
        val record = MemoryStateRecord(
            schemaVersion = 999,
            learnerId = "learner-1",
            learningItemId = "item-1",
            stage = LearningStage.NEW.name,
            difficulty = 5.0,
            stabilityDays = 0.0,
            dueAtEpochMillis = 1_000L,
            lastReviewedAtEpochMillis = null,
            reviewCount = 0,
            lapseCount = 0
        )

        assertFailsWith<IllegalArgumentException> {
            MemoryStateRecordMapper.toDomain(record)
        }
    }

    @Test
    fun `toDomain rejects unknown learning stage`() {
        val record = MemoryStateRecord(
            schemaVersion =
                MemoryStateRecord.CURRENT_SCHEMA_VERSION,
            learnerId = "learner-1",
            learningItemId = "item-1",
            stage = "UNKNOWN_STAGE",
            difficulty = 5.0,
            stabilityDays = 0.0,
            dueAtEpochMillis = 1_000L,
            lastReviewedAtEpochMillis = null,
            reviewCount = 0,
            lapseCount = 0
        )

        assertFailsWith<IllegalArgumentException> {
            MemoryStateRecordMapper.toDomain(record)
        }
    }
}