package vn.loi.learning.infrastructure.contentpackaging

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Adapter JVM ghi các file vào archive ZIP đinh ninh (deterministic).
 *
 * Để đảm bảo tính byte-for-byte deterministic:
 * - Các entry được sắp xếp đinh ninh theo tên đường dẫn;
 * - Thời gian chỉnh sửa ZipEntry (modification time) được cố định về mốc Epoch 0;
 * - Thuật toán nén được cấu hình đồng nhất.
 */
class DeterministicZipWriter {

    fun writeZip(files: List<DeterministicZipEntry>): ByteArray {
        val sortedFiles = files.sortedBy { it.relativePath }
        val outputStream = ByteArrayOutputStream()

        ZipOutputStream(outputStream).use { zip ->
            zip.setLevel(ZipOutputStream.DEFLATED)
            sortedFiles.forEach { entry ->
                val zipEntry = ZipEntry(entry.relativePath).apply {
                    time = FIXED_TIMESTAMP
                }
                zip.putNextEntry(zipEntry)
                zip.write(entry.bytes)
                zip.closeEntry()
            }
            zip.finish()
        }

        return outputStream.toByteArray()
    }

    companion object {
        /**
         * Mốc thời gian cố định (2020-01-01T00:00:00Z UTC) giúp đinh ninh hoá ZipEntry time.
         */
        const val FIXED_TIMESTAMP = 1577836800000L
    }
}

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
