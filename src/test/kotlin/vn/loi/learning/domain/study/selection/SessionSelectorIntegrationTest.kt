package vn.loi.learning.domain.study.selection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.selection.model.NoSelectionReason
import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.selection.model.SessionSelectionRequest
import vn.loi.learning.domain.study.selection.model.SessionSelectionResult
import vn.loi.learning.domain.study.selection.policy.SelectionPriority
import vn.loi.learning.domain.study.selection.policy.SessionSelectionPolicy
import vn.loi.learning.domain.study.selection.service.SessionSelector
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession

class SessionSelectorIntegrationTest {

    private val selector = SessionSelector()

    @Test
    fun `review first selects due review before new item`() {
        val newCandidate =
            newCandidate(
                itemId = "new-item",
                contentId = "new-content",
                dueAt = NOW
            )

        val reviewCandidate =
            reviewCandidate(
                itemId = "review-item",
                contentId = "review-content",
                dueAt = NOW
            )

        val result =
            selector.select(
                request(
                    candidates = listOf(newCandidate, reviewCandidate),
                    priority = SelectionPriority.REVIEW_FIRST
                )
            )

        val selected =
            assertIs<SessionSelectionResult.Selected>(result)

        assertEquals(
            reviewCandidate.learningItemId,
            selected.candidate.learningItemId
        )

        assertFalse(selected.usedSiblingFallback)
    }

    @Test
    fun `new first selects new item before due review`() {
        val reviewCandidate =
            reviewCandidate(
                itemId = "review-item",
                contentId = "review-content",
                dueAt = NOW
            )

        val newCandidate =
            newCandidate(
                itemId = "new-item",
                contentId = "new-content",
                dueAt = NOW
            )

        val result =
            selector.select(
                request(
                    candidates = listOf(reviewCandidate, newCandidate),
                    priority = SelectionPriority.NEW_FIRST
                )
            )

        val selected =
            assertIs<SessionSelectionResult.Selected>(result)

        assertEquals(
            newCandidate.learningItemId,
            selected.candidate.learningItemId
        )
    }

    @Test
    fun `disabled and future candidates are not eligible`() {
        val disabledCandidate =
            newCandidate(
                itemId = "disabled-item",
                contentId = "disabled-content",
                dueAt = NOW,
                isEnabled = false
            )

        val futureCandidate =
            newCandidate(
                itemId = "future-item",
                contentId = "future-content",
                dueAt = FUTURE
            )

        val result =
            selector.select(
                request(
                    candidates =
                        listOf(
                            disabledCandidate,
                            futureCandidate
                        )
                )
            )

        val noSelection =
            assertIs<SessionSelectionResult.NoSelection>(result)

        assertEquals(
            NoSelectionReason.NO_ELIGIBLE_CANDIDATES,
            noSelection.reason
        )
    }

    @Test
    fun `reviewed item is excluded when repeat is not allowed`() {
        val candidate =
            reviewCandidate(
                itemId = "reviewed-item",
                contentId = "content-a",
                dueAt = NOW
            )

        val session =
            activeSession(
                policy =
                    SessionPolicy(
                        newItemLimit = 10,
                        reviewItemLimit = 10,
                        allowRepeatInSameSession = false
                    )
            ).recordReview(
                learningItemId = candidate.learningItemId,
                contentId = candidate.contentId,
                wasNewItem = false
            )

        val result =
            selector.select(
                request(
                    candidates = listOf(candidate),
                    session = session
                )
            )

        val noSelection =
            assertIs<SessionSelectionResult.NoSelection>(result)

        assertEquals(
            NoSelectionReason.REPEAT_NOT_ALLOWED,
            noSelection.reason
        )
    }

    @Test
    fun `reviewed item can be selected again when repeat is allowed`() {
        val candidate =
            reviewCandidate(
                itemId = "repeatable-item",
                contentId = "content-a",
                dueAt = NOW
            )

        val session =
            activeSession(
                policy =
                    SessionPolicy(
                        newItemLimit = 10,
                        reviewItemLimit = 10,
                        allowRepeatInSameSession = true
                    )
            ).recordReview(
                learningItemId = candidate.learningItemId,
                contentId = candidate.contentId,
                wasNewItem = false
            )

        val result =
            selector.select(
                request(
                    candidates = listOf(candidate),
                    session = session
                )
            )

        val selected =
            assertIs<SessionSelectionResult.Selected>(result)

        assertEquals(
            candidate.learningItemId,
            selected.candidate.learningItemId
        )
    }

    @Test
    fun `selector avoids consecutive sibling when another content exists`() {
        val siblingCandidate =
            reviewCandidate(
                itemId = "sibling-item",
                contentId = "content-a",
                dueAt = EARLIER
            )

        val nonSiblingCandidate =
            reviewCandidate(
                itemId = "non-sibling-item",
                contentId = "content-b",
                dueAt = NOW
            )

        val result =
            selector.select(
                request(
                    candidates =
                        listOf(
                            siblingCandidate,
                            nonSiblingCandidate
                        ),
                    previousContentId = ContentId("content-a")
                )
            )

        val selected =
            assertIs<SessionSelectionResult.Selected>(result)

        assertEquals(
            nonSiblingCandidate.learningItemId,
            selected.candidate.learningItemId
        )

        assertFalse(selected.usedSiblingFallback)
    }

    @Test
    fun `selector uses sibling fallback when only siblings remain`() {
        val firstSibling =
            reviewCandidate(
                itemId = "sibling-1",
                contentId = "content-a",
                dueAt = EARLIER
            )

        val secondSibling =
            reviewCandidate(
                itemId = "sibling-2",
                contentId = "content-a",
                dueAt = NOW
            )

        val result =
            selector.select(
                request(
                    candidates =
                        listOf(
                            secondSibling,
                            firstSibling
                        ),
                    previousContentId = ContentId("content-a"),
                    allowSiblingFallback = true
                )
            )

        val selected =
            assertIs<SessionSelectionResult.Selected>(result)

        assertEquals(
            firstSibling.learningItemId,
            selected.candidate.learningItemId
        )

        assertTrue(selected.usedSiblingFallback)
    }

    @Test
    fun `selector reports sibling blocked when fallback is disabled`() {
        val siblingCandidate =
            reviewCandidate(
                itemId = "sibling-item",
                contentId = "content-a",
                dueAt = NOW
            )

        val result =
            selector.select(
                request(
                    candidates = listOf(siblingCandidate),
                    previousContentId = ContentId("content-a"),
                    allowSiblingFallback = false
                )
            )

        val noSelection =
            assertIs<SessionSelectionResult.NoSelection>(result)

        assertEquals(
            NoSelectionReason.SIBLING_BLOCKED,
            noSelection.reason
        )
    }

    @Test
    fun `finished session cannot select another item`() {
        val candidate =
            reviewCandidate(
                itemId = "review-item",
                contentId = "content-a",
                dueAt = NOW
            )

        val finishedSession =
            activeSession().finish(FINISHED_AT)

        val result =
            selector.select(
                request(
                    candidates = listOf(candidate),
                    session = finishedSession
                )
            )

        val noSelection =
            assertIs<SessionSelectionResult.NoSelection>(result)

        assertEquals(
            NoSelectionReason.SESSION_FINISHED,
            noSelection.reason
        )
    }

    @Test
    fun `new quota excludes new items while review quota remains available`() {
        val newCandidate =
            newCandidate(
                itemId = "new-item",
                contentId = "new-content",
                dueAt = NOW
            )

        val reviewCandidate =
            reviewCandidate(
                itemId = "review-item",
                contentId = "review-content",
                dueAt = NOW
            )

        val session =
            activeSession(
                policy =
                    SessionPolicy(
                        newItemLimit = 1,
                        reviewItemLimit = 10
                    )
            ).recordReview(
                learningItemId = LearningItemId("previous-new-item"),
                contentId = ContentId("previous-new-content"),
                wasNewItem = true
            )

        val result =
            selector.select(
                request(
                    candidates =
                        listOf(
                            newCandidate,
                            reviewCandidate
                        ),
                    session = session,
                    priority = SelectionPriority.NEW_FIRST
                )
            )

        val selected =
            assertIs<SessionSelectionResult.Selected>(result)

        assertEquals(
            reviewCandidate.learningItemId,
            selected.candidate.learningItemId
        )
    }

    @Test
    fun `selector reports session limit when all quotas are reached`() {
        val candidate =
            reviewCandidate(
                itemId = "another-review-item",
                contentId = "another-content",
                dueAt = NOW
            )

        val session =
            activeSession(
                policy =
                    SessionPolicy(
                        newItemLimit = 1,
                        reviewItemLimit = 1
                    )
            )
                .recordReview(
                    learningItemId = LearningItemId("completed-new-item"),
                    contentId = ContentId("completed-new-content"),
                    wasNewItem = true
                )
                .recordReview(
                    learningItemId = LearningItemId("completed-review-item"),
                    contentId = ContentId("completed-review-content"),
                    wasNewItem = false
                )

        val result =
            selector.select(
                request(
                    candidates = listOf(candidate),
                    session = session
                )
            )

        val noSelection =
            assertIs<SessionSelectionResult.NoSelection>(result)

        assertEquals(
            NoSelectionReason.SESSION_LIMIT_REACHED,
            noSelection.reason
        )
    }

    @Test
    fun `older due item is selected first within the same priority group`() {
        val laterCandidate =
            reviewCandidate(
                itemId = "later-item",
                contentId = "content-later",
                dueAt = NOW
            )

        val earlierCandidate =
            reviewCandidate(
                itemId = "earlier-item",
                contentId = "content-earlier",
                dueAt = EARLIER
            )

        val result =
            selector.select(
                request(
                    candidates =
                        listOf(
                            laterCandidate,
                            earlierCandidate
                        )
                )
            )

        val selected =
            assertIs<SessionSelectionResult.Selected>(result)

        assertEquals(
            earlierCandidate.learningItemId,
            selected.candidate.learningItemId
        )
    }


    @Test
    fun `learning item id breaks due time ties deterministically`() {

        val secondCandidate =
            reviewCandidate(
                itemId = "item-002",
                contentId = "content-b",
                dueAt = NOW
            )

        val firstCandidate =
            reviewCandidate(
                itemId = "item-001",
                contentId = "content-a",
                dueAt = NOW
            )

        val result =
            selector.select(
                request(
                    candidates =
                        listOf(
                            secondCandidate,
                            firstCandidate
                        )
                )
            )

        val selected =
            assertIs<SessionSelectionResult.Selected>(result)

        assertEquals(
            firstCandidate.learningItemId,
            selected.candidate.learningItemId
        )
    }

    @Test
    fun `priority ordering is independent of input order`() {

        val candidate1 =
            reviewCandidate(
                itemId = "item-003",
                contentId = "content-3",
                dueAt = NOW
            )

        val candidate2 =
            reviewCandidate(
                itemId = "item-001",
                contentId = "content-1",
                dueAt = NOW
            )

        val candidate3 =
            reviewCandidate(
                itemId = "item-002",
                contentId = "content-2",
                dueAt = NOW
            )

        val result =
            selector.select(
                request(
                    candidates =
                        listOf(
                            candidate1,
                            candidate3,
                            candidate2
                        )
                )
            )

        val selected =
            assertIs<SessionSelectionResult.Selected>(result)

        assertEquals(
            candidate2.learningItemId,
            selected.candidate.learningItemId
        )
    }

    private fun request(
        candidates: List<SelectionCandidate>,
        session: StudySession = activeSession(),
        priority: SelectionPriority = SelectionPriority.REVIEW_FIRST,
        previousContentId: ContentId? = null,
        avoidConsecutiveSiblings: Boolean = true,
        allowSiblingFallback: Boolean = true
    ): SessionSelectionRequest =
        SessionSelectionRequest(
            candidates = candidates,
            session = session,
            selectionPolicy =
                SessionSelectionPolicy(
                    priority = priority,
                    avoidConsecutiveSiblings = avoidConsecutiveSiblings,
                    allowSiblingFallback = allowSiblingFallback
                ),
            at = NOW,
            previousContentId = previousContentId
        )

    private fun activeSession(
        policy: SessionPolicy =
            SessionPolicy(
                newItemLimit = 20,
                reviewItemLimit = 100,
                allowRepeatInSameSession = false
            )
    ): StudySession =
        StudySession.start(
            id = SessionId("session-1"),
            learnerId = LEARNER_ID,
            startedAt = STARTED_AT,
            policy = policy
        )

    private fun newCandidate(
        itemId: String,
        contentId: String,
        dueAt: Moment,
        isEnabled: Boolean = true
    ): SelectionCandidate {

        val learningItem =
            learningItem(
                itemId = itemId,
                contentId = contentId,
                isEnabled = isEnabled
            )

        val memoryState =
            MemoryState.new(
                learnerId = LEARNER_ID,
                learningItemId = learningItem.id,
                availableAt = dueAt
            )

        return SelectionCandidate(
            learningItem = learningItem,
            memoryState = memoryState
        )
    }

    private fun reviewCandidate(
        itemId: String,
        contentId: String,
        dueAt: Moment,
        isEnabled: Boolean = true
    ): SelectionCandidate {

        val learningItem =
            learningItem(
                itemId = itemId,
                contentId = contentId,
                isEnabled = isEnabled
            )

        val memoryState =
            MemoryState(
                learnerId = LEARNER_ID,
                learningItemId = learningItem.id,
                stage = LearningStage.REVIEW,
                difficulty = MemoryState.DEFAULT_DIFFICULTY,
                stabilityDays = 5.0,
                dueAt = dueAt,
                lastReviewedAt = LAST_REVIEWED_AT,
                reviewCount = 1,
                lapseCount = 0
            )

        return SelectionCandidate(
            learningItem = learningItem,
            memoryState = memoryState
        )
    }

    private fun learningItem(
        itemId: String,
        contentId: String,
        isEnabled: Boolean
    ): LearningItem =
        LearningItem(
            id = LearningItemId(itemId),
            contentId = ContentId(contentId),
            mode = LearningMode.MEANING_RECOGNITION,
            isEnabled = isEnabled
        )

    private companion object {
        val LEARNER_ID = LearnerId("learner-1")

        val STARTED_AT = Moment(1_000L)
        val LAST_REVIEWED_AT = Moment(2_000L)
        val EARLIER = Moment(3_000L)
        val NOW = Moment(5_000L)
        val FINISHED_AT = Moment(6_000L)
        val FUTURE = Moment(10_000L)
    }
}