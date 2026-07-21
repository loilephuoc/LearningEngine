package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId

class StudyQueuePlanTest {

    @Test
    fun `plan preserves session time and item order`() {
        val plan =
            StudyQueuePlan.create(
                sessionId =
                    SessionId("session-1"),
                plannedAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        LearningItemId("item-3"),
                        LearningItemId("item-1"),
                        LearningItemId("item-2")
                    )
            )

        assertEquals(
            expected =
                SessionId("session-1"),
            actual =
                plan.sessionId
        )

        assertEquals(
            expected =
                Moment(1_000L),
            actual =
                plan.plannedAt
        )

        assertEquals(
            expected =
                listOf(
                    LearningItemId("item-3"),
                    LearningItemId("item-1"),
                    LearningItemId("item-2")
                ),
            actual =
                plan.learningItemIds
        )

        assertEquals(
            expected = 3,
            actual =
                plan.totalItemCount
        )

        assertFalse(
            plan.isEmpty
        )

        assertTrue(
            LearningItemId("item-1") in
                    plan
        )
    }

    @Test
    fun `plan copies input item list`() {
        val mutableIds =
            mutableListOf(
                LearningItemId("item-1"),
                LearningItemId("item-2")
            )

        val plan =
            StudyQueuePlan.create(
                sessionId =
                    SessionId("session-1"),
                plannedAt =
                    Moment(1_000L),
                learningItemIds =
                    mutableIds
            )

        mutableIds.clear()

        assertEquals(
            expected =
                listOf(
                    LearningItemId("item-1"),
                    LearningItemId("item-2")
                ),
            actual =
                plan.learningItemIds
        )
    }

    @Test
    fun `empty plan is valid`() {
        val plan =
            StudyQueuePlan.create(
                sessionId =
                    SessionId("session-1"),
                plannedAt =
                    Moment(1_000L),
                learningItemIds =
                    emptyList()
            )

        assertTrue(
            plan.isEmpty
        )

        assertEquals(
            expected = 0,
            actual =
                plan.totalItemCount
        )
    }

    @Test
    fun `plan rejects duplicate learning item ids`() {
        assertFailsWith<
                IllegalArgumentException
                > {
            StudyQueuePlan.create(
                sessionId =
                    SessionId("session-1"),
                plannedAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        LearningItemId("item-1"),
                        LearningItemId("item-1")
                    )
            )
        }
    }

    @Test
    fun `plans with equal values are equal`() {
        val first =
            StudyQueuePlan.create(
                sessionId =
                    SessionId("session-1"),
                plannedAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        LearningItemId("item-1")
                    )
            )

        val second =
            StudyQueuePlan.create(
                sessionId =
                    SessionId("session-1"),
                plannedAt =
                    Moment(1_000L),
                learningItemIds =
                    listOf(
                        LearningItemId("item-1")
                    )
            )

        assertEquals(
            expected = first,
            actual = second
        )

        assertEquals(
            expected =
                first.hashCode(),
            actual =
                second.hashCode()
        )
    }
}