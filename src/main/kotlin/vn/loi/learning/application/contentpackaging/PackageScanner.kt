package vn.loi.learning.application.contentpackaging

/**
 * Port dùng để tìm các nguồn Content Package có thể được nhập.
 *
 * Application không biết cách duyệt filesystem.
 * Adapter JVM sẽ triển khai ở bước sau.
 */
fun interface PackageScanner {

    fun scan(): List<PackageScanCandidate>
}
