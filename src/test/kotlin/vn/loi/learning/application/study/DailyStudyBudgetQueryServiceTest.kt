package vn.loi.learning.application.study

import java.time.ZoneId
import kotlin.test.*
import kotlin.test.Test
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DailyStudyBudgetQueryServiceTest {
    private val learner = LearnerId("daily-learner")
    private val day = 1_700_000_000_000L
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun `daily budget is learner global supports 20 to 50 increase and local day rollover`() {
        val context = LearningApplicationFactory.createInMemory()
        val contentIds = seed(context, 50)
        repeat(20) { index -> review(context, index, day + index) }
        repeat(5) { index -> review(context, index, day + 1_000 + index) }

        val capped = context.dailyStudyBudget!!.execute(
            learner, DailyStudyBudgetLimits(20, 100), Moment(day + 2_000), zone, contentIds
        )
        assertEquals(20, capped.newCompletedToday)
        assertEquals(5, capped.reviewCompletedToday)
        assertEquals(0, capped.newRemainingToday)
        assertEquals(95, capped.reviewRemainingToday)

        val increased = context.dailyStudyBudget!!.execute(
            learner, DailyStudyBudgetLimits(50, 100), Moment(day + 2_000), zone, contentIds
        )
        assertEquals(30, increased.newRemainingToday)
        val switchedPackageScope = context.dailyStudyBudget!!.execute(
            learner, DailyStudyBudgetLimits(50, 100), Moment(day + 2_000), zone, setOf(ContentId("daily-content-49"))
        )
        assertEquals(20, switchedPackageScope.newCompletedToday, "switching package must not reset learner budget")

        val nextLocalDay = Moment(day + 86_400_000)
        val reset = context.dailyStudyBudget!!.execute(
            learner, DailyStudyBudgetLimits(), nextLocalDay, zone, contentIds
        )
        assertEquals(0, reset.newCompletedToday)
        assertEquals(0, reset.reviewCompletedToday)
        assertEquals(20, reset.newRemainingToday)
        assertEquals(25, context.reviewEventRepository!!.findAll(learner).size)
    }

    @Test
    fun `limits validate defaults and exclude other learners`() {
        assertEquals(DailyStudyBudgetLimits(20, 100), DailyStudyBudgetLimits())
        assertFailsWith<IllegalArgumentException> { DailyStudyBudgetLimits(0, 100) }
        assertFailsWith<IllegalArgumentException> { DailyStudyBudgetLimits(20, 1_000) }
        val context = LearningApplicationFactory.createInMemory()
        val ids = seed(context, 1)
        review(context, 0, day)
        val other = context.dailyStudyBudget!!.execute(
            LearnerId("other"), DailyStudyBudgetLimits(), Moment(day + 1), zone, ids
        )
        assertEquals(0, other.newCompletedToday)
    }

    @Test
    fun `mixed remaining budgets independently allow NEW-only and REVIEW-only work`() {
        assertTrue(DailyStudyBudgetSnapshot(DailyStudyBudgetLimits(20, 100), 20, 0, 3, 0).hasEligibleWork)
        assertTrue(DailyStudyBudgetSnapshot(DailyStudyBudgetLimits(20, 100), 0, 100, 0, 4).hasEligibleWork)
        assertTrue(DailyStudyBudgetSnapshot(DailyStudyBudgetLimits(20, 100), 0, 0, 3, 4).hasEligibleWork)
        assertFalse(DailyStudyBudgetSnapshot(DailyStudyBudgetLimits(20, 100), 20, 100, 3, 4).hasEligibleWork)
        assertFalse(DailyStudyBudgetSnapshot(DailyStudyBudgetLimits(20, 100), 20, 0, 0, 0).hasEligibleWork)
    }

    private fun seed(context: vn.loi.learning.infrastructure.LearningApplicationContext, count: Int): Set<ContentId> =
        (0 until count).mapTo(linkedSetOf()) { index ->
            val contentId = ContentId("daily-content-$index")
            context.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("word-$index", "meaning-$index")))
            context.learningItemRepository!!.save(
                LearningItem(LearningItemId("daily-item-$index"), contentId, LearningMode.MEANING_RECOGNITION)
            )
            contentId
        }

    private fun review(context: vn.loi.learning.infrastructure.LearningApplicationContext, index: Int, at: Long) {
        context.engine.review(ReviewCommand(
            ReviewEventId("daily-review-$index-$at"), learner, LearningItemId("daily-item-$index"),
            ReviewRating.GOOD, Moment(at)
        ))
    }
}
