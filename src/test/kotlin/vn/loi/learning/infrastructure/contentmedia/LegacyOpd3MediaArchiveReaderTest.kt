package vn.loi.learning.infrastructure.contentmedia

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.CRC32
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class LegacyOpd3MediaArchiveReaderTest {

    private val reader =
        LegacyOpd3MediaArchiveReader()

    @Test
    fun `reads entry metadata and payload from OPD3 archive`() {
        val fileName =
            "lesson/audio/example.mp3"

        val payload =
            "synthetic-media-content"
                .toByteArray(
                    StandardCharsets.UTF_8
                )

        val packageBytes =
            createPackage(
                fileName = fileName,
                mediaType = 1,
                payload = payload
            )

        val packageFile =
            Files.createTempFile(
                "learning-engine-opd3-",
                ".pkg"
            )

        try {
            Files.write(
                packageFile,
                packageBytes
            )

            val entries =
                reader.readEntries(
                    packageFile
                )

            assertEquals(
                1,
                entries.size
            )

            val entry =
                entries.single()

            assertEquals(
                fileName,
                entry.fileName
            )

            assertEquals(
                1,
                entry.mediaType
            )

            assertEquals(
                payload.size.toLong(),
                entry.size
            )

            assertEquals(
                crc32(payload),
                entry.crc32
            )

            assertContentEquals(
                payload,
                reader.readBytes(
                    packageFile,
                    entry
                )
            )
        } finally {
            Files.deleteIfExists(
                packageFile
            )
        }
    }

    private fun createPackage(
        fileName: String,
        mediaType: Int,
        payload: ByteArray
    ): ByteArray {
        val fileNameBytes =
            fileName.toByteArray(
                StandardCharsets.UTF_8
            )

        val headerSize =
            MAGIC_SIZE +
                    INTEGER_SIZE +
                    INTEGER_SIZE

        val entryMetadataSize =
            UNSIGNED_SHORT_SIZE +
                    UNSIGNED_BYTE_SIZE +
                    LONG_SIZE +
                    LONG_SIZE +
                    INTEGER_SIZE +
                    fileNameBytes.size

        val payloadOffset =
            headerSize +
                    entryMetadataSize

        return ByteArrayOutputStream()
            .use { byteStream ->
                DataOutputStream(
                    byteStream
                ).use { output ->
                    output.write(
                        MAGIC_BYTES
                    )

                    output.writeInt(
                        SUPPORTED_VERSION
                    )

                    output.writeInt(
                        1
                    )

                    output.writeShort(
                        fileNameBytes.size
                    )

                    output.writeByte(
                        mediaType
                    )

                    output.writeLong(
                        payloadOffset.toLong()
                    )

                    output.writeLong(
                        payload.size.toLong()
                    )

                    output.writeInt(
                        crc32(payload).toInt()
                    )

                    output.write(
                        fileNameBytes
                    )

                    output.write(
                        payload
                    )
                }

                byteStream.toByteArray()
            }
    }

    private fun crc32(
        bytes: ByteArray
    ): Long =
        CRC32()
            .apply {
                update(bytes)
            }
            .value

    private companion object {

        const val MAGIC_SIZE =
            4

        const val INTEGER_SIZE =
            4

        const val LONG_SIZE =
            8

        const val UNSIGNED_SHORT_SIZE =
            2

        const val UNSIGNED_BYTE_SIZE =
            1

        const val SUPPORTED_VERSION =
            1

        val MAGIC_BYTES =
            byteArrayOf(
                'O'.code.toByte(),
                'P'.code.toByte(),
                'D'.code.toByte(),
                '3'.code.toByte()
            )
    }
}