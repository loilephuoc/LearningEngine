package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.session.model.SessionId

class SessionSeededNewItemOrdererTest {
    private val orderer = SessionSeededNewItemOrderer()

    @Test
    fun `same session and candidate set produce the same new ordering`() {
        val candidates = (1..12).map(::newCandidate)

        val first = orderer.order(candidates, SessionId("stable-session"))
        val reorderedInput = orderer.order(candidates.reversed(), SessionId("stable-session"))

        assertEquals(first.map { it.learningItemId }, reorderedInput.map { it.learningItemId })
    }

    @Test
    fun `different sessions generally produce different new ordering`() {
        val candidates = (1..12).map(::newCandidate)

        val first = orderer.order(candidates, SessionId("session-a"))
        val second = orderer.order(candidates, SessionId("session-b"))

        assertNotEquals(first.map { it.learningItemId }, second.map { it.learningItemId })
    }

    @Test
    fun `review subsequence and slots remain unchanged`() {
        val firstReview = reviewCandidate(101)
        val secondReview = reviewCandidate(102)
        val candidates = listOf(newCandidate(1), firstReview, newCandidate(2), newCandidate(3), secondReview)

        val result = orderer.order(candidates, SessionId("mixed-session"))

        assertEquals(listOf(1, 4), result.indices.filter { !result[it].isNew })
        assertEquals(listOf(firstReview, secondReview), result.filterNot { it.isNew })
    }

    @Test
    fun `990 item seeded diversity pipeline is deterministic complete and identity preserving`() {
        val candidates = (1..990).map { number ->
            candidate(number, isNew = true, contentNumber = (number - 1) / 3)
        }
        val sessionId = SessionId("large-package-session")
        val diversifier = ContentDiversityQueueDiversifier()

        val first = diversifier.diversify(orderer.order(candidates, sessionId))
        val second = diversifier.diversify(orderer.order(candidates, sessionId))

        assertEquals(990, first.size)
        assertEquals(first.map { it.learningItemId }, second.map { it.learningItemId })
        assertEquals(candidates.map { it.learningItemId }.toSet(), first.map { it.learningItemId }.toSet())
        assertEquals(990, first.map { it.learningItemId }.distinct().size)
        assertTrue(first.zipWithNext().all { (left, right) ->
            left.contentId != right.contentId || first.all { it.contentId == left.contentId }
        })
    }

    private fun newCandidate(number: Int): SelectionCandidate = candidate(number, true)

    private fun reviewCandidate(number: Int): SelectionCandidate = candidate(number, false)

    private fun candidate(number: Int, isNew: Boolean, contentNumber: Int = number): SelectionCandidate {
        val id = LearningItemId("seeded-item-$number")
        return SelectionCandidate(
            learningItem = LearningItem(id, ContentId("seeded-content-$contentNumber"), LearningMode.MEANING_RECOGNITION),
            memoryState = if (isNew) {
                MemoryState.new(LearnerId("learner"), id, Moment(1_000L))
            } else {
                MemoryState(
                    learnerId = LearnerId("learner"),
                    learningItemId = id,
                    stage = vn.loi.learning.domain.study.memory.model.LearningStage.REVIEW,
                    difficulty = MemoryState.DEFAULT_DIFFICULTY,
                    stabilityDays = 1.0,
                    dueAt = Moment(1_000L),
                    lastReviewedAt = Moment(500L),
                    reviewCount = 1,
                    lapseCount = 0
                )
            }
        )
    }
}
