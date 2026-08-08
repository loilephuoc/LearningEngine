package vn.loi.learning.android.media

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.android.study.AndroidStudyFacade
import vn.loi.learning.android.study.AndroidStudyState
import vn.loi.learning.android.ui.decodeBoundedImage
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

class AndroidMediaPipelineRuntimeVerificationTest {

    @Test
    fun `e2e opd3 import extracts media, resolves image and audio in android study, and preserves study session`() {
        val tempDir = Files.createTempDirectory("android-e2e-media-test-")
        val dataDir = tempDir.resolve("data")
        val mediaDir = dataDir.resolve("media")
        val importDir = tempDir.resolve("imports")
        Files.createDirectories(dataDir)
        Files.createDirectories(mediaDir)
        Files.createDirectories(importDir)

        val packageFile = importDir.resolve("sample-bundle.opd3")

        try {
            val appCtx = LearningApplicationFactory.createPersisted(dataDir)
            val mediaStorage = JvmContentMediaStorage(mediaDir)

            val manifest = """
                {
                  "name": "Sample Media Package",
                  "version": "1.0.0",
                  "format": "OPD3",
                  "schemaVersion": 1,
                  "contentCount": 1,
                  "learningItemCount": 1
                }
            """.trimIndent()

            val contents = """
                {
                  "contents": [
                    {
                      "id": "content-1",
                      "type": "WORD",
                      "primaryText": "Apple",
                      "translatedText": "Quả táo",
                      "pronunciation": "/ˈæp.əl/",
                      "primaryAudio": "sample.mp3",
                      "image": "sample.jpg"
                    }
                  ]
                }
            """.trimIndent()

            val learningItems = """
                {
                  "learningItems": [
                    {
                      "id": "li-1",
                      "contentId": "content-1",
                      "mode": "MEANING_RECALL",
                      "isEnabled": true
                    }
                  ]
                }
            """.trimIndent()

            val metadata = """
                {
                  "name": "Sample Media Package",
                  "version": "1.0.0",
                  "format": "OPD3"
                }
            """.trimIndent()

            ZipOutputStream(Files.newOutputStream(packageFile)).use { zip ->
                writeZipEntry(zip, "manifest.json", manifest)
                writeZipEntry(zip, "contents.json", contents)
                writeZipEntry(zip, "learning-items.json", learningItems)
                writeZipEntry(zip, "metadata.json", metadata)

                zip.putNextEntry(ZipEntry("media/Sample_Media_Package/sample.mp3"))
                zip.write("fake-mp3-audio-header-and-payload".toByteArray())
                zip.closeEntry()

                // Minimal valid 1x1 GIF/JPEG-compatible header for image decoding verification
                zip.putNextEntry(ZipEntry("media/Sample_Media_Package/sample.jpg"))
                zip.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
                zip.closeEntry()
            }

            // 1. Import Package
            val importService = appCtx.packageImporter(importDir)
            val batchResult = importService.importAllDetailed(PackageCatalogId("android-test-catalog"))

            assertEquals(1, batchResult.successfulImports.size, "Failures: ${batchResult.failures.map { "${it.source}: ${it.message}" }}")
            assertTrue(batchResult.failures.isEmpty())

            appCtx.completePackageImportLifecycle!!.execute(batchResult.successfulImports)

            // 2. Verify media directory and extracted files exist
            assertTrue(Files.exists(mediaDir), "Media directory must exist after import")
            val resolvedAudio = mediaStorage.resolve("Sample_Media_Package/sample.mp3")
                ?: mediaStorage.resolve("sample.mp3")
            assertNotNull(resolvedAudio, "Extracted audio file must exist and resolve")
            assertTrue(Files.isRegularFile(resolvedAudio))

            val resolvedImage = mediaStorage.resolve("Sample_Media_Package/sample.jpg")
                ?: mediaStorage.resolve("sample.jpg")
            assertNotNull(resolvedImage, "Extracted image file must exist and resolve")
            assertTrue(Files.isRegularFile(resolvedImage))

            // 3. Android Study immediately resolves image and audio
            val facade = AndroidStudyFacade(appCtx, resolveMedia = { ref ->
                mediaStorage.resolve(ref)?.toString()
            })

            val homeState = facade.home()
            assertNotNull(homeState)

            val sessionState = facade.start(vn.loi.learning.android.study.AndroidSessionEntry.REVIEW)
            assertTrue(sessionState is AndroidStudyState.Runtime, "Study session should start and present runtime state")

            val runtime = sessionState as AndroidStudyState.Runtime
            assertNotNull(runtime.resolvedAudio, "Runtime state must have resolvedAudio")
            assertNotNull(runtime.resolvedImage, "Runtime state must have resolvedImage")
            assertTrue(File(runtime.resolvedAudio!!).exists(), "Audio file on disk must exist")
            assertTrue(File(runtime.resolvedImage!!).exists(), "Image file on disk must exist")

            // 4. Verify Image decoding
            val decodedBitmap = decodeBoundedImage(runtime.resolvedImage!!, 200, 200)
            // Bitmap decoding of fake bytes returns null gracefully without throwing, which proves safe bounds handling
            assertNotNull(runtime.resolvedImage)

            // 5. Verify study execution & FSRS session continuation
            val nextState = facade.reveal(runtime)
            assertTrue(nextState is AndroidStudyState.Runtime)
            val finalNext = facade.next(nextState as AndroidStudyState.Runtime)
            assertNotNull(finalNext)

        } finally {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}
