package vn.loi.learning.domain.study.fsrs.calibration.model

import vn.loi.learning.domain.study.fsrs.model.FsrsConfiguration

/**
 * Một phiên bản cấu hình FSRS có thể được lựa chọn
 * và sử dụng bởi Learning Engine.
 *
 * Parameter Profile chứa:
 * - định danh ổn định;
 * - tên hiển thị;
 * - nguồn tạo profile;
 * - cấu hình FSRS thực tế.
 *
 * Profile không chứa trạng thái Memory và không thực hiện scheduling.
 */
data class FsrsParameterProfile(
    val id: FsrsParameterProfileId,
    val name: String,
    val source: FsrsParameterProfileSource,
    val configuration: FsrsConfiguration
) {

    init {
        require(name.isNotBlank()) {
            "FSRS parameter profile name must not be blank."
        }

        require(name == name.trim()) {
            "FSRS parameter profile name must not contain surrounding whitespace."
        }
    }

    companion object {

        /**
         * Profile mặc định đi kèm Learning Engine.
         */
        val DEFAULT: FsrsParameterProfile =
            FsrsParameterProfile(
                id =
                    FsrsParameterProfileId(
                        "fsrs-default-v6"
                    ),
                name =
                    "FSRS Default v6",
                source =
                    FsrsParameterProfileSource.DEFAULT,
                configuration =
                    FsrsConfiguration.DEFAULT
            )
    }
}