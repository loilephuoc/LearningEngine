package vn.loi.learning.application.contentpackaging

/**
 * Cổng đọc nội dung chuỗi văn bản JSON từ đường dẫn nguồn file legacy.
 */
fun interface LegacyJsonSourceReader {
    fun readJsonText(jsonSource: String): String
}
