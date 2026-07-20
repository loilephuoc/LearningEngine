package vn.loi.learning.application.reviewhistory

import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.study.memory.model.ReviewEvent

/**
 * Application service truy vấn lịch sử review.
 *
 * Service này:
 * - đọc ReviewEvent thông qua ReviewEventRepository;
 * - áp dụng các điều kiện tùy chọn từ ReviewHistoryQuery;
 * - không thay đổi dữ liệu;
 * - không thực hiện analytics;
 * - không thực hiện calibration;
 * - luôn trả kết quả theo thời điểm review tăng dần.
 */
class ReviewHistoryQueryService(
    private val reviewEventRepository: ReviewEventRepository
) {

    fun query(
        query: ReviewHistoryQuery
    ): List<ReviewEvent> {
        val events =
            query.learningItemId?.let { learningItemId ->
                reviewEventRepository.findAll(
                    learnerId = query.learnerId,
                    learningItemId = learningItemId
                )
            } ?: reviewEventRepository.findAll(
                learnerId = query.learnerId
            )

        return events
            .asSequence()
            .filter { event ->
                query.period?.contains(event.reviewedAt) ?: true
            }
            .filter { event ->
                query.ratings.isEmpty() ||
                        event.rating in query.ratings
            }
            .sortedBy { event ->
                event.reviewedAt.epochMillis
            }
            .toList()
    }
}