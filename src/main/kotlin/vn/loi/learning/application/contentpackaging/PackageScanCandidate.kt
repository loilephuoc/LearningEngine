package vn.loi.learning.application.contentpackaging

/**
 * Một nguồn package được PackageScanner phát hiện.
 *
 * Candidate chưa phải ContentPackage đã được xác thực hoặc cài đặt.
 * source chỉ là định danh vị trí do adapter cung cấp.
 */
data class PackageScanCandidate(
    val source: String
) {

    init {
        require(source.isNotBlank()) {
            "Package scan candidate source must not be blank."
        }
    }
}
