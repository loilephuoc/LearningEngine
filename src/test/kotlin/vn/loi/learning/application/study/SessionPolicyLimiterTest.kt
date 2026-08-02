package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
    fun `New quota selects one deterministic representative per Content`() {
        val result = limiter.applyEntries(
            listOf(
                newEntry("a-meaning", "content-a"),
                newEntry("a-listening", "content-a"),
                newEntry("b-meaning", "content-b")
            ),
            SessionPolicy(newItemLimit = 1, reviewItemLimit = 0)
        )

        assertEquals(
            listOf(LearningItemId("a-meaning")),
            result.map { it.learningItemId }
        )
    }

    @Test
    fun `early sibling candidates do not underfill fifty unique Content goal`() {
        val entries = buildList {
            repeat(80) { content ->
                repeat(if (content < 13) 4 else 3) { sibling ->
                    add(newEntry("item-$content-$sibling", "content-$content"))
                }
            }
            repeat(20) { content ->
                repeat(3) { sibling ->
                    add(reviewEntry("review-$content-$sibling", "review-content-$content"))
                }
            }
        }

        val result = limiter.applyEntries(
            entries,
            SessionPolicy(newItemLimit = 50, reviewItemLimit = 200)
        )

        val newResults = result.filter(StudyQueuePlanEntry::isNew)
        val reviewResults = result.filterNot(StudyQueuePlanEntry::isNew)
        assertEquals(50, newResults.size)
        assertEquals(50, newResults.mapNotNull { it.contentId }.distinct().size)
        assertEquals(20, reviewResults.size)
        assertEquals(20, reviewResults.mapNotNull { it.contentId }.distinct().size)
    }

    @Test
    fun `genuine inventory underfill records only available unique Content`() {
        val entries = (0 until 13).flatMap { content ->
            (0 until 4).map { sibling ->
                newEntry("item-$content-$sibling", "content-$content")
            }
        }

        val result = limiter.applyEntries(
            entries,
            SessionPolicy(newItemLimit = 50, reviewItemLimit = 0)
        )

        assertEquals(13, result.mapNotNull { it.contentId }.distinct().size)
        assertEquals(13, result.size)
    }

    @Test
    fun `five plus five selects exactly ten representatives despite siblings`() {
        val entries = buildList {
            repeat(5) { content -> repeat(4) { sibling -> add(newEntry("new-$content-$sibling", "new-$content")) } }
            repeat(5) { content -> repeat(3) { sibling -> add(reviewEntry("review-$content-$sibling", "review-$content")) } }
        }
        val result = limiter.applyEntries(entries, SessionPolicy(5, 5))

        assertEquals(10, result.size)
        assertEquals(10, result.mapNotNull { it.contentId }.distinct().size)
        assertEquals((0 until 5).map { LearningItemId("new-$it-0") }, result.take(5).map { it.learningItemId })
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
        id: String,
        contentId: String? = null
    ): StudyQueuePlanEntry =
        StudyQueuePlanEntry(
            learningItemId =
                LearningItemId(id),
            isNew = false,
            contentId = contentId?.let(::ContentId)
        )
}
