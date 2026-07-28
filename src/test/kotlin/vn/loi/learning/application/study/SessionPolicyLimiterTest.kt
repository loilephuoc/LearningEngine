package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.session.model.SessionPolicy

class SessionPolicyLimiterTest {

    private val limiter =
        SessionPolicyLimiter()

    @Test
    fun `limits new and review items independently`() {
        val result =
            limiter.apply(
                orderedEntries =
                    listOf(
                        reviewEntry("review-1"),
                        reviewEntry("review-2"),
                        reviewEntry("review-3"),
                        newEntry("new-1"),
                        newEntry("new-2"),
                        newEntry("new-3")
                    ),
                policy =
                    SessionPolicy(
                        newItemLimit = 2,
                        reviewItemLimit = 1
                    )
            )

        assertEquals(
            expected =
                listOf(
                    LearningItemId("review-1"),
                    LearningItemId("new-1"),
                    LearningItemId("new-2")
                ),
            actual =
                result
        )
    }

    @Test
    fun `preserves original ordering while skipping excess items`() {
        val result =
            limiter.apply(
                orderedEntries =
                    listOf(
                        reviewEntry("review-1"),
                        newEntry("new-1"),
                        reviewEntry("review-2"),
                        newEntry("new-2"),
                        reviewEntry("review-3"),
                        newEntry("new-3")
                    ),
                policy =
                    SessionPolicy(
                        newItemLimit = 2,
                        reviewItemLimit = 2
                    )
            )

        assertEquals(
            expected =
                listOf(
                    LearningItemId("review-1"),
                    LearningItemId("new-1"),
                    LearningItemId("review-2"),
                    LearningItemId("new-2")
                ),
            actual =
                result
        )
    }

    @Test
    fun `zero new limit excludes all new items`() {
        val result =
            limiter.apply(
                orderedEntries =
                    listOf(
                        newEntry("new-1"),
                        reviewEntry("review-1"),
                        newEntry("new-2"),
                        reviewEntry("review-2")
                    ),
                policy =
                    SessionPolicy(
                        newItemLimit = 0,
                        reviewItemLimit = 2
                    )
            )

        assertEquals(
            expected =
                listOf(
                    LearningItemId("review-1"),
                    LearningItemId("review-2")
                ),
            actual =
                result
        )
    }

    @Test
    fun `zero review limit excludes all review items`() {
        val result =
            limiter.apply(
                orderedEntries =
                    listOf(
                        reviewEntry("review-1"),
                        newEntry("new-1"),
                        reviewEntry("review-2"),
                        newEntry("new-2")
                    ),
                policy =
                    SessionPolicy(
                        newItemLimit = 2,
                        reviewItemLimit = 0
                    )
            )

        assertEquals(
            expected =
                listOf(
                    LearningItemId("new-1"),
                    LearningItemId("new-2")
                ),
            actual =
                result
        )
    }

    @Test
    fun `returns all entries when counts are below limits`() {
        val entries =
            listOf(
                reviewEntry("review-1"),
                newEntry("new-1"),
                reviewEntry("review-2")
            )

        val result =
            limiter.apply(
                orderedEntries =
                    entries,
                policy =
                    SessionPolicy(
                        newItemLimit = 5,
                        reviewItemLimit = 5
                    )
            )

        assertEquals(
            expected =
                entries.map { entry ->
                    entry.learningItemId
                },
            actual =
                result
        )
    }

    @Test
    fun `New quota counts unique Content while retaining sibling experiences`() {
        val result = limiter.applyEntries(
            listOf(
                newEntry("a-meaning", "content-a"),
                newEntry("a-listening", "content-a"),
                newEntry("b-meaning", "content-b")
            ),
            SessionPolicy(newItemLimit = 1, reviewItemLimit = 0)
        )

        assertEquals(
            listOf(LearningItemId("a-meaning"), LearningItemId("a-listening")),
            result.map { it.learningItemId }
        )
    }

    private fun newEntry(
        id: String,
        contentId: String? = null
    ): StudyQueuePlanEntry =
        StudyQueuePlanEntry(
            learningItemId =
                LearningItemId(id),
            isNew = true,
            contentId = contentId?.let(::ContentId)
        )

    private fun reviewEntry(
        id: String
    ): StudyQueuePlanEntry =
        StudyQueuePlanEntry(
            learningItemId =
                LearningItemId(id),
            isNew = false
        )
}
