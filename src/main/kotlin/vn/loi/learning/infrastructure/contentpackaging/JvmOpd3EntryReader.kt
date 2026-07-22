package vn.loi.learning.infrastructure.contentpackaging

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile
import vn.loi.learning.application.contentpackaging.InvalidPackageTextEncodingException
import vn.loi.learning.application.contentpackaging.PackageEntryTooLargeException

class JvmOpd3EntryReader(
    private val limits: Opd3EntryReadLimits =
        Opd3EntryReadLimits()
) : Opd3EntryReader {

    override fun readText(
        archive: ZipFile,
        entryName: String
    ): String? {
        val entry = archive.getEntry(entryName)
            ?: return null

        if (entry.isDirectory) {
            return null
        }

        if (
            entry.size > limits.maximumTextEntryBytes
        ) {
            throw PackageEntryTooLargeException(
                entryName = entryName,
                maximumBytes = limits.maximumTextEntryBytes
            )
        }

        val bytes =
            archive.getInputStream(entry).use { input ->
                val output = ByteArrayOutputStream(
                    initialBufferSize(entry.size)
                )
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalBytes = 0L

                while (true) {
                    val read = input.read(buffer)

                    if (read < 0) {
                        break
                    }

                    totalBytes += read

                    if (
                        totalBytes > limits.maximumTextEntryBytes
                    ) {
                        throw PackageEntryTooLargeException(
                            entryName = entryName,
                            maximumBytes = limits.maximumTextEntryBytes
                        )
                    }

                    output.write(buffer, 0, read)
                }

                output.toByteArray()
            }

        return decodeUtf8(
            entryName = entryName,
            bytes = bytes
        )
    }

    private fun decodeUtf8(
        entryName: String,
        bytes: ByteArray
    ): String =
        try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(
                    CodingErrorAction.REPORT
                )
                .onUnmappableCharacter(
                    CodingErrorAction.REPORT
                )
                .decode(
                    ByteBuffer.wrap(bytes)
                )
                .toString()
        } catch (exception: Exception) {
            throw InvalidPackageTextEncodingException(
                entryName = entryName,
                cause = exception
            )
        }

    private fun initialBufferSize(
        declaredSize: Long
    ): Int =
        declaredSize
            .coerceAtLeast(0L)
            .coerceAtMost(
                limits.maximumTextEntryBytes
            )
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
}
