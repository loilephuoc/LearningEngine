package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.scheduling.SimpleScheduler
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository

class RatingInventoryQueryTest {
    @Test
    fun `inventory counts latest committed rating once per content and keeps never reviewed`() {
        val learner = LearnerId("learner")
        val items = InMemoryLearningItemRepository()
        val memories = InMemoryMemoryStateRepository()
        val events = InMemoryReviewEventRepository()
        val contents = (1..5).map { ContentId("content-$it") }
        val representatives = contents.mapIndexed { index, content ->
            LearningItem(LearningItemId("item-$index"), content, LearningMode.MEANING_RECALL)
                .also(items::save)
        }
        items.save(LearningItem(LearningItemId("sibling"), contents.first(), LearningMode.MEANING_RECOGNITION))
        val review = ReviewLearningItemUseCase(memories, events, SimpleScheduler())
        listOf(ReviewRating.AGAIN, ReviewRating.HARD, ReviewRating.GOOD, ReviewRating.EASY)
            .forEachIndexed { index, rating ->
                review.execute(
                    ReviewCommand(
                        ReviewEventId("event-$index"), learner, representatives[index].id,
                        rating, Moment(100L + index)
                    )
                )
            }
        review.execute(
            ReviewCommand(
                ReviewEventId("latest-first-content"), learner, representatives.first().id,
                ReviewRating.GOOD, Moment(200)
            )
        )

        val inventory = RatingInventoryQuery(items, memories, events, null).execute(
            LearnEntryScope(
                learner,
                InstalledPackageId("package"),
                topicId = null,
                includedContentIds = contents.toSet()
            )
        )

        assertEquals(RatingInventory(0, 1, 2, 1, 1, 5), inventory)
    }
}
