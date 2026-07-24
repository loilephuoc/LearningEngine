package vn.loi.learning.application.contentpackaging

/**
 * Đại diện một entry file sẽ ghi vào tệp nén ZIP.
 */
data class DeterministicZipEntry(
    val relativePath: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeterministicZipEntry) return false
        return relativePath == other.relativePath && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = relativePath.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

/**
 * Port ghi các file vào archive ZIP đinh ninh (deterministic).
 * Application chỉ phụ thuộc contract này.
 */
fun interface DeterministicZipWriter {

    fun writeZip(files: List<DeterministicZipEntry>): ByteArray
}
