package vn.loi.learning.android.reminder

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.LearningApplicationContext

data class AndroidFsrsInspectorUiModel(
    val hasFsrsData: Boolean,
    val stage: String,
    val due: String,
    val lastReviewed: String,
    val reviewCount: Int,
    val lapseCount: Int,
    val difficulty: String,
    val stability: String,
    val history: List<AndroidReviewHistoryRow>
) {
    val recentHistory: List<AndroidReviewHistoryRow> get() = history.take(RECENT_HISTORY_LIMIT)
    val hasMoreHistory: Boolean get() = history.size > RECENT_HISTORY_LIMIT

    companion object {
        const val RECENT_HISTORY_LIMIT = 10
        fun noFsrsData() = AndroidFsrsInspectorUiModel(false, "New", "Not scheduled", "Never", 0, 0, "—", "—", emptyList())
    }
}

data class AndroidReviewHistoryRow(
    val rating: ReviewRating,
    val reviewedAt: String,
    val source: String,
    val stageTransition: String,
    val stabilityTransition: String,
    val difficultyTransition: String
)

class AndroidReminderReviewFsrsInspectorQuery(
    private val context: LearningApplicationContext,
    private val learnerId: LearnerId = LearnerId("default-learner"),
    private val now: () -> Long = System::currentTimeMillis,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val locale: Locale = Locale.getDefault()
) {
    fun query(contentId: String): AndroidFsrsInspectorUiModel {
        val item = context.learningItemRepository?.findByContentId(ContentId(contentId)).orEmpty()
            .filter { it.isEnabled }
            .firstOrNull { it.mode == LearningMode.MEANING_RECOGNITION }
            ?: return AndroidFsrsInspectorUiModel.noFsrsData()
        val state = context.memoryStateRepository?.find(learnerId, item.id)
        val formatter = AndroidFsrsInspectorFormatter(now(), zoneId, locale)
        val history = context.reviewEventRepository?.findAll(learnerId, item.id).orEmpty()
            .asReversed()
            .map { event ->
                AndroidReviewHistoryRow(
                    rating = event.rating,
                    reviewedAt = formatter.formatCalendarTime(event.reviewedAt),
                    source = sourceLabel(event.source),
                    stageTransition = "${stageLabel(event.stateBefore.stage)} → ${stageLabel(event.stateAfter.stage)}",
                    stabilityTransition = "${formatStability(event.stateBefore.stabilityDays)} → ${formatStability(event.stateAfter.stabilityDays)}",
                    difficultyTransition = "${formatDifficultyValue(event.stateBefore.difficulty)} → ${formatDifficultyValue(event.stateAfter.difficulty)}"
                )
            }
        return if (state == null) {
            AndroidFsrsInspectorUiModel.noFsrsData().copy(hasFsrsData = true, history = history)
        } else {
            AndroidFsrsInspectorUiModel(
                hasFsrsData = true,
                stage = stageLabel(state.stage),
                due = formatter.formatDue(state.dueAt),
                lastReviewed = state.lastReviewedAt?.let(formatter::formatCalendarTime) ?: "Never",
                reviewCount = state.reviewCount,
                lapseCount = state.lapseCount,
                difficulty = "${formatDifficultyValue(state.difficulty)} / 10",
                stability = formatStability(state.stabilityDays),
                history = history
            )
        }
    }

    companion object {
        fun stageLabel(stage: LearningStage): String = stage.name.lowercase().replaceFirstChar(Char::uppercase)
        fun sourceLabel(source: RatingSource): String = when (source) {
            RatingSource.STANDARD_REVIEW -> "Study"
            RatingSource.MANUAL_USER -> "Manual rating"
            else -> source.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
        }
        fun formatStability(days: Double): String = when {
            days < 1.0 -> "%.1f hours".format(Locale.US, days * 24.0)
            days < 30.0 -> "%.1f days".format(Locale.US, days)
            else -> "%.1f months".format(Locale.US, days / 30.0)
        }
        fun formatDifficultyValue(value: Double): String = "%.2f".format(Locale.US, value)
    }
}

class AndroidFsrsInspectorFormatter(
    nowMillis: Long,
    private val zoneId: ZoneId,
    locale: Locale
) {
    private val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", locale)

    fun formatDue(moment: Moment): String {
        val value = Instant.ofEpochMilli(moment.epochMillis).atZone(zoneId)
        val days = ChronoUnit.DAYS.between(now.toLocalDate(), value.toLocalDate())
        if (!value.toInstant().isAfter(now.toInstant())) {
            val overdueDays = ChronoUnit.DAYS.between(value.toLocalDate(), now.toLocalDate())
            return if (overdueDays == 0L) "Due now" else "Overdue by $overdueDays ${if (overdueDays == 1L) "day" else "days"}"
        }
        return when (days) {
            0L -> "Today, ${value.format(timeFormatter)}"
            1L -> "Tomorrow, ${value.format(timeFormatter)}"
            in 2L..6L -> "In $days days"
            else -> value.format(dateTimeFormatter)
        }
    }

    fun formatCalendarTime(moment: Moment): String {
        val value = Instant.ofEpochMilli(moment.epochMillis).atZone(zoneId)
        return when (ChronoUnit.DAYS.between(value.toLocalDate(), now.toLocalDate())) {
            0L -> "Today, ${value.format(timeFormatter)}"
            1L -> "Yesterday, ${value.format(timeFormatter)}"
            else -> value.format(dateTimeFormatter)
        }
    }
}
