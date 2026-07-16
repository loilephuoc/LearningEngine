package vn.loi.learning

import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.infrastructure.LearningEngineFactory

fun main() {
    val engine = LearningEngineFactory.createInMemory()

    val learnerId = LearnerId("loi")
    val learningItemId = LearningItemId("sentence-001-listening")

    var currentTime = Moment(1_000_000L)

    println("=== LEARNING ENGINE 2.0 ===")
    println()

    val firstResult = engine.review(
        ReviewCommand(
            reviewEventId = ReviewEventId("review-001"),
            learnerId = learnerId,
            learningItemId = learningItemId,
            rating = ReviewRating.GOOD,
            reviewedAt = currentTime,
            responseTime = TimeSpan.seconds(3)
        )
    )

    printResult(
        number = 1,
        rating = ReviewRating.GOOD,
        result = firstResult
    )

    currentTime = firstResult.memoryState.dueAt

    val secondResult = engine.review(
        ReviewCommand(
            reviewEventId = ReviewEventId("review-002"),
            learnerId = learnerId,
            learningItemId = learningItemId,
            rating = ReviewRating.AGAIN,
            reviewedAt = currentTime,
            responseTime = TimeSpan.seconds(8)
        )
    )

    printResult(
        number = 2,
        rating = ReviewRating.AGAIN,
        result = secondResult
    )

    currentTime = secondResult.memoryState.dueAt

    val thirdResult = engine.review(
        ReviewCommand(
            reviewEventId = ReviewEventId("review-003"),
            learnerId = learnerId,
            learningItemId = learningItemId,
            rating = ReviewRating.GOOD,
            reviewedAt = currentTime,
            responseTime = TimeSpan.seconds(4)
        )
    )

    printResult(
        number = 3,
        rating = ReviewRating.GOOD,
        result = thirdResult
    )

    val history = engine.getReviewHistory(
        learnerId = learnerId,
        learningItemId = learningItemId
    )

    println("=== REVIEW HISTORY ===")

    history.forEachIndexed { index, event ->
        println(
            "${index + 1}. " +
                    "rating=${event.rating}, " +
                    "reviewedAt=${event.reviewedAt.epochMillis}, " +
                    "stage=${event.stateAfter.stage}, " +
                    "dueAt=${event.stateAfter.dueAt.epochMillis}"
        )
    }
}

private fun printResult(
    number: Int,
    rating: ReviewRating,
    result: vn.loi.learning.application.review.ReviewResult
) {
    val state = result.memoryState

    println("Review #$number")
    println("Rating: $rating")
    println("Stage: ${state.stage}")
    println("Difficulty: ${"%.2f".format(state.difficulty)}")
    println("Stability: ${"%.2f".format(state.stabilityDays)} days")
    println("Interval: ${result.scheduledInterval}")
    println("Due at: ${state.dueAt.epochMillis}")
    println("Review count: ${state.reviewCount}")
    println("Lapse count: ${state.lapseCount}")
    println()
}