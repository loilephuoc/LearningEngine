package vn.loi.learning.infrastructure.contentmedia

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.RandomAccessFile
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.CRC32
import vn.loi.learning.application.contentpackaging.InvalidOpd3BinaryPackageException

class LegacyOpd3MediaArchiveReader {

    fun readEntries(
        packageFile: Path
    ): List<LegacyOpd3MediaEntry> {
        require(Files.isRegularFile(packageFile)) {
            "Legacy media package does not exist: $packageFile"
        }

        val packageSize =
            Files.size(
                packageFile
            )

        val entries: List<LegacyOpd3MediaEntry>
        val metadataEndOffset: Long

        try {
            DataInputStream(
                BufferedInputStream(
                    Files.newInputStream(packageFile)
                )
            ).use { input ->
                val magic =
                    ByteArray(
                        MAGIC_SIZE
                    )

            input.readFully(
                magic
            )

            require(
                magic.contentEquals(
                    MAGIC_BYTES
                )
            ) {
                "Unsupported legacy package signature: $packageFile"
            }

            val version =
                input.readInt()

            require(
                version == SUPPORTED_VERSION
            ) {
                "Unsupported legacy package version $version: $packageFile"
            }

            val entryCount =
                input.readInt()

            require(
                entryCount >= 0
            ) {
                "Legacy package entry count must not be negative."
            }

            var consumedBytes =
                HEADER_SIZE.toLong()

            require(entryCount <= MAX_ENTRY_COUNT) {
                "OPD3 media entry count exceeds limit $MAX_ENTRY_COUNT."
            }

                entries = List(entryCount) {
                    val result =
                        readEntry(
                            input
                        )

                    consumedBytes +=
                        ENTRY_FIXED_METADATA_SIZE +
                                result.fileName
                                    .toByteArray(
                                        StandardCharsets.UTF_8
                                    )
                                    .size

                    result
                }

                metadataEndOffset =
                    consumedBytes
            }
        } catch (exception: InvalidOpd3BinaryPackageException) {
            throw exception
        } catch (exception: Exception) {
            throw InvalidOpd3BinaryPackageException(
                message = "Invalid OPD3 binary package index: ${exception.message ?: packageFile}",
                cause = exception
            )
        }

        validateEntries(
            packageFile = packageFile,
            packageSize = packageSize,
            metadataEndOffset = metadataEndOffset,
            entries = entries
        )

        return entries
    }

    fun readBytes(
        packageFile: Path,
        entry: LegacyOpd3MediaEntry
    ): ByteArray {
        require(
            Files.isRegularFile(
                packageFile
            )
        ) {
            "Legacy media package does not exist: $packageFile"
        }

        require(
            entry.size <= Int.MAX_VALUE.toLong()
        ) {
            "Media entry is too large to load: ${entry.fileName}"
        }

        val packageSize =
            Files.size(
                packageFile
            )

        require(
            entry.offset <= packageSize &&
                    entry.size <= packageSize - entry.offset
        ) {
            "Media entry exceeds package bounds: ${entry.fileName}"
        }

        val bytes =
            ByteArray(
                entry.size.toInt()
            )

        RandomAccessFile(
            packageFile.toFile(),
            READ_MODE
        ).use { file ->
            file.seek(
                entry.offset
            )

            file.readFully(
                bytes
            )
        }

        val actualCrc32 =
            CRC32()
                .apply {
                    update(
                        bytes
                    )
                }
                .value

        require(
            actualCrc32 == entry.crc32
        ) {
            "CRC32 mismatch for media entry: ${entry.fileName}"
        }

        return bytes
    }

    private fun readEntry(
        input: DataInputStream
    ): LegacyOpd3MediaEntry {
        val fileNameLength =
            input.readUnsignedShort()

        val mediaType =
            input.readUnsignedByte()

        val offset =
            input.readLong()

        val size =
            input.readLong()

        val crc32 =
            input.readInt()
                .toLong() and
                    UNSIGNED_INT_MASK

        require(
            fileNameLength > 0
        ) {
            "Legacy media entry file name must not be empty."
        }

        require(
            offset >= 0L
        ) {
            "Legacy media entry offset must not be negative."
        }

        require(
            size >= 0L
        ) {
            "Legacy media entry size must not be negative."
        }

        require(mediaType in SUPPORTED_MEDIA_TYPES) {
            "Unsupported OPD3 media type $mediaType."
        }

        val fileNameBytes =
            ByteArray(
                fileNameLength
            )

        input.readFully(
            fileNameBytes
        )

        val fileName = StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(fileNameBytes))
            .toString()

        require(
            fileName.isNotBlank()
        ) {
            "Legacy media entry file name must not be blank."
        }

        return LegacyOpd3MediaEntry(
            fileName = fileName,
            mediaType = mediaType,
            offset = offset,
            size = size,
            crc32 = crc32
        )
    }

    private fun validateEntries(
        packageFile: Path,
        packageSize: Long,
        metadataEndOffset: Long,
        entries: List<LegacyOpd3MediaEntry>
    ) {
        val duplicateFileName =
            entries
                .groupingBy { entry ->
                    entry.fileName
                }
                .eachCount()
                .entries
                .firstOrNull { entry ->
                    entry.value > 1
                }
                ?.key

        require(
            duplicateFileName == null
        ) {
            "Duplicate media entry file name: $duplicateFileName"
        }

        entries.forEach { entry ->
            require(
                entry.offset >= metadataEndOffset
            ) {
                "Media entry overlaps package metadata: ${entry.fileName}"
            }

            require(
                entry.offset <= packageSize
            ) {
                "Media entry offset exceeds package bounds: ${entry.fileName}"
            }

            require(
                entry.size <= packageSize - entry.offset
            ) {
                "Media entry exceeds package bounds: ${entry.fileName}"
            }
        }

        val entriesByOffset =
            entries.sortedBy { entry ->
                entry.offset
            }

        entriesByOffset
            .zipWithNext()
            .forEach { pair ->
                val current =
                    pair.first

                val next =
                    pair.second

                val currentEnd =
                    current.offset +
                            current.size

                require(
                    currentEnd <= next.offset
                ) {
                    "Media entries overlap: ${current.fileName} and ${next.fileName}"
                }
            }

        if (entries.isNotEmpty()) {
            require(
                entriesByOffset
                    .first()
                    .offset == metadataEndOffset
            ) {
                "Legacy package payload does not start after metadata: $packageFile"
            }
        }
    }

    private companion object {

        const val MAGIC_SIZE =
            4

        const val HEADER_SIZE =
            12

        const val ENTRY_FIXED_METADATA_SIZE =
            23L

        const val SUPPORTED_VERSION =
            1

        const val MAX_ENTRY_COUNT = 100_000

        val SUPPORTED_MEDIA_TYPES = 1..2

        const val UNSIGNED_INT_MASK =
            0xFFFF_FFFFL

        const val READ_MODE =
            "r"

        val MAGIC_BYTES =
            byteArrayOf(
                'O'.code.toByte(),
                'P'.code.toByte(),
                'D'.code.toByte(),
                '3'.code.toByte()
            )
    }
}

data class LegacyOpd3MediaEntry(
    val fileName: String,
    val mediaType: Int,
    val offset: Long,
    val size: Long,
    val crc32: Long
)
