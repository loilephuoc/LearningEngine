package vn.loi.learning.application.importing

/**
 * Port contract cho dịch vụ chuyển đổi JSON từ ứng dụng legacy sang Domain Model.
 */
fun interface LegacyJsonImporter {

    fun import(
        sourceName: String,
        jsonText: String
    ): LegacyImportResult
}
