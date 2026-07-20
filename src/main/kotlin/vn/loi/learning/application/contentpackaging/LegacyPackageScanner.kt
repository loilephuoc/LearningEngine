package vn.loi.learning.application.contentpackaging

/**
 * Port tìm các cặp package legacy JSON + PKG.
 *
 * Application Layer không phụ thuộc filesystem cụ thể.
 */
fun interface LegacyPackageScanner {

    fun scan(): List<LegacyPackageCandidate>
}