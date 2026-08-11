package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.infrastructure.persistence.mapper.StudyQueueRecordMapper

class AdaptivePracticeQueueTest {
    private val ids = (1..20).map { LearningItemId("item-$it") }

    @Test
    fun `typed feedback maps to graduated bounded reinforcement without starving base membership`() {
        var queue = adaptive()
        val difficult = requireNotNull(queue.currentLearningItemId)
        queue = queue.advancePractice(PracticeRecallResult.INCORRECT)
        val first = requireNotNull(queue.practiceReinforcementStates[difficult])
        assertEquals(2, first.previousGap)
        assertEquals(difficult, queue.learningItemIds[first.lastInsertionIndex!!])

        while (queue.currentLearningItemId != difficult) queue = queue.advancePractice()
        queue = queue.advancePractice(PracticeRecallResult.REVEALED)
        assertEquals(6, queue.practiceReinforcementStates[difficult]?.previousGap)
        while (queue.currentLearningItemId != difficult) queue = queue.advancePractice()
        queue = queue.advancePractice(PracticeRecallResult.INCORRECT)
        assertEquals(15, queue.practiceReinforcementStates[difficult]?.previousGap)
        while (queue.currentLearningItemId != difficult) queue = queue.advancePractice()
        queue = queue.advancePractice(PracticeRecallResult.INCORRECT)
        assertEquals(4, queue.practiceReinforcementStates[difficult]?.againCount)
        assertEquals(15, queue.practiceReinforcementStates[difficult]?.previousGap)
        assertEquals(ids.size, queue.learningItemIds.size)
        assertTrue(ids.all { it in queue.learningItemIds })
    }

    @Test
    fun `good creates no reinforcement and hard gaps increase`() {
        var queue = adaptive()
        queue = queue.advancePractice(PracticeRecallResult.CORRECT)
        assertTrue(queue.practiceReinforcementStates.isEmpty())
        val hard = requireNotNull(queue.currentLearningItemId)
        queue = queue.advancePractice(PracticeRecallResult.ALMOST_CORRECT)
        assertEquals(4, queue.practiceReinforcementStates[hard]?.previousGap)
        while (queue.currentLearningItemId != hard) queue = queue.advancePractice()
        queue = queue.advancePractice(PracticeRecallResult.ALMOST_CORRECT)
        assertEquals(12, queue.practiceReinforcementStates[hard]?.previousGap)
    }

    @Test
    fun `adaptive state survives round trip but a new session starts empty`() {
        val advanced = adaptive().advancePractice(PracticeRecallResult.INCORRECT)
        assertEquals(advanced, StudyQueueRecordMapper.toDomain(StudyQueueRecordMapper.toRecord(advanced)))
        assertTrue(adaptive().practiceReinforcementStates.isEmpty())
    }

    @Test
    fun `semantic priority tiers rank latest feedback and Good graduates old difficulty`() {
        assertTrue(
            ContinuousSkimPriorityPolicy.tier(PracticeFeedback.AGAIN_LIKE) <
                ContinuousSkimPriorityPolicy.tier(PracticeFeedback.HARD_LIKE)
        )
        assertTrue(
            ContinuousSkimPriorityPolicy.tier(PracticeFeedback.HARD_LIKE) <
                ContinuousSkimPriorityPolicy.tier(PracticeFeedback.GOOD_LIKE)
        )
        assertTrue(
            ContinuousSkimPriorityPolicy.tier(PracticeFeedback.GOOD_LIKE) <
                ContinuousSkimPriorityPolicy.tier(PracticeFeedback.EASY_LIKE)
        )

        val item = adaptive().currentLearningItemId!!
        val difficult = adaptive().updateAdaptivePriority(item, ReviewRating.AGAIN)
        assertEquals(PracticeFeedback.AGAIN_LIKE, difficult.practiceReinforcementStates[item]?.latestFeedback)
        val graduated = difficult.updateAdaptivePriority(item, ReviewRating.GOOD)
        assertFalse(item in graduated.practiceReinforcementStates)
        val reentered = graduated.updateAdaptivePriority(item, ReviewRating.AGAIN)
        assertEquals(PracticeFeedback.AGAIN_LIKE, reentered.practiceReinforcementStates[item]?.latestFeedback)
    }

    @Test
    fun `round boundary preserves Again spacing and Easy participates less often`() {
        var queue = adaptive()
        while (!queue.isLastItem) queue = queue.advancePractice()
        val boundaryAgain = queue.currentLearningItemId!!
        queue = queue.advancePractice(PracticeRecallResult.INCORRECT)
        assertTrue(queue.learningItemIds.indexOf(boundaryAgain) >= 2)

        val easy = queue.currentLearningItemId!!
        queue = queue.updateAdaptivePriority(easy, ReviewRating.EASY)
        while (queue.practiceRound == 2) queue = queue.advancePractice()
        assertEquals(3, queue.practiceRound)
        while (queue.practiceRound == 3) {
            queue = queue.advancePractice()
            queue = queue.updateAdaptivePriority(easy, ReviewRating.EASY)
        }
        assertEquals(4, queue.practiceRound)
        assertFalse(easy in queue.learningItemIds)
        assertTrue(queue.learningItemIds.isNotEmpty())
    }

    @Test
    fun `fixed seed and feedback produce deterministic round routing`() {
        fun route(): List<LearningItemId> {
            var queue = adaptive().advancePractice(PracticeRecallResult.INCORRECT)
            while (queue.practiceRound == 1) queue = queue.advancePractice()
            return queue.learningItemIds
        }
        assertEquals(route(), route())
    }

    @Test
    fun `dynamic membership removes future entries restores exactly and completes at zero`() {
        val current = LearningItemId("only")
        var queue = StudyQueueSnapshot.create(
            SessionId("dynamic"), Moment(1), listOf(current),
            itemOrigins = mapOf(current to SessionItemOrigin.REVIEW),
            itemContentIds = mapOf(current to ContentId("only-content")), practiceSeed = 2,
            practiceLoopPolicy = PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP
        )
        val removed = queue.updateDifficultMembership(current, ReviewRating.GOOD)
        assertTrue(removed.fixedPracticeMembership.isEmpty())
        val restored = removed.restorePracticeMembershipUndo(current)
        assertEquals(listOf(current), restored.fixedPracticeMembership)
        assertEquals(restored, restored.restorePracticeMembershipUndo(current))

        queue = removed.advancePractice()
        assertTrue(queue.isCompleted)
        assertNull(queue.practiceProgress)
        assertFalse(current in queue.pendingLearningItemIds)
    }

    @Test
    fun `dynamic membership retains Again and Hard without duplicates`() {
        listOf(ReviewRating.AGAIN, ReviewRating.HARD).forEach { rating ->
            val queue = dynamic().updateDifficultMembership(dynamic().currentLearningItemId!!, rating)
            assertEquals(queue.fixedPracticeMembership.distinct(), queue.fixedPracticeMembership)
        }
    }

    @Test
    fun `dynamic practice feedback graduates Correct locally and retains other outcomes`() {
        val correctItem = dynamic().currentLearningItemId!!
        val graduated = dynamic().advancePractice(
            PracticeRecallResult.CORRECT,
            graduateCorrectLocally = true
        )
        assertFalse(correctItem in graduated.fixedPracticeMembership)

        listOf(
            PracticeRecallResult.INCORRECT,
            PracticeRecallResult.REVEALED,
            PracticeRecallResult.ALMOST_CORRECT
        ).forEach { result ->
            val queue = dynamic()
            val current = queue.currentLearningItemId!!
            assertTrue(current in queue.advancePractice(
                result,
                graduateCorrectLocally = true
            ).fixedPracticeMembership)
        }
    }

    @Test
    fun `three removals from twelve produce nine item next round`() {
        val twelve = (1..12).map { LearningItemId("twelve-$it") }
        var queue = StudyQueueSnapshot.create(
            SessionId("twelve"), Moment(1), twelve, practiceSeed = 19,
            practiceLoopPolicy = PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP
        )
        repeat(3) {
            val current = requireNotNull(queue.currentLearningItemId)
            queue = queue.updateDifficultMembership(current, if (it == 2) ReviewRating.EASY else ReviewRating.GOOD)
            queue = queue.advancePractice()
            val progress = requireNotNull(queue.practiceProgress)
            assertTrue(progress.position in 1..progress.membershipSize)
        }
        while (queue.practiceRound == 1) queue = queue.advancePractice()
        assertEquals(9, queue.fixedPracticeMembership.size)
        assertEquals(9, queue.learningItemIds.size)
    }

    private fun adaptive() = StudyQueueSnapshot.create(
        SessionId("adaptive"), Moment(1), ids, practiceSeed = 42,
        practiceLoopPolicy = PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED
    )

    private fun dynamic() = StudyQueueSnapshot.create(
        SessionId("difficult"), Moment(1), ids.take(3), practiceSeed = 9,
        practiceLoopPolicy = PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP
    )
}
