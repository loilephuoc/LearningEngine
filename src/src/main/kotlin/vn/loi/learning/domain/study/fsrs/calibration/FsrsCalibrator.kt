package vn.loi.learning.domain.study.fsrs.calibration

import vn.loi.learning.domain.study.fsrs.calibration.model.ReviewHistoryDataset
import vn.loi.learning.domain.study.fsrs.model.FsrsParameters

/**
 * Contract của một bộ hiệu chỉnh tham số FSRS.
 *
 * Calibrator nhận lịch sử review đã được chuẩn hóa
 * và trả về một bộ FsrsParameters mới.
 *
 * Contract này không quy định:
 * - thuật toán tối ưu;
 * - cách lưu dữ liệu;
 * - cách chạy song song;
 * - cách đánh giá chất lượng.
 */
fun interface FsrsCalibrator {

    /**
     * Hiệu chỉnh tham số FSRS từ lịch sử review.
     */
    fun calibrate(
        dataset: ReviewHistoryDataset
    ): FsrsParameters
}