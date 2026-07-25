package vn.loi.learning.application.contentpackaging

/**
 * Cấu hình giới hạn an toàn tài nguyên (Resource Safety Limits) khi đọc và xác thực gói OPD3.
 */
data class PackageSafetyLimits(
    val maxEntryCount: Int = DEFAULT_MAX_ENTRY_COUNT,
    val maxSingleEntrySizeBytes: Long = DEFAULT_MAX_SINGLE_ENTRY_SIZE_BYTES,
    val maxTotalUncompressedSizeBytes: Long = DEFAULT_MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES
) {
    companion object {
        const val DEFAULT_MAX_ENTRY_COUNT: Int = 50_000
        const val DEFAULT_MAX_SINGLE_ENTRY_SIZE_BYTES: Long = 500 * 1024 * 1024L
        const val DEFAULT_MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES: Long = 2L * 1024 * 1024 * 1024L
        const val SHA256_HEX_LENGTH: Int = 64

        val DEFAULT = PackageSafetyLimits()
    }
}
