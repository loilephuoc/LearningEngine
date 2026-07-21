package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.persistence.record.StudyQueueRecord

class StudyQueueRecordMapperTest {

    @Test
    fun `round trip preserves queue order and current index`() {
        val snapshot =
            StudyQueueSnapshot(
                sessionId = SessionId("session-1"),
                createdAt = Moment(1_000L),
                learningItemIds =
                    listOf(
                        LearningItemId("item-1"),
                        LearningItemId("item-2"),
                        LearningItemId("item-3")
                    ),
                currentIndex = 2
            )

        val restored =
            StudyQueueRecordMapper.toDomain(
                StudyQueueRecordMapper.toRecord(snapshot)
            )

        assertEquals(
            expected = snapshot,
            actual = restored
        )
    }

    @Test
    fun `toDomain rejects unsupported schema version`() {
        val record =
            StudyQueueRecord(
                schemaVersion = 999,
                sessionId = "session-1",
                createdAtEpochMillis = 1_000L,
                learningItemIds = listOf("item-1"),
                currentIndex = 0
            )

        assertFailsWith<IllegalArgumentException> {
            StudyQueueRecordMapper.toDomain(record)
        }
    }
}