package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learninginsight.*
import vn.loi.learning.domain.study.evidence.RecommendationAction

data class LearningInsightStrings(
    val titles: Map<LearningInsightTitle, String>,
    val summaries: Map<LearningInsightSummary, String>,
    val actions: Map<RecommendationAction, String>,
    val metrics: Map<LearningInsightMetricKind, String>,
    val heading: String,
    val expand: String,
    val collapse: String
) {
    companion object {
        val ENGLISH = create(false)
        val VIETNAMESE = create(true)

        private fun create(vi: Boolean) = LearningInsightStrings(
            titles = LearningInsightTitle.entries.associateWith { title ->
                if (vi) when (title) {
                    LearningInsightTitle.PROMOTION_READY -> "Sẵn sàng thăng hạng"
                    LearningInsightTitle.PROMOTION_IN_PROGRESS -> "Đang tích lũy bằng chứng"
                    LearningInsightTitle.RECOVERY_NEEDED -> "Cần phục hồi"
                    LearningInsightTitle.DIFFICULT_CONTENT -> "Nội dung khó"
                    LearningInsightTitle.RETENTION_AT_RISK -> "Có nguy cơ quên"
                    LearningInsightTitle.KEEP_PRACTICING -> "Tiếp tục luyện tập"
                    LearningInsightTitle.READY_TO_PROGRESS -> "Sẵn sàng tiến bộ"
                    LearningInsightTitle.STABLE_LEARNING -> "Tiến trình ổn định"
                    LearningInsightTitle.MASTERY_REACHED -> "Đã làm chủ"
                    LearningInsightTitle.MORE_EVIDENCE_NEEDED -> "Cần thêm bằng chứng"
                    LearningInsightTitle.PRACTICE_DOES_NOT_CHANGE_RATING -> "Luyện tập không đổi đánh giá"
                    LearningInsightTitle.MANUAL_RATING_RECORDED -> "Đã ghi nhận đánh giá thủ công"
                } else title.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
            },
            summaries = LearningInsightSummary.entries.associateWith { summary ->
                if (vi) when (summary) {
                    LearningInsightSummary.PROMOTION_ELIGIBLE -> "Bằng chứng hiện tại đáp ứng điều kiện thăng hạng."
                    LearningInsightSummary.PROMOTION_WAITING_FOR_TIME -> "Cần thêm thời gian trước lần xác nhận tiếp theo."
                    LearningInsightSummary.PROMOTION_MISSING_RECALLS -> "Cần thêm lần nhớ lại độc lập."
                    LearningInsightSummary.PROMOTION_EVIDENCE_EXCLUDED -> "Một số bằng chứng không đủ điều kiện thăng hạng."
                    LearningInsightSummary.TRAJECTORY_RECOVERING -> "Xu hướng đang phục hồi sau khó khăn."
                    LearningInsightSummary.TRAJECTORY_REGRESSING -> "Kết quả gần đây đang suy giảm."
                    LearningInsightSummary.VERY_DIFFICULT_HIGH_RISK -> "Độ khó và rủi ro hiện ở mức cao."
                    LearningInsightSummary.RETENTION_RISK_HIGH -> "Khả năng duy trì kiến thức đang có rủi ro."
                    LearningInsightSummary.FOLLOW_RECOMMENDATION -> "Hãy ưu tiên hành động được đề xuất."
                    LearningInsightSummary.STABLE_FOLLOW_SCHEDULE -> "Tiếp tục theo lịch ôn hiện tại."
                    LearningInsightSummary.MASTERED_MONITOR_ONLY -> "Kiến thức ổn định; chỉ cần theo dõi định kỳ."
                    LearningInsightSummary.ENGINE_CONFIDENCE_LOW -> "Chưa có đủ dữ liệu để kết luận chắc chắn."
                    LearningInsightSummary.PRACTICE_IS_NOT_PROMOTION_EVIDENCE -> "Kết quả luyện tập không thay đổi đánh giá, lịch ôn hoặc bằng chứng thăng hạng."
                    LearningInsightSummary.MANUAL_RATING_IS_NOT_AUTOMATIC_EVIDENCE -> "Đánh giá thủ công được lưu nhưng không được coi là bằng chứng nhớ lại tự động."
                } else summary.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) + "."
            },
            actions = RecommendationAction.entries.associateWith { action ->
                (if (vi) "Đề xuất: " else "Recommended: ") + action.name.lowercase().replace('_', ' ')
            },
            metrics = LearningInsightMetricKind.entries.associateWith {
                it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
            },
            heading = if (vi) "Thông tin học tập" else "Learning insight",
            expand = if (vi) "Xem thêm" else "Show more",
            collapse = if (vi) "Thu gọn" else "Show less"
        )
    }
}

data class LearningInsightPresentation(
    val heading: String,
    val title: String,
    val summary: String,
    val recommendation: String?,
    val metrics: List<String>,
    val accessibilityText: String
)

object LearningInsightPresentationMapper {
    fun map(bundle: LearningInsightBundle, strings: LearningInsightStrings): LearningInsightPresentation {
        val insight = bundle.primary
        val metrics = insight.supportingMetrics.map { metric ->
            val value = when (val typed = metric.value) {
                is LearningInsightMetricValue.Score -> "${(typed.value * 100).toInt()}%"
                is LearningInsightMetricValue.Count -> typed.value.toString()
                is LearningInsightMetricValue.Duration -> formatDuration(typed.value.millis)
            }
            "${strings.metrics.getValue(metric.kind)}: $value"
        }
        val recommendation = insight.recommendedAction?.let(strings.actions::getValue)
        val title = strings.titles.getValue(insight.title)
        val summary = strings.summaries.getValue(insight.summary)
        return LearningInsightPresentation(
            strings.heading, title, summary, recommendation, metrics,
            listOfNotNull(strings.heading, title, summary, recommendation).plus(metrics).joinToString(". ")
        )
    }

    private fun formatDuration(millis: Long): String {
        val hours = millis / 3_600_000
        return if (hours >= 24) "${hours / 24}d" else "${hours}h"
    }
}

object LearningInsightViewportPolicy {
    fun visibleMetricCount(widthDp: Int, heightDp: Int): Int = when {
        heightDp < 600 -> 1
        widthDp < 720 -> 2
        else -> 3
    }
}
