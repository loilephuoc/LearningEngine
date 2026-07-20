package vn.loi.learning

import java.nio.file.Path
import vn.loi.learning.adapter.jvm.DashboardConsolePrinter
import vn.loi.learning.adapter.jvm.LegacyJsonFileImportService
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.importing.ImportContentResult
import vn.loi.learning.application.importing.LegacyJsonImportService
import vn.loi.learning.application.learningdashboard.LearningDashboardQuery
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        return
    }

    val jsonPath =
        Path.of(
            args.joinToString(" ")
        )

    val application =
        LearningApplicationFactory.createInMemory()

    val importResult =
        importLegacyJson(
            engine = application.engine,
            jsonPath = jsonPath
        )

    printImportResult(
        jsonPath = jsonPath,
        result = importResult
    )

    if (importResult.registeredLearningItemCount == 0) {
        println("No learning items were imported.")
        return
    }

    runDemoSession(
        application = application
    )
}

private fun importLegacyJson(
    engine: LearningEngine,
    jsonPath: Path
): ImportContentResult {
    val textImportService =
        LegacyJsonImportService(
            engine = engine
        )

    val fileImportService =
        LegacyJsonFileImportService(
            importService = textImportService
        )

    return fileImportService.import(
        path = jsonPath
    )
}

private fun printImportResult(
    jsonPath: Path,
    result: ImportContentResult
) {
    println("=== IMPORT RESULT ===")
    println("File: $jsonPath")
    println(
        "Contents: ${result.registeredContentCount}"
    )
    println(
        "Learning items: " +
                result.registeredLearningItemCount
    )
    println(
        "Skipped records: " +
                result.skippedRecordCount
    )
    println()
}

private fun runDemoSession(
    application: LearningApplicationContext
) {
    val engine =
        application.engine

    val learnerId =
        LearnerId("loi")

    val sessionId =
        SessionId("fsrs-integration-session")

    val sessionStartedAt =
        Moment(
            System.currentTimeMillis()
        )

    val startedSession =
        engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learnerId,
                startedAt = sessionStartedAt,
                policy =
                    SessionPolicy(
                        newItemLimit =
                            MAX_DEMO_REVIEWS,
                        reviewItemLimit = 100,
                        allowRepeatInSameSession =
                            false
                    )
            )
        )

    println("=== SESSION STARTED ===")
    println("Session ID: ${startedSession.id}")
    println("Learner ID: ${startedSession.learnerId}")
    println("Status: ${startedSession.status}")
    println()

    var reviewNumber = 1

    while (reviewNumber <= MAX_DEMO_REVIEWS) {
        val reviewedAt =
            sessionStartedAt +
                    TimeSpan.seconds(
                        reviewNumber.toLong()
                    )

        val next =
            engine.getNextSessionItem(
                sessionId = sessionId,
                now = reviewedAt
            ) ?: break

        println("=== ITEM #$reviewNumber ===")
        println(
            "Learning item ID: " +
                    next.item.learningItem.id
        )
        println(
            "Content ID: " +
                    next.item.content.id
        )
        println(
            "Type: " +
                    next.item.content.type
        )
        println(
            "Text: " +
                    next.item.content.text.primaryText
        )
        println(
            "Translation: " +
                    (
                            next.item.content.text
                                .translatedText
                                ?: "-"
                            )
        )
        println(
            "Mode: " +
                    next.item.learningItem.mode
        )
        println("New: ${next.item.isNew}")
        println(
            "Audio: " +
                    (
                            next.item.content.media
                                .primaryAudio
                                ?: "-"
                            )
        )
        println(
            "Group: " +
                    (
                            next.item.content.metadata.group
                                ?: "-"
                            )
        )
        println(
            "Section: " +
                    (
                            next.item.content.metadata.section
                                ?: "-"
                            )
        )
        println(
            "Lesson: " +
                    (
                            next.item.content.metadata.lesson
                                ?: "-"
                            )
        )

        val result =
            engine.reviewSessionItem(
                ReviewSessionItemCommand(
                    sessionId = sessionId,
                    reviewEventId =
                        ReviewEventId(
                            "fsrs-review-$reviewNumber"
                        ),
                    learningItemId =
                        next.item.learningItem.id,
                    rating =
                        ReviewRating.GOOD,
                    reviewedAt = reviewedAt,
                    responseTime =
                        TimeSpan.seconds(3)
                )
            )

        val returnedState =
            result.reviewResult.memoryState

        val persistedState =
            requireNotNull(
                engine.getMemoryState(
                    learnerId = learnerId,
                    learningItemId =
                        next.item.learningItem.id
                )
            ) {
                "MemoryState was not saved after review."
            }

        require(persistedState == returnedState) {
            "Persisted MemoryState does not match " +
                    "the Scheduler result."
        }

        val reviewHistory =
            engine.getReviewHistory(
                learnerId = learnerId,
                learningItemId =
                    next.item.learningItem.id
            )

        require(reviewHistory.isNotEmpty()) {
            "ReviewEvent was not saved."
        }

        println()
        println("=== FSRS RESULT ===")
        println("Rating: ${ReviewRating.GOOD}")

        printMemoryState(
            title = "State before",
            state =
                result.reviewResult.reviewEvent
                    .stateBefore
        )

        printMemoryState(
            title = "State after",
            state = persistedState
        )

        println(
            "Scheduled interval: %.6f days".format(
                result.reviewResult
                    .scheduledInterval
                    .toDays()
            )
        )

        println(
            "Review history entries: " +
                    reviewHistory.size
        )

        println("MemoryState persisted: YES")
        println()

        reviewNumber++
    }

    val finishTime =
        sessionStartedAt +
                TimeSpan.minutes(5)

    val finishedSession =
        engine.finishSession(
            sessionId = sessionId,
            finishedAt = finishTime
        )

    val persistedSession =
        requireNotNull(
            engine.getSession(
                sessionId = sessionId
            )
        ) {
            "Finished session was not saved."
        }

    require(persistedSession == finishedSession) {
        "Persisted StudySession does not match " +
                "the finished session."
    }

    println("=== SESSION FINISHED ===")
    println("Session ID: ${finishedSession.id}")
    println("Status: ${finishedSession.status}")

    println(
        "Total reviews: " +
                finishedSession.totalReviews
    )

    println(
        "New items: " +
                finishedSession.newItemsReviewed
    )

    println(
        "Due reviews: " +
                finishedSession.reviewItemsReviewed
    )

    println("Finished session persisted: YES")
    println()

    printDashboard(
        application = application,
        learnerId = learnerId,
        sessionStartedAt = sessionStartedAt,
        dashboardAt = finishTime
    )

    println()
    println("=== END-TO-END RESULT ===")

    println(
        "Import -> Start Session -> Review via FSRS -> " +
                "MemoryState saved -> ReviewEvent saved -> " +
                "Finish Session -> Query Dashboard"
    )

    println("STATUS: SUCCESS")
}

private fun printDashboard(
    application: LearningApplicationContext,
    learnerId: LearnerId,
    sessionStartedAt: Moment,
    dashboardAt: Moment
) {
    val oneDay =
        TimeSpan.seconds(
            SECONDS_PER_DAY
        )

    val snapshot =
        application.dashboard.query(
            LearningDashboardQuery(
                learnerId = learnerId,
                activityFrom =
                    sessionStartedAt,
                activityUntil =
                    dashboardAt +
                            TimeSpan.seconds(1),
                at = dashboardAt,
                forecastWindowEnds =
                    listOf(
                        dashboardAt + oneDay,
                        dashboardAt +
                                TimeSpan.seconds(
                                    SECONDS_PER_DAY * 3
                                ),
                        dashboardAt +
                                TimeSpan.seconds(
                                    SECONDS_PER_DAY * 7
                                )
                    )
            )
        )

    DashboardConsolePrinter().print(
        snapshot = snapshot
    )
}

private fun printMemoryState(
    title: String,
    state: MemoryState
) {
    println("$title:")
    println("  Stage: ${state.stage}")

    println(
        "  Difficulty: %.6f".format(
            state.difficulty
        )
    )

    println(
        "  Stability: %.6f days".format(
            state.stabilityDays
        )
    )

    println(
        "  Review count: ${state.reviewCount}"
    )

    println(
        "  Lapse count: ${state.lapseCount}"
    )

    println(
        "  Last reviewed at: ${state.lastReviewedAt}"
    )

    println(
        "  Due at: ${state.dueAt}"
    )
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

private const val SECONDS_PER_DAY =
    86_400L