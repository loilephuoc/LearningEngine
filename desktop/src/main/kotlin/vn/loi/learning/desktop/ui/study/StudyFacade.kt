package vn.loi.learning.desktop.ui.study

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import vn.loi.learning.application.session.NextSessionItem
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.LearningApplicationContext

class StudyFacade(
    private val applicationContext:
    LearningApplicationContext
) {

    private val learnerId =
        LearnerId("default-learner")

    private var activeSessionId:
            SessionId? = null

    private var currentItem:
            NextSessionItem? = null

    private var presentedAtMillis:
            Long? = null

    private var latestSession:
            StudySession? = null

    private var includedContentIds:
            Set<ContentId> = emptySet()

    private var studyTitle:
            String = DEFAULT_STUDY_TITLE

    private var lessonStudy:
            Boolean = false

    private var totalItems:
            Int = 0

    private var latestSchedulerFeedback:
            StudySchedulerFeedback? = null

    fun load(): StudyUiState =
        currentItem?.let { nextItem ->
            toUiState(
                nextSessionItem = nextItem,
                answerRevealed = false
            )
        } ?: createIdleUiState()

    fun startStudy(): StudyUiState {
        includedContentIds =
            emptySet()

        studyTitle =
            DEFAULT_STUDY_TITLE

        lessonStudy =
            false

        totalItems =
            0

        latestSchedulerFeedback =
            null

        return startSession()
    }

    fun startLessonStudy(
        contentId: String
    ): StudyUiState {
        val resolvedContentId =
            ContentId(contentId)

        val content =
            requireNotNull(
                applicationContext
                    .engine
                    .getContent(
                        resolvedContentId
                    )
            ) {
                "Content $contentId does not exist."
            }

        includedContentIds =
            setOf(
                resolvedContentId
            )

        studyTitle =
            content.displayName

        lessonStudy =
            true

        latestSchedulerFeedback =
            null

        totalItems =
            applicationContext
                .engine
                .getLearningItemsByContentId(
                    resolvedContentId
                )
                .count {
                    it.isEnabled
                }

        return startSession()
    }

    private fun startSession(): StudyUiState {
        val nowMillis =
            System.currentTimeMillis()

        val now =
            Moment(nowMillis)

        val sessionId =
            SessionId(
                UUID.randomUUID()
                    .toString()
            )

        latestSession =
            applicationContext
                .engine
                .startSession(
                    StartStudySessionCommand(
                        sessionId = sessionId,
                        learnerId = learnerId,
                        startedAt = now
                    )
                )

        activeSessionId =
            sessionId

        return loadNextItem(
            sessionId = sessionId,
            now = now,
            nowMillis = nowMillis,
            emptyMessage =
                if (lessonStudy) {
                    "No learning items available " +
                            "for this lesson."
                } else {
                    "No learning items available."
                }
        )
    }

    fun revealAnswer(): StudyUiState {
        val nextItem =
            currentItem
                ?: return createIdleUiState(
                    message =
                        "No active learning item."
                )

        return toUiState(
            nextSessionItem = nextItem,
            answerRevealed = true
        )
    }

    fun review(
        rating: ReviewRating
    ): StudyUiState {
        val sessionId =
            activeSessionId
                ?: return createIdleUiState(
                    message =
                        "No active study session."
                )

        val nextItem =
            currentItem
                ?: return createIdleUiState(
                    message =
                        "No active learning item."
                )

        val nowMillis =
            System.currentTimeMillis()

        val reviewedAt =
            Moment(nowMillis)

        val responseTime =
            presentedAtMillis?.let {
                    presentedAt ->
                TimeSpan(
                    (
                            nowMillis -
                                    presentedAt
                            ).coerceAtLeast(0L)
                )
            }

        val reviewSessionItemResult =
            applicationContext
                .engine
                .reviewSessionItem(
                    ReviewSessionItemCommand(
                        sessionId = sessionId,
                        reviewEventId =
                            ReviewEventId(
                                UUID.randomUUID()
                                    .toString()
                            ),
                        learningItemId =
                            nextItem
                                .item
                                .learningItem
                                .id,
                        rating = rating,
                        reviewedAt = reviewedAt,
                        responseTime =
                            responseTime
                    )
                )

        latestSession =
            reviewSessionItemResult
                .session

        val reviewResult =
            reviewSessionItemResult
                .reviewResult

        val previousState =
            reviewResult
                .reviewEvent
                .stateBefore

        val nextState =
            reviewResult
                .reviewEvent
                .stateAfter

        latestSchedulerFeedback =
            StudySchedulerFeedback(
                rating = rating.name,
                stageTransition =
                    "${previousState.stage.name} → " +
                            nextState.stage.name,
                scheduledInterval =
                    formatDuration(
                        reviewResult
                            .scheduledInterval
                            .millis
                    ),
                nextReviewAt =
                    formatMoment(
                        nextState
                            .dueAt
                            .epochMillis
                    ),
                difficultyBefore =
                    formatDecimal(
                        previousState
                            .difficulty
                    ),
                difficultyAfter =
                    formatDecimal(
                        nextState
                            .difficulty
                    ),
                stabilityBefore =
                    formatDays(
                        previousState
                            .stabilityDays
                    ),
                stabilityAfter =
                    formatDays(
                        nextState
                            .stabilityDays
                    ),
                reviewCount =
                    nextState
                        .reviewCount,
                lapseCount =
                    nextState
                        .lapseCount
            )

        return loadNextItem(
            sessionId = sessionId,
            now = reviewedAt,
            nowMillis = nowMillis,
            emptyMessage =
                "Study session completed."
        )
    }

    private fun loadNextItem(
        sessionId: SessionId,
        now: Moment,
        nowMillis: Long,
        emptyMessage: String
    ): StudyUiState {
        currentItem =
            applicationContext
                .engine
                .getNextSessionItem(
                    sessionId = sessionId,
                    now = now,
                    includedContentIds =
                        includedContentIds
                )

        val nextItem =
            currentItem

        if (nextItem == null) {
            latestSession =
                applicationContext
                    .engine
                    .finishSession(
                        sessionId = sessionId,
                        finishedAt = now
                    )

            activeSessionId =
                null

            presentedAtMillis =
                null

            includedContentIds =
                emptySet()

            val completedSession =
                requireNotNull(
                    latestSession
                )

            return StudyUiState(
                sessionStarted = true,
                studyTitle = studyTitle,
                isLessonStudy = lessonStudy,
                reviewedCount =
                    completedSession
                        .totalReviews,
                newItemsReviewed =
                    completedSession
                        .newItemsReviewed,
                reviewItemsReviewed =
                    completedSession
                        .reviewItemsReviewed,
                totalItems =
                    totalItems,
                currentItemPosition =
                    completedSession
                        .totalReviews
                        .coerceAtMost(
                            totalItems
                        ),
                sessionCompleted = true,
                schedulerFeedback =
                    latestSchedulerFeedback,
                message = emptyMessage
            )
        }

        presentedAtMillis =
            nowMillis

        return toUiState(
            nextSessionItem = nextItem,
            answerRevealed = false
        )
    }

    private fun toUiState(
        nextSessionItem: NextSessionItem,
        answerRevealed: Boolean
    ): StudyUiState {
        val item =
            nextSessionItem.item

        val reviewedCount =
            nextSessionItem
                .session
                .totalReviews

        val currentItemPosition =
            if (totalItems > 0) {
                (reviewedCount + 1)
                    .coerceAtMost(
                        totalItems
                    )
            } else {
                reviewedCount + 1
            }

        return StudyUiState(
            hasActiveSession =
                activeSessionId != null,
            sessionStarted = true,
            studyTitle = studyTitle,
            isLessonStudy = lessonStudy,
            contentText =
                item.content
                    .text
                    .primaryText,
            translationText =
                item.content
                    .text
                    .translatedText
                    ?: "No translation available.",
            canRevealAnswer =
                !answerRevealed,
            canReview =
                answerRevealed,
            reviewedCount =
                reviewedCount,
            newItemsReviewed =
                nextSessionItem
                    .session
                    .newItemsReviewed,
            reviewItemsReviewed =
                nextSessionItem
                    .session
                    .reviewItemsReviewed,
            totalItems =
                totalItems,
            currentItemPosition =
                currentItemPosition,
            schedulerFeedback =
                latestSchedulerFeedback,
            message =
                if (item.isNew) {
                    "New learning item"
                } else {
                    "Review learning item"
                }
        )
    }

    private fun createIdleUiState(
        message: String =
            "Press Start Study"
    ): StudyUiState =
        StudyUiState(
            studyTitle = studyTitle,
            isLessonStudy = lessonStudy,
            totalItems = totalItems,
            schedulerFeedback =
                latestSchedulerFeedback,
            message = message
        )

    private fun formatDuration(
        millis: Long
    ): String {
        val totalMinutes =
            millis / MILLIS_PER_MINUTE

        if (totalMinutes < MINUTES_PER_HOUR) {
            return "${totalMinutes.coerceAtLeast(1L)} min"
        }

        val totalHours =
            millis / MILLIS_PER_HOUR

        if (totalHours < HOURS_PER_DAY) {
            return "$totalHours h"
        }

        val days =
            millis.toDouble() /
                    MILLIS_PER_DAY.toDouble()

        return formatDays(days)
    }

    private fun formatMoment(
        epochMillis: Long
    ): String =
        DATE_TIME_FORMATTER
            .format(
                Instant
                    .ofEpochMilli(epochMillis)
                    .atZone(
                        ZoneId.systemDefault()
                    )
            )

    private fun formatDecimal(
        value: Double
    ): String =
        String.format(
            Locale.US,
            "%.2f",
            value
        )

    private fun formatDays(
        value: Double
    ): String =
        "${formatDecimal(value)} d"

    private companion object {

        const val DEFAULT_STUDY_TITLE =
            "All learning items"

        const val MILLIS_PER_MINUTE =
            60_000L

        const val MILLIS_PER_HOUR =
            3_600_000L

        const val MILLIS_PER_DAY =
            86_400_000L

        const val MINUTES_PER_HOUR =
            60L

        const val HOURS_PER_DAY =
            24L

        val DATE_TIME_FORMATTER:
                DateTimeFormatter =
            DateTimeFormatter.ofPattern(
                "yyyy-MM-dd HH:mm"
            )
    }
}