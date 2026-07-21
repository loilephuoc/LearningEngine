package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord

class StudySessionRecordMapperTest {

    @Test
    fun `maps active session to record and back`() {
        val session =
            StudySession
                .start(
                    id =
                        SessionId(
                            "session-1"
                        ),
                    learnerId =
                        LearnerId(
                            "learner-1"
                        ),
                    startedAt =
                        Moment(1_000L),
                    policy =
                        SessionPolicy(
                            newItemLimit = 4,
                            reviewItemLimit = 8,
                            allowRepeatInSameSession =
                                false
                        ),
                    includedContentIds =
                        setOf(
                            ContentId(
                                "content-2"
                            ),
                            ContentId(
                                "content-1"
                            )
                        )
                )
                .recordReview(
                    learningItemId =
                        LearningItemId(
                            "item-1"
                        ),
                    contentId =
                        ContentId(
                            "content-1"
                        ),
                    wasNewItem = true
                )

        val record =
            StudySessionRecordMapper
                .toRecord(session)

        assertEquals(
            StudySessionRecord
                .CURRENT_SCHEMA_VERSION,
            record.schemaVersion
        )

        assertEquals(
            listOf(
                "content-1",
                "content-2"
            ),
            record.includedContentIds
        )

        assertEquals(
            session,
            StudySessionRecordMapper
                .toDomain(record)
        )
    }

    @Test
    fun `maps finished session to record and back`() {
        val session =
            StudySession
                .start(
                    id =
                        SessionId(
                            "session-2"
                        ),
                    learnerId =
                        LearnerId(
                            "learner-2"
                        ),
                    startedAt =
                        Moment(2_000L),
                    policy =
                        SessionPolicy(
                            newItemLimit = 3,
                            reviewItemLimit = 5,
                            allowRepeatInSameSession =
                                true
                        ),
                    includedContentIds =
                        setOf(
                            ContentId(
                                "content-3"
                            )
                        )
                )
                .recordReview(
                    learningItemId =
                        LearningItemId(
                            "item-2"
                        ),
                    contentId =
                        ContentId(
                            "content-3"
                        ),
                    wasNewItem = false
                )
                .finish(
                    Moment(3_000L)
                )

        val record =
            StudySessionRecordMapper
                .toRecord(session)

        assertEquals(
            session,
            StudySessionRecordMapper
                .toDomain(record)
        )
    }

    @Test
    fun `maps legacy record without study scope to unrestricted session`() {
        val record =
            StudySessionRecord(
                schemaVersion =
                    StudySessionRecord
                        .CURRENT_SCHEMA_VERSION,
                id =
                    "legacy-session",
                learnerId =
                    "legacy-learner",
                startedAtEpochMillis =
                    4_000L,
                status =
                    "ACTIVE",
                policyNewItemLimit =
                    10,
                policyReviewItemLimit =
                    20,
                policyAllowRepeatInSameSession =
                    false,
                reviewedItemIds =
                    emptyList(),
                reviewedContentIds =
                    emptyList(),
                newItemsReviewed =
                    0,
                reviewItemsReviewed =
                    0,
                finishedAtEpochMillis =
                    null
            )

        val session =
            StudySessionRecordMapper
                .toDomain(record)

        assertEquals(
            emptySet(),
            session.includedContentIds
        )
    }
}