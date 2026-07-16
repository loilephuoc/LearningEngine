package vn.loi.learning

import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.infrastructure.LearningEngineFactory

fun main() {
    val engine = LearningEngineFactory.createInMemory()
    val learnerId = LearnerId("loi")

    registerSampleData(engine)

    var now = Moment(1_000_000L)

    println("=== LEARNING ENGINE 2.0 ===")
    println()

    val first = engine.getNextLearningItem(
        learnerId = learnerId,
        now = now
    )

    requireNotNull(first)

    println("Next item: ${first.content.displayName}")
    println("Mode: ${first.learningItem.mode}")
    println("New: ${first.isNew}")
    println()

    val firstResult = engine.review(
        ReviewCommand(
            reviewEventId = ReviewEventId("review-001"),
            learnerId = learnerId,
            learningItemId = first.learningItem.id,
            rating = ReviewRating.GOOD,
            reviewedAt = now,
            responseTime = TimeSpan.seconds(3)
        )
    )

    println("Reviewed: GOOD")
    println(
        "Next due in: " +
                "${firstResult.scheduledInterval.toDays()} days"
    )
    println()

    val second = engine.getNextLearningItem(
        learnerId = learnerId,
        now = now
    )

    requireNotNull(second)

    println("Next item: ${second.content.displayName}")
    println("Mode: ${second.learningItem.mode}")
    println("New: ${second.isNew}")
    println()

    engine.review(
        ReviewCommand(
            reviewEventId = ReviewEventId("review-002"),
            learnerId = learnerId,
            learningItemId = second.learningItem.id,
            rating = ReviewRating.EASY,
            reviewedAt = now,
            responseTime = TimeSpan.seconds(2)
        )
    )

    val noItemDue = engine.getNextLearningItem(
        learnerId = learnerId,
        now = now
    )

    println("Item available now: ${noItemDue != null}")

    now = firstResult.memoryState.dueAt

    val dueLater = engine.getNextLearningItem(
        learnerId = learnerId,
        now = now
    )

    println()
    println("After time advances:")
    println("Next item: ${dueLater?.content?.displayName}")
    println("New: ${dueLater?.isNew}")
}

private fun registerSampleData(
    engine: vn.loi.learning.application.LearningEngine
) {
    val firstContent = Content(
        id = ContentId("sentence-001"),
        type = ContentType.SENTENCE,
        text = ContentText(
            primaryText = "She opened the door.",
            translatedText = "Cô ấy mở cửa."
        ),
        metadata = ContentMetadata(
            title = "She opened the door"
        )
    )

    val secondContent = Content(
        id = ContentId("sentence-002"),
        type = ContentType.SENTENCE,
        text = ContentText(
            primaryText = "The patient received radiation therapy.",
            translatedText = "Bệnh nhân đã được xạ trị."
        ),
        metadata = ContentMetadata(
            title = "Radiation therapy"
        )
    )

    engine.registerContent(firstContent)
    engine.registerContent(secondContent)

    engine.registerLearningItem(
        LearningItem(
            id = LearningItemId(
                "sentence-001-listening"
            ),
            contentId = firstContent.id,
            mode = LearningMode.LISTENING_RECOGNITION
        )
    )

    engine.registerLearningItem(
        LearningItem(
            id = LearningItemId(
                "sentence-002-meaning"
            ),
            contentId = secondContent.id,
            mode = LearningMode.MEANING_RECOGNITION
        )
    )
}