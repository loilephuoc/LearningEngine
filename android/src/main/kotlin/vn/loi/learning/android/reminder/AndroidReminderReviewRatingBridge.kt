package vn.loi.learning.android.reminder

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToLong
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.scheduling.FsrsScheduler
import vn.loi.learning.domain.study.scheduling.Scheduler
import vn.loi.learning.infrastructure.LearningApplicationContext

/**
 * Submission state machine for quick review rating interactions.
 */
enum class QuickReviewSubmissionState {
    IDLE,
    SUBMITTING,
    SUCCESS,
    ERROR
}

/**
 * Typed result representing the outcome of a quick review rating submission.
 */
sealed interface QuickReviewRatingResult {
    data class Success(
        val learningItemId: String,
        val rating: ReviewRating,
        val scheduledInterval: TimeSpan,
        val memoryState: MemoryState
    ) : QuickReviewRatingResult

    data object NotFound : QuickReviewRatingResult
    data object NotReviewable : QuickReviewRatingResult
    data object AlreadySubmitting : QuickReviewRatingResult
    data class Failure(val cause: Throwable) : QuickReviewRatingResult
}

/**
 * Non-persisted preview for a candidate FSRS rating choice.
 */
data class QuickReviewRatingPreview(
    val rating: ReviewRating,
    val scheduledInterval: TimeSpan,
    val formattedInterval: String
)

/**
 * Application bridge connecting Reminder Full Review surfaces to canonical Core FSRS rating transactions.
 *
 * Invariants:
 * - Reuses [LearningEngine.review] with atomic [ReviewCommand] transaction.
 * - Zero direct mutation of FSRS fields (stability, difficulty, lapses, due, repetitions) from Android.
 * - Zero automatic StudySession creation or Study queue distortion.
 * - Fresh resolution of LearningItem and MemoryState directly from repositories on every rating and preview.
 * - Atomic submission guard preventing double-tap / concurrent rating mutations.
 * - Deterministic targeting: strictly resolves enabled [LearningMode.MEANING_RECOGNITION] without arbitrary fallback.
 */
class AndroidReminderReviewRatingBridge(
    private val context: LearningApplicationContext,
    private val learnerId: LearnerId = LearnerId("default-learner"),
    private val now: () -> Long = System::currentTimeMillis,
    private val scheduler: Scheduler = FsrsScheduler()
) {
    private val submitting = AtomicBoolean(false)

    /**
     * Resolves the authoritative [LearningItem] for a given [contentId].
     *
     * Strictly requires an enabled item with [LearningMode.MEANING_RECOGNITION].
     * Never falls back to arbitrary unrelated learning modes.
     */
    fun resolveLearningItem(
        contentId: String,
        intendedMode: LearningMode = LearningMode.MEANING_RECOGNITION
    ): LearningItem? {
        val cid = ContentId(contentId)
        val items = context.learningItemRepository?.findByContentId(cid).orEmpty()
        if (items.isEmpty()) return null
        val enabled = items.filter { it.isEnabled }
        if (enabled.isEmpty()) return null
        return enabled.firstOrNull { it.mode == intendedMode }
    }

    /**
     * Non-mutating preview of FSRS scheduling outcomes for all 4 ratings (AGAIN, HARD, GOOD, EASY).
     *
     * Performs ZERO database persistence and ZERO ReviewEvent creation.
     */
    fun previewRatings(
        contentId: String,
        intendedMode: LearningMode = LearningMode.MEANING_RECOGNITION
    ): Map<ReviewRating, QuickReviewRatingPreview>? {
        val item = resolveLearningItem(contentId, intendedMode) ?: return null
        val currentMemory = context.memoryStateRepository?.find(learnerId, item.id)
            ?: MemoryState.new(learnerId, item.id, Moment(now()))
        val momentNow = Moment(now())

        return ReviewRating.entries.associateWith { rating ->
            val decision = scheduler.schedule(currentMemory, rating, momentNow)
            QuickReviewRatingPreview(
                rating = rating,
                scheduledInterval = decision.scheduledInterval,
                formattedInterval = formatTimeSpan(decision.scheduledInterval)
            )
        }
    }

    /**
     * Executes the canonical Core review transaction for the target content item.
     */
    fun submitRating(
        contentId: String,
        rating: ReviewRating,
        intendedMode: LearningMode = LearningMode.MEANING_RECOGNITION
    ): QuickReviewRatingResult {
        if (!submitting.compareAndSet(false, true)) {
            return QuickReviewRatingResult.AlreadySubmitting
        }
        return try {
            val cid = ContentId(contentId)
            val content = context.contentRepository?.findById(cid)
            if (content == null) {
                return QuickReviewRatingResult.NotFound
            }

            val item = resolveLearningItem(contentId, intendedMode)
                ?: return QuickReviewRatingResult.NotReviewable

            val momentNow = Moment(now())
            val reviewCommand = ReviewCommand(
                reviewEventId = ReviewEventId(UUID.randomUUID().toString()),
                learnerId = learnerId,
                learningItemId = item.id,
                rating = rating,
                reviewedAt = momentNow,
                source = RatingSource.MANUAL_USER
            )

            val result = context.engine.review(reviewCommand)
            QuickReviewRatingResult.Success(
                learningItemId = item.id.value,
                rating = rating,
                scheduledInterval = result.scheduledInterval,
                memoryState = result.memoryState
            )
        } catch (e: Throwable) {
            QuickReviewRatingResult.Failure(e)
        } finally {
            submitting.set(false)
        }
    }

    companion object {
        /**
         * Formats a [TimeSpan] into standard compact FSRS badge text (e.g. "1m", "10m", "1d", "4d", "2mo").
         */
        fun formatTimeSpan(timeSpan: TimeSpan): String {
            val millis = timeSpan.millis
            val seconds = timeSpan.toSeconds()
            val minutes = timeSpan.toMinutes()
            val hours = timeSpan.toHours()
            val days = timeSpan.toDays().roundToLong()

            return when {
                millis < 60_000L -> if (seconds <= 0L) "1m" else "${seconds}s"
                minutes < 60L -> "${minutes}m"
                hours < 24L -> "${hours}h"
                days < 30L -> "${days}d"
                days < 365L -> "${(days / 30L).coerceAtLeast(1L)}mo"
                else -> "${(days / 365L).coerceAtLeast(1L)}y"
            }
        }
    }
}
