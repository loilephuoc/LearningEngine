package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.infrastructure.contentmedia.PackageMediaExtractor

class JvmPackageContentImporterMediaIntegrationTest {

    @Test
    fun `extracts media and maps imported content media paths`() {
        val packageFile =
            Files.createTempFile(
                "legacy-media-package-",
                ".pkg"
            )

        try {
            writeLegacyPackage(
                packageFile
            )

            val extractor =
                RecordingPackageMediaExtractor()

            val importer =
                JvmPackageContentImporter(
                    archiveReader =
                        JvmOpd3ArchiveReader(),
                    entryReader =
                        JvmOpd3EntryReader(),
                    mediaExtractor =
                        extractor
                )

            val result =
                importer.importContent(
                    PackageScanCandidate(
                        source =
                            packageFile.toString()
                    )
                )

            assertTrue(
                extractor.wasCalled
            )

            assertEquals(
                packageFile,
                extractor.receivedPackageFile
            )

            assertTrue(
                extractor.receivedPackageName
                    .orEmpty()
                    .startsWith(
                        "legacy-library-"
                    )
            )

            val importedContent =
                result.contents.single()

            assertEquals(
                "${extractor.receivedPackageName}/audio/door.mp3",
                importedContent.media.primaryAudio
            )

            assertEquals(
                "${extractor.receivedPackageName}/images/door.png",
                importedContent.media.image
            )
        } finally {
            Files.deleteIfExists(
                packageFile
            )
        }
    }

    private fun writeLegacyPackage(
        packageFile: Path
    ) {
        ZipOutputStream(
            Files.newOutputStream(
                packageFile
            )
        ).use { zip ->
            zip.putNextEntry(
                ZipEntry(
                    "content.json"
                )
            )

            zip.write(
                """
                [
                  {
                    "group": "Short Stories",
                    "section": "Section 1",
                    "lesson": "Lesson 1",
                    "en": "She opened the door.",
                    "vi": "Co ay mo cua.",
                    "audio": "audio/door.mp3",
                    "image": "images/door.png"
                  }
                ]
                """.trimIndent()
                    .toByteArray()
            )

            zip.closeEntry()
        }
    }

    private class RecordingPackageMediaExtractor :
        PackageMediaExtractor {

        var wasCalled: Boolean =
            false

        var receivedPackageFile: Path? =
            null

        var receivedPackageName: String? =
            null

        override fun extract(
            packageFile: Path,
            packageName: String
        ): List<ContentMediaAsset> {
            wasCalled =
                true

            receivedPackageFile =
                packageFile

            receivedPackageName =
                packageName

            return listOf(
                ContentMediaAsset(
                    packageName =
                        packageName,
                    fileName =
                        "audio/door.mp3",
                    relativePath =
                        "$packageName/audio/door.mp3"
                ),
                ContentMediaAsset(
                    packageName =
                        packageName,
                    fileName =
                        "images/door.png",
                    relativePath =
                        "$packageName/images/door.png"
                )
            )
        }
    }
}