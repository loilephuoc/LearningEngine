package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord

class StudySessionRecordMapperTest {

    @Test
    fun `active session survives round trip`() {
        val original =
            createActiveSession()

        val record =
            StudySessionRecordMapper.toRecord(original)

        val restored =
            StudySessionRecordMapper.toDomain(record)

        assertEquals(
            expected = original,
            actual = restored
        )

        assertEquals(
            expected = SessionStatus.ACTIVE,
            actual = restored.status
        )

        assertNull(restored.finishedAt)
    }

    @Test
    fun `finished session survives round trip`() {
        val original =
            createActiveSession(
                startedAt = Moment(1_000L)
            ).finish(
                at = Moment(2_000L)
            )

        val record =
            StudySessionRecordMapper.toRecord(original)

        val restored =
            StudySessionRecordMapper.toDomain(record)

        assertEquals(
            expected = original,
            actual = restored
        )

        assertEquals(
            expected = SessionStatus.FINISHED,
            actual = restored.status
        )

        assertEquals(
            expected = Moment(2_000L),
            actual = restored.finishedAt
        )
    }

    @Test
    fun `reviewed ids and counters survive round trip`() {
        val initial =
            createActiveSession()

        val afterNewReview =
            initial.recordReview(
                learningItemId =
                    LearningItemId("item-1"),
                contentId =
                    ContentId("content-1"),
                wasNewItem =
                    true
            )

        val original =
            afterNewReview.recordReview(
                learningItemId =
                    LearningItemId("item-2"),
                contentId =
                    ContentId("content-2"),
                wasNewItem =
                    false
            )

        val record =
            StudySessionRecordMapper.toRecord(original)

        val restored =
            StudySessionRecordMapper.toDomain(record)

        assertEquals(
            expected = setOf(
                LearningItemId("item-1"),
                LearningItemId("item-2")
            ),
            actual = restored.reviewedItemIds
        )

        assertEquals(
            expected = setOf(
                ContentId("content-1"),
                ContentId("content-2")
            ),
            actual = restored.reviewedContentIds
        )

        assertEquals(
            expected = 1,
            actual = restored.newItemsReviewed
        )

        assertEquals(
            expected = 1,
            actual = restored.reviewItemsReviewed
        )

        assertEquals(
            expected = original,
            actual = restored
        )
    }

    @Test
    fun `toRecord writes current schema version`() {
        val session =
            createActiveSession()

        val record =
            StudySessionRecordMapper.toRecord(session)

        assertEquals(
            expected =
                StudySessionRecord.CURRENT_SCHEMA_VERSION,
            actual =
                record.schemaVersion
        )
    }

    @Test
    fun `unsupported schema version throws IllegalArgumentException`() {
        val validRecord =
            StudySessionRecordMapper.toRecord(
                createActiveSession()
            )

        val invalidRecord =
            validRecord.copy(
                schemaVersion =
                    StudySessionRecord.CURRENT_SCHEMA_VERSION + 1
            )

        assertFailsWith<IllegalArgumentException> {
            StudySessionRecordMapper.toDomain(
                invalidRecord
            )
        }
    }

    @Test
    fun `unknown session status throws IllegalArgumentException`() {
        val validRecord =
            StudySessionRecordMapper.toRecord(
                createActiveSession()
            )

        val invalidRecord =
            validRecord.copy(
                status = "UNKNOWN_STATUS"
            )

        assertFailsWith<IllegalArgumentException> {
            StudySessionRecordMapper.toDomain(
                invalidRecord
            )
        }
    }

    @Test
    fun `invalid session invariant throws IllegalArgumentException`() {
        val validRecord =
            StudySessionRecordMapper.toRecord(
                createActiveSession()
            )

        val invalidRecord =
            validRecord.copy(
                status =
                    SessionStatus.ACTIVE.name,
                finishedAtEpochMillis =
                    2_000L
            )

        assertFailsWith<IllegalArgumentException> {
            StudySessionRecordMapper.toDomain(
                invalidRecord
            )
        }
    }

    private fun createActiveSession(
        sessionId: SessionId =
            SessionId("session-1"),
        learnerId: LearnerId =
            LearnerId("learner-1"),
        startedAt: Moment =
            Moment(1_000L)
    ): StudySession =
        StudySession.start(
            id = sessionId,
            learnerId = learnerId,
            startedAt = startedAt,
            policy = SessionPolicy(
                newItemLimit = 10,
                reviewItemLimit = 10,
                allowRepeatInSameSession = false
            )
        )
}