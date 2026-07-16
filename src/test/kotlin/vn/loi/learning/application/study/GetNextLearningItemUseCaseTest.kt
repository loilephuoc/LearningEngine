package vn.loi.learning

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
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
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningEngineFactory

fun main() {
    val engine = LearningEngineFactory.createInMemory()
    val learnerId = LearnerId("loi")
    val sessionId = SessionId("session-001")

    registerSampleData(engine)

    var now = Moment(1_000_000L)

    val session = engine.startSession(
        StartStudySessionCommand(
            sessionId = sessionId,
            learnerId = learnerId,
            startedAt = now,
            policy = SessionPolicy(
                newItemLimit = 2,
                reviewItemLimit = 10,
                allowRepeatInSameSession = false
            )
        )
    )

    println("=== LEARNING ENGINE 2.0 ===")
    println("Session: ${session.id}")
    println("Status: ${session.status}")
    println()

    var reviewNumber = 1

    while (true) {
        val next = engine.getNextSessionItem(
            sessionId = sessionId,
            now = now
        ) ?: break

        println("Item #$reviewNumber")
        println("Content: ${next.item.content.displayName}")
        println("Mode: ${next.item.learningItem.mode}")
        println("New item: ${next.item.isNew}")

        val rating = when (reviewNumber) {
            1 -> ReviewRating.GOOD
            else -> ReviewRating.EASY
        }

        val result = engine.reviewSessionItem(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId = ReviewEventId("review-$reviewNumber"),
                learningItemId = next.item.learningItem.id,
                rating = rating,
                reviewedAt = now,
                responseTime = TimeSpan.seconds(3)
            )
        )

        println("Rating: $rating")
        println(
            "Difficulty: %.2f".format(
                result.reviewResult.memoryState.difficulty
            )
        )
        println(
            "Stability: %.2f days".format(
                result.reviewResult.memoryState.stabilityDays
            )
        )
        println(
            "Interval: %.2f days".format(
                result.reviewResult.scheduledInterval.toDays()
            )
        )
        println(
            "Session progress: " +
                    "${result.session.totalReviews} reviews, " +
                    "${result.session.newItemsReviewed} new"
        )
        println()

        reviewNumber++
    }

    val finishedSession = engine.finishSession(
        sessionId = sessionId,
        finishedAt = now + TimeSpan.minutes(5)
    )

    println("=== SESSION FINISHED ===")
    println("Status: ${finishedSession.status}")
    println("Total reviews: ${finishedSession.totalReviews}")
    println("New items: ${finishedSession.newItemsReviewed}")
    println("Due reviews: ${finishedSession.reviewItemsReviewed}")
    println("Finished at: ${finishedSession.finishedAt}")
}

private fun registerSampleData(engine: LearningEngine) {
    registerSentence(
        engine = engine,
        number = 1,
        english = "She opened the door.",
        vietnamese = "Cô ấy mở cửa.",
        title = "She opened the door",
        mode = LearningMode.LISTENING_RECOGNITION
    )

    registerSentence(
        engine = engine,
        number = 2,
        english = "The patient received radiation therapy.",
        vietnamese = "Bệnh nhân đã được xạ trị.",
        title = "Radiation therapy",
        mode = LearningMode.MEANING_RECOGNITION
    )

    registerSentence(
        engine = engine,
        number = 3,
        english = "Learning requires consistent practice.",
        vietnamese = "Việc học đòi hỏi luyện tập đều đặn.",
        title = "Consistent practice",
        mode = LearningMode.MEANING_RECALL
    )
}

private fun registerSentence(
    engine: LearningEngine,
    number: Int,
    english: String,
    vietnamese: String,
    title: String,
    mode: LearningMode
) {
    val contentId = ContentId("sentence-$number")

    engine.registerContent(
        Content(
            id = contentId,
            type = ContentType.SENTENCE,
            text = ContentText(
                primaryText = english,
                translatedText = vietnamese
            ),
            metadata = ContentMetadata(
                title = title,
                group = "Demo",
                section = "Section 1",
                lesson = "Lesson 1"
            )
        )
    )

    engine.registerLearningItem(
        LearningItem(
            id = LearningItemId("sentence-$number-${mode.name.lowercase()}"),
            contentId = contentId,
            mode = mode
        )
    )
}