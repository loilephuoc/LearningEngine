package vn.loi.learning.application.contentpackaging

/**
 * Báo cáo kết quả xác thực tính hợp lệ và toàn vẹn của một gói nội dung OPD3.
 */
data class PackageVerificationReport(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
) {
    val totalIssueCount: Int
        get() = errors.size + warnings.size
}
