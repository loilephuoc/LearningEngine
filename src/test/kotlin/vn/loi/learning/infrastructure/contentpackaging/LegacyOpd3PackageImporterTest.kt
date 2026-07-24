package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.LegacyPackageCandidate
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.infrastructure.contentmedia.ImportedMediaVerifier
import vn.loi.learning.infrastructure.contentmedia.LegacyContentMediaPathMapper
import vn.loi.learning.infrastructure.contentmedia.PackageMediaExtractor
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

class LegacyOpd3PackageImporterTest {

    @Test
    fun `importer combines legacy JSON and OPD3 media`() {
        val directory =
            createTempDirectory(
                "legacy-opd3-package-importer-test"
            )

        try {
            val jsonPath =
                directory.resolve(
                    "2000Cau.json"
                )

            val packagePath =
                directory.resolve(
                    "2000Cau.pkg"
                )

            jsonPath.writeText(
                """
                [
                  {
                    "group": "50 Languages Listening",
                    "section": "50 Languages",
                    "lesson": "",
                    "en": "One",
                    "image": "ic_logo.png",
                    "audio": "audios50languages-0001.mp3",
                    "audio_vi": "audios50languages_vn-0001.mp3",
                    "vi": "Một",
                    "ipa": "",
                    "example": "",
                    "example_audio": ""
                  }
                ]
                """.trimIndent()
            )

            Files.write(
                packagePath,
                byteArrayOf(
                    0x4F,
                    0x50,
                    0x44,
                    0x33
                )
            )

            val mediaStorage =
                RecordingContentMediaStorage()

            val mediaExtractor =
                RecordingPackageMediaExtractor(
                    mediaStorage
                )

            val importer =
                LegacyOpd3PackageImporter(
                    jsonImporter =
                        LegacyJsonImporter(),
                    mediaExtractor =
                        mediaExtractor,
                    mediaPathMapper =
                        LegacyContentMediaPathMapper(),
                    mediaVerifier =
                        ImportedMediaVerifier(
                            mediaStorage
                        )
                )

            val result =
                importer.import(
                    LegacyPackageCandidate(
                        jsonSource = jsonPath.toString(),
                        mediaSource = packagePath.toString()
                    )
                )

            assertEquals(
                packagePath,
                mediaExtractor.receivedPackageFile
            )

            assertEquals(
                "2000Cau",
                mediaExtractor.receivedPackageName
            )

            assertEquals(
                1,
                result.contents.size
            )

            assertEquals(
                5,
                result.learningItems.size
            )

            val importedContent =
                result.contents.single()

            assertEquals(
                "media/2000Cau/audios50languages-0001.mp3",
                importedContent.media.primaryAudio
            )

            assertEquals(
                "media/2000Cau/audios50languages_vn-0001.mp3",
                importedContent.media.translatedAudio
            )

            assertEquals(
                "media/2000Cau/ic_logo.png",
                importedContent.media.image
            )

            assertTrue(
                result.warnings.isEmpty()
            )
        } finally {
            deleteRecursively(
                directory
            )
        }
    }

    @Test
    fun `importer reports skipped legacy records`() {
        val directory =
            createTempDirectory(
                "legacy-opd3-package-warning-test"
            )

        try {
            val jsonPath =
                directory.resolve(
                    "InvalidRecords.json"
                )

            val packagePath =
                directory.resolve(
                    "InvalidRecords.pkg"
                )

            jsonPath.writeText(
                """
                [
                  {
                    "group": "Test",
                    "section": "Test",
                    "lesson": "",
                    "en": "",
                    "image": "",
                    "audio": "",
                    "audio_vi": "",
                    "vi": "Thiếu câu tiếng Anh",
                    "ipa": "",
                    "example": "",
                    "example_audio": ""
                  }
                ]
                """.trimIndent()
            )

            Files.write(
                packagePath,
                byteArrayOf(
                    0x4F,
                    0x50,
                    0x44,
                    0x33
                )
            )

            val mediaStorage =
                RecordingContentMediaStorage()

            val importer =
                LegacyOpd3PackageImporter(
                    jsonImporter =
                        LegacyJsonImporter(),
                    mediaExtractor =
                        RecordingPackageMediaExtractor(
                            mediaStorage
                        ),
                    mediaPathMapper =
                        LegacyContentMediaPathMapper(),
                    mediaVerifier =
                        ImportedMediaVerifier(
                            mediaStorage
                        )
                )

            val result =
                importer.import(
                    LegacyPackageCandidate(
                        jsonSource = jsonPath.toString(),
                        mediaSource = packagePath.toString()
                    )
                )

            assertEquals(
                0,
                result.contents.size
            )

            assertEquals(
                0,
                result.learningItems.size
            )

            assertEquals(
                listOf(
                    "Skipped 1 legacy record(s)."
                ),
                result.warnings
            )
        } finally {
            deleteRecursively(
                directory
            )
        }
    }

    private class RecordingPackageMediaExtractor(
        private val mediaStorage: RecordingContentMediaStorage
    ) : PackageMediaExtractor {

        var receivedPackageFile: Path? = null
        var receivedPackageName: String? = null

        override fun extract(
            packageFile: Path,
            packageName: String,
            progressListener: ((processed: Int, total: Int, stage: vn.loi.learning.application.contentpackaging.PackageImportProgressStage, details: String?) -> Unit)?,
            cancellationSignal: vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal?
        ): List<ContentMediaAsset> {
            receivedPackageFile =
                packageFile

            receivedPackageName =
                packageName

            return listOf(
                mediaStorage.store(
                    packageName = packageName,
                    fileName = "audios50languages-0001.mp3",
                    content = byteArrayOf(1)
                ),
                mediaStorage.store(
                    packageName = packageName,
                    fileName = "audios50languages_vn-0001.mp3",
                    content = byteArrayOf(2)
                ),
                mediaStorage.store(
                    packageName = packageName,
                    fileName = "ic_logo.png",
                    content = byteArrayOf(3)
                )
            )
        }
    }

    private class RecordingContentMediaStorage :
        ContentMediaStorage {

        private val storedPaths =
            mutableSetOf<String>()

        override fun store(
            packageName: String,
            fileName: String,
            content: ByteArray
        ): ContentMediaAsset {
            val relativePath =
                "media/$packageName/$fileName"

            storedPaths +=
                relativePath

            return ContentMediaAsset(
                packageName = packageName,
                fileName = fileName,
                relativePath = relativePath
            )
        }

        override fun resolve(
            relativePath: String
        ): Path? =
            if (relativePath in storedPaths) {
                Path.of(
                    relativePath
                )
            } else {
                null
            }

        override fun exists(
            relativePath: String
        ): Boolean =
            relativePath in storedPaths
    }

    private fun deleteRecursively(
        directory: Path
    ) {
        Files.walk(directory).use { paths ->
            paths
                .sorted(Comparator.reverseOrder())
                .forEach(Files::deleteIfExists)
        }
    }
}