package vn.loi.learning.application.contentpackaging

/**
 * Một package legacy OPD3 gồm hai nguồn đi cùng nhau:
 *
 * - jsonSource chứa metadata và nội dung học;
 * - mediaSource chứa media nhị phân theo format OPD3.
 *
 * Candidate chỉ đại diện cho cặp nguồn được phát hiện.
 * Việc đọc, xác thực và import dữ liệu thuộc trách nhiệm của importer.
 */
data class LegacyPackageCandidate(
    val jsonSource: String,
    val mediaSource: String
) {

    init {
        require(jsonSource.isNotBlank()) {
            "Legacy package JSON source must not be blank."
        }

        require(mediaSource.isNotBlank()) {
            "Legacy package media source must not be blank."
        }
    }
}