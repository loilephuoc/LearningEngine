package vn.loi.learning

import java.nio.file.Path
import vn.loi.learning.adapter.jvm.LegacyJsonFileImportService
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.importing.LegacyJsonImportService
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningEngineFactory

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        return
    }

    val jsonPath = Path.of(args.joinToString(" "))
    val engine = LearningEngineFactory.createInMemory()

    val importResult = importLegacyJson(
        engine = engine,
        jsonPath = jsonPath
    )

    println("=== IMPORT RESULT ===")
    println("File: $jsonPath")
    println("Contents: ${importResult.registeredContentCount}")
    println("Learning items: ${importResult.registeredLearningItemCount}")
    println("Skipped records: ${importResult.skippedRecordCount}")
    println()

    if (importResult.registeredLearningItemCount == 0) {
        println("No learning items were imported.")
        return
    }

    runDemoSession(engine)
}

private fun importLegacyJson(
    engine: LearningEngine,
    jsonPath: Path
): vn.loi.learning.application.importing.ImportContentResult {
    val textImportService = LegacyJsonImportService(engine)

    val fileImportService = LegacyJsonFileImportService(
        importService = textImportService
    )

    return fileImportService.import(jsonPath)
}

private fun runDemoSession(engine: LearningEngine) {
    val learnerId = LearnerId("loi")
    val sessionId = SessionId("json-import-session")
    val now = Moment(System.currentTimeMillis())

    engine.startSession(
        StartStudySessionCommand(
            sessionId = sessionId,
            learnerId = learnerId,
            startedAt = now,
            policy = SessionPolicy(
                newItemLimit = 10,
                reviewItemLimit = 100,
                allowRepeatInSameSession = false
            )
        )
    )

    println("=== STUDY SESSION ===")

    var reviewNumber = 1

    while (reviewNumber <= MAX_DEMO_REVIEWS) {
        val next = engine.getNextSessionItem(
            sessionId = sessionId,
            now = now
        ) ?: break

        println()
        println("Item #$reviewNumber")
        println("Content ID: ${next.item.content.id}")
        println("Type: ${next.item.content.type}")
        println("Text: ${next.item.content.text.primaryText}")
        println(
            "Translation: " +
                    (next.item.content.text.translatedText ?: "-")
        )
        println("Mode: ${next.item.learningItem.mode}")
        println("New: ${next.item.isNew}")
        println(
            "Audio: " +
                    (next.item.content.media.primaryAudio ?: "-")
        )
        println(
            "Group: " +
                    (next.item.content.metadata.group ?: "-")
        )
        println(
            "Section: " +
                    (next.item.content.metadata.section ?: "-")
        )
        println(
            "Lesson: " +
                    (next.item.content.metadata.lesson ?: "-")
        )

        /*
         * Demo tự đánh giá GOOD để kiểm tra toàn bộ pipeline.
         * UI thật sau này sẽ nhận rating từ người dùng.
         */
        val result = engine.reviewSessionItem(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId =
                    ReviewEventId("json-review-$reviewNumber"),
                learningItemId = next.item.learningItem.id,
                rating = ReviewRating.GOOD,
                reviewedAt = now,
                responseTime = TimeSpan.seconds(3)
            )
        )

        println("Rating: GOOD")
        println(
            "Next interval: %.2f days".format(
                result.reviewResult.scheduledInterval.toDays()
            )
        )

        reviewNumber++
    }

    val finishedSession = engine.finishSession(
        sessionId = sessionId,
        finishedAt = now + TimeSpan.minutes(5)
    )

    println()
    println("=== SESSION FINISHED ===")
    println("Total reviews: ${finishedSession.totalReviews}")
    println("New items: ${finishedSession.newItemsReviewed}")
    println("Due reviews: ${finishedSession.reviewItemsReviewed}")
}

private fun printUsage() {
    println("Learning Engine 2.0")
    println()
    println("Usage:")
    println(
        """  .\gradlew.bat run --args='"F:\path\file.json"'"""
    )
}

private const val MAX_DEMO_REVIEWS = 5