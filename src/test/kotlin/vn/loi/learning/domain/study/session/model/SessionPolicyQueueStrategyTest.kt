package vn.loi.learning.domain.study.session.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionPolicyQueueStrategyTest {

    @Test
    fun `default queue strategy remains review first`() {
        val policy =
            SessionPolicy()

        assertEquals(
            expected =
                StudyQueueStrategyType.REVIEW_FIRST,
            actual =
                policy.queueStrategy
        )
    }

    @Test
    fun `new first queue strategy can be selected`() {
        val policy =
            SessionPolicy(
                queueStrategy =
                    StudyQueueStrategyType.NEW_FIRST
            )

        assertEquals(
            expected =
                StudyQueueStrategyType.NEW_FIRST,
            actual =
                policy.queueStrategy
        )
    }
}