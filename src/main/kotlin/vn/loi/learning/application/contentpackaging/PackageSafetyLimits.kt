package vn.loi.learning.application.contentpackaging

/**
 * Giới hạn an toàn tài nguyên (Resource Safety Limits) khi đọc và xác thực gói OPD3.
 */
object PackageSafetyLimits {
    /** Số lượng file entry tối đa cho phép trong 1 archive ZIP */
    const val MAX_ENTRY_COUNT: Int = 10_000

    /** Dung lượng giải nén tối đa cho 1 file entry đơn lẻ (500 MB) */
    const val MAX_SINGLE_ENTRY_SIZE_BYTES: Long = 500 * 1024 * 1024L

    /** Tổng dung lượng giải nén tối đa của toàn bộ archive (2 GB) */
    const val MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES: Long = 2L * 1024 * 1024 * 1024L

    /** Tỉ lệ nén tối đa cho phép nghi ngờ Zip Bomb (ví dụ: 100x cho file > 10MB) */
    const val MAX_COMPRESSION_RATIO: Int = 100

    /** Chiều dài chuỗi SHA-256 hex chuẩn */
    const val SHA256_HEX_LENGTH: Int = 64
}
