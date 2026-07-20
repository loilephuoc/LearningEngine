package vn.loi.learning.domain.study.fsrs.calibration.model

/**
 * Nguồn tạo ra một bộ tham số FSRS.
 */
enum class FsrsParameterProfileSource {

    /**
     * Bộ tham số mặc định đi kèm Learning Engine.
     */
    DEFAULT,

    /**
     * Bộ tham số được tạo bằng quá trình calibration
     * từ lịch sử review.
     */
    CALIBRATED,

    /**
     * Bộ tham số được nhập từ nguồn bên ngoài.
     */
    IMPORTED
}