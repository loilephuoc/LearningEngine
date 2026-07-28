package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
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
                currentIndex = 2,
                itemOrigins = mapOf(
                    LearningItemId("item-1") to SessionItemOrigin.NEW,
                    LearningItemId("item-2") to SessionItemOrigin.REVIEW,
                    LearningItemId("item-3") to SessionItemOrigin.REVIEW
                ),
                itemContentIds = mapOf(
                    LearningItemId("item-1") to ContentId("content-1"),
                    LearningItemId("item-2") to ContentId("content-2"),
                    LearningItemId("item-3") to ContentId("content-2")
                )
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
    fun `schema one and two queues restore without invented Content identities`() {
        listOf(1, 2).forEach { schemaVersion ->
            val restored = StudyQueueRecordMapper.toDomain(
                StudyQueueRecord(
                    schemaVersion = schemaVersion,
                    sessionId = "legacy-session-$schemaVersion",
                    createdAtEpochMillis = 1_000L,
                    learningItemIds = listOf("legacy-item"),
                    currentIndex = 0,
                    itemOrigins =
                        if (schemaVersion == 2) {
                            mapOf("legacy-item" to SessionItemOrigin.REVIEW.name)
                        } else {
                            emptyMap()
                        }
                )
            )
            assertEquals(emptyMap(), restored.itemContentIds)
            assertEquals(null, restored.currentContentId)
        }
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
