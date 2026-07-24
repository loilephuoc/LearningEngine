package vn.loi.learning.infrastructure.contentpackaging

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import vn.loi.learning.application.contentpackaging.DeterministicZipEntry
import vn.loi.learning.application.contentpackaging.DeterministicZipWriter

/**
 * Adapter JVM ghi các file vào archive ZIP đinh ninh (deterministic).
 */
class JvmDeterministicZipWriter : DeterministicZipWriter {

    override fun writeZip(files: List<DeterministicZipEntry>): ByteArray {
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
        const val FIXED_TIMESTAMP = 1577836800000L
    }
}

typealias DeterministicZipWriter = JvmDeterministicZipWriter
