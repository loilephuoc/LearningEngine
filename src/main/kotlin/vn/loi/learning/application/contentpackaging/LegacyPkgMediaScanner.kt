package vn.loi.learning.application.contentpackaging

/**
 * Cổng quét thông tin cấu trúc danh mục file media bên trong gói legacy PKG.
 */
fun interface LegacyPkgMediaScanner {
    fun scanMediaEntries(packageSource: String): List<String>
}
