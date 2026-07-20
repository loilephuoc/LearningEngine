package vn.loi.learning.infrastructure.contentmedia

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.zip.CRC32
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LegacyOpd3MediaExtractorTest {

    @Test
    fun `extracts archive entries into JVM media storage`() {
        val temporaryDirectory =
            Files.createTempDirectory(
                "learning-engine-media-extractor-"
            )

        try {
            val packageFile =
                temporaryDirectory.resolve(
                    "lesson.pkg"
                )

            val mediaDirectory =
                temporaryDirectory.resolve(
                    "media"
                )

            val firstEntry =
                TestEntry(
                    fileName =
                        "audio/example.mp3",
                    mediaType = 1,
                    content =
                        "example-audio"
                            .toByteArray(
                                StandardCharsets.UTF_8
                            )
                )

            val secondEntry =
                TestEntry(
                    fileName =
                        "images/example.png",
                    mediaType = 2,
                    content =
                        "example-image"
                            .toByteArray(
                                StandardCharsets.UTF_8
                            )
                )

            Files.write(
                packageFile,
                createPackage(
                    listOf(
                        firstEntry,
                        secondEntry
                    )
                )
            )

            val storage =
                JvmContentMediaStorage(
                    mediaDirectory
                )

            val extractor: PackageMediaExtractor =
                LegacyOpd3MediaExtractor(
                    archiveReader =
                        LegacyOpd3MediaArchiveReader(),
                    mediaStorage =
                        storage
                )

            val assets =
                extractor.extract(
                    packageFile = packageFile,
                    packageName = "lesson-package"
                )

            assertEquals(
                2,
                assets.size
            )

            val audioAsset =
                assets.first { asset ->
                    asset.fileName ==
                            firstEntry.fileName
                }

            val imageAsset =
                assets.first { asset ->
                    asset.fileName ==
                            secondEntry.fileName
                }

            assertEquals(
                "lesson-package/audio/example.mp3",
                audioAsset.relativePath
            )

            assertEquals(
                "lesson-package/images/example.png",
                imageAsset.relativePath
            )

            assertTrue(
                storage.exists(
                    audioAsset.relativePath
                )
            )

            assertTrue(
                storage.exists(
                    imageAsset.relativePath
                )
            )

            val storedAudioPath =
                assertNotNull(
                    storage.resolve(
                        audioAsset.relativePath
                    )
                )

            val storedImagePath =
                assertNotNull(
                    storage.resolve(
                        imageAsset.relativePath
                    )
                )

            assertContentEquals(
                firstEntry.content,
                Files.readAllBytes(
                    storedAudioPath
                )
            )

            assertContentEquals(
                secondEntry.content,
                Files.readAllBytes(
                    storedImagePath
                )
            )
        } finally {
            deleteRecursively(
                temporaryDirectory
            )
        }
    }

    private fun createPackage(
        entries: List<TestEntry>
    ): ByteArray {
        val encodedEntries =
            entries.map { entry ->
                EncodedTestEntry(
                    entry = entry,
                    fileNameBytes =
                        entry.fileName.toByteArray(
                            StandardCharsets.UTF_8
                        )
                )
            }

        val payloadStartOffset =
            HEADER_SIZE +
                    encodedEntries.sumOf { encodedEntry ->
                        ENTRY_FIXED_METADATA_SIZE +
                                encodedEntry.fileNameBytes.size
                    }

        var nextPayloadOffset =
            payloadStartOffset.toLong()

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
                        encodedEntries.size
                    )

                    encodedEntries.forEach { encodedEntry ->
                        output.writeShort(
                            encodedEntry.fileNameBytes.size
                        )

                        output.writeByte(
                            encodedEntry.entry.mediaType
                        )

                        output.writeLong(
                            nextPayloadOffset
                        )

                        output.writeLong(
                            encodedEntry.entry.content.size.toLong()
                        )

                        output.writeInt(
                            crc32(
                                encodedEntry.entry.content
                            ).toInt()
                        )

                        output.write(
                            encodedEntry.fileNameBytes
                        )

                        nextPayloadOffset +=
                            encodedEntry.entry.content.size
                    }

                    encodedEntries.forEach { encodedEntry ->
                        output.write(
                            encodedEntry.entry.content
                        )
                    }
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

    private fun deleteRecursively(
        root: Path
    ) {
        if (!Files.exists(root)) {
            return
        }

        Files.walk(root)
            .sorted(
                Comparator.reverseOrder()
            )
            .use { paths ->
                paths.forEach(
                    Files::deleteIfExists
                )
            }
    }

    private data class TestEntry(
        val fileName: String,
        val mediaType: Int,
        val content: ByteArray
    )

    private data class EncodedTestEntry(
        val entry: TestEntry,
        val fileNameBytes: ByteArray
    )

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

        const val HEADER_SIZE =
            MAGIC_SIZE +
                    INTEGER_SIZE +
                    INTEGER_SIZE

        const val ENTRY_FIXED_METADATA_SIZE =
            UNSIGNED_SHORT_SIZE +
                    UNSIGNED_BYTE_SIZE +
                    LONG_SIZE +
                    LONG_SIZE +
                    INTEGER_SIZE

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