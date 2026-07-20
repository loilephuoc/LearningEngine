package vn.loi.learning.domain.study.fsrs.model

/**
 * Cấu hình đầy đủ của thuật toán FSRS.
 *
 * Configuration xác định:
 * - bộ tham số toán học của FSRS;
 * - tỷ lệ ghi nhớ mục tiêu.
 *
 * Đây là immutable Value Object và không chứa trạng thái học tập
 * của một Memory cụ thể.
 *
 * Việc gom cấu hình vào một đối tượng duy nhất giúp:
 * - thay đổi bộ tham số mà không sửa thuật toán;
 * - hỗ trợ nhiều profile FSRS trong tương lai;
 * - chuẩn bị cho calibration từ lịch sử review;
 * - tránh constructor của thuật toán ngày càng dài.
 */
data class FsrsConfiguration(
    val parameters: FsrsParameters,
    val desiredRetention: DesiredRetention
) {

    companion object {

        /**
         * Cấu hình mặc định của Learning Engine.
         *
         * Sử dụng:
         * - bộ tham số mặc định chính thức của FSRS-6;
         * - desired retention mặc định của hệ thống.
         */
        val DEFAULT: FsrsConfiguration =
            FsrsConfiguration(
                parameters = FsrsParameters.DEFAULT,
                desiredRetention = DesiredRetention.DEFAULT
            )
    }
}