package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningEngineFactory

class StudyQueuePlanningPolicyIntegrationTest {

    @Test
    fun `session queue does not exceed new item limit`() {
        val engine =
            LearningEngineFactory
                .createInMemory()

        repeat(5) { index ->
            registerItem(
                engine = engine,
                number = index + 1
            )
        }

        val sessionId =
            SessionId("session-1")

        engine.startSession(
            StartStudySessionCommand(
                sessionId =
                    sessionId,
                learnerId =
                    LearnerId("learner-1"),
                startedAt =
                    Moment(1_000L),
                policy =
                    SessionPolicy(
                        newItemLimit = 2,
                        reviewItemLimit = 0
                    )
            )
        )

        val queue =
            requireNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 2,
            actual =
                queue.learningItemIds.size
        )
    }

    @Test
    fun `zero new item limit creates no new queue entries`() {
        val engine =
            LearningEngineFactory
                .createInMemory()

        repeat(3) { index ->
            registerItem(
                engine = engine,
                number = index + 1
            )
        }

        val sessionId =
            SessionId("session-1")

        engine.startSession(
            StartStudySessionCommand(
                sessionId =
                    sessionId,
                learnerId =
                    LearnerId("learner-1"),
                startedAt =
                    Moment(1_000L),
                policy =
                    SessionPolicy(
                        newItemLimit = 0,
                        reviewItemLimit = 5
                    )
            )
        )

        val queue =
            requireNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 0,
            actual =
                queue.learningItemIds.size
        )
    }

    private fun registerItem(
        engine:
        vn.loi.learning.application
        .LearningEngine,
        number: Int
    ) {
        val content =
            Content(
                id =
                    ContentId(
                        "content-$number"
                    ),
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Sentence $number"
                    )
            )

        engine.registerContent(
            content
        )

        engine.registerLearningItem(
            LearningItem(
                id =
                    LearningItemId(
                        "item-$number"
                    ),
                contentId =
                    content.id,
                mode =
                    LearningMode
                        .MEANING_RECOGNITION
            )
        )
    }
}