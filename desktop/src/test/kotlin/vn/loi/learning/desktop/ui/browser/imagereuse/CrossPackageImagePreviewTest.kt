package vn.loi.learning.desktop.ui.browser.imagereuse

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.desktop.ui.contentlibrary.ThumbnailResult
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

class CrossPackageImagePreviewTest {

    private lateinit var tempMediaDir: Path
    private lateinit var storage: JvmContentMediaStorage

    @BeforeTest
    fun setUp() {
        tempMediaDir = Files.createTempDirectory("crosspackage-preview-test")
        storage = JvmContentMediaStorage(tempMediaDir)
    }

    @AfterTest
    fun tearDown() {
        tempMediaDir.toFile().deleteRecursively()
    }

    private fun createTestImageBytes(width: Int, height: Int, rgbColor: Int): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                img.setRGB(x, y, rgbColor)
            }
        }
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "png", out)
        return out.toByteArray()
    }

    @Test
    fun `same imageRef in different packages resolves independently to correct files without cross-package collision`() {
        val bytesA = createTestImageBytes(10, 10, 0xFF0000) // Red
        val bytesB = createTestImageBytes(20, 20, 0x00FF00) // Green

        // Store same relative fileName "shared.png" in two different packages
        val assetA = storage.store("Package_A", "shared.png", bytesA)
        val assetB = storage.store("Package_B", "shared.png", bytesB)

        // Resolve TARGET from Package_A and SOURCE from Package_B
        val resolvedPathA = PackageMediaResolver.resolve("Package_A", "shared.png", storage)
        val resolvedPathB = PackageMediaResolver.resolve("Package_B", "shared.png", storage)

        assertNotNull(resolvedPathA)
        assertNotNull(resolvedPathB)
        assertNotEquals(resolvedPathA, resolvedPathB)
        assertTrue(resolvedPathA.toString().contains("Package_A"))
        assertTrue(resolvedPathB.toString().contains("Package_B"))
        assertEquals(Files.size(resolvedPathA), bytesA.size.toLong())
        assertEquals(Files.size(resolvedPathB), bytesB.size.toLong())

        // Thumbnail loader loads independent bitmaps
        val loader = LessonThumbnailLoader(storage)
        val thumbA = loader.load("shared.png", "Package_A") as ThumbnailResult.Ready
        val thumbB = loader.load("shared.png", "Package_B") as ThumbnailResult.Ready

        assertNotNull(thumbA.bitmap)
        assertNotNull(thumbB.bitmap)
        assertNotEquals(thumbA.bitmap, thumbB.bitmap)
    }

    @Test
    fun `switching candidates from different source packages sharing same filename updates preview correctly`() {
        val bytesElem = createTestImageBytes(15, 15, 0x0000FF) // Blue
        val bytesInter = createTestImageBytes(25, 25, 0xFFFF00) // Yellow

        storage.store("Vocabulary_In_Use_Elementary", "belt.png", bytesElem)
        storage.store("Vocabulary_In_Use_Intermediate", "belt.png", bytesInter)

        val candidate1 = ImageReuseSourceCandidate(
            sourcePackageId = "src_elem",
            sourcePackageName = "Vocabulary_In_Use_Elementary",
            sourceContentId = "c1",
            question = "belt",
            answer = "thắt lưng",
            translation = "thắt lưng",
            exampleText = "He wears a belt.",
            partOfSpeech = "noun",
            imageRef = "belt.png"
        )

        val candidate2 = ImageReuseSourceCandidate(
            sourcePackageId = "src_inter",
            sourcePackageName = "Vocabulary_In_Use_Intermediate",
            sourceContentId = "c2",
            question = "belt",
            answer = "dây đai",
            translation = "dây đai",
            exampleText = "Fasten your seat belt.",
            partOfSpeech = "noun",
            imageRef = "belt.png"
        )

        val resolvedCandidate1 = PackageMediaResolver.resolve(candidate1.sourcePackageName, candidate1.imageRef, storage)
        val resolvedCandidate2 = PackageMediaResolver.resolve(candidate2.sourcePackageName, candidate2.imageRef, storage)

        assertNotNull(resolvedCandidate1)
        assertNotNull(resolvedCandidate2)
        assertTrue(resolvedCandidate1.toString().contains("Vocabulary_In_Use_Elementary"))
        assertTrue(resolvedCandidate2.toString().contains("Vocabulary_In_Use_Intermediate"))
        assertNotEquals(resolvedCandidate1, resolvedCandidate2)
    }

    @Test
    fun `Previous Item navigation resolves target and source from their respective package identities`() {
        val bytesTarget1 = createTestImageBytes(12, 12, 0x111111)
        val bytesTarget2 = createTestImageBytes(14, 14, 0x222222)
        val bytesSource1 = createTestImageBytes(16, 16, 0x333333)

        storage.store("Target_Package", "target1.png", bytesTarget1)
        storage.store("Target_Package", "target2.png", bytesTarget2)
        storage.store("Source_Package", "source1.png", bytesSource1)

        val targetItem1 = ImageReuseTargetItem(
            targetContentId = "t1",
            targetLesson = "Unit 1",
            question = "item 1",
            answer = "nghĩa 1",
            translation = "nghĩa 1",
            exampleText = "ex 1",
            partOfSpeech = "noun",
            currentImageRef = "target1.png",
            candidates = listOf(
                ImageReuseSourceCandidate(
                    sourcePackageId = "src_1",
                    sourcePackageName = "Source_Package",
                    sourceContentId = "sc1",
                    question = "item 1",
                    answer = "nghĩa 1",
                    translation = "nghĩa 1",
                    exampleText = "ex 1",
                    partOfSpeech = "noun",
                    imageRef = "source1.png"
                )
            )
        )

        // Target 1 preview resolution
        val targetPath = PackageMediaResolver.resolve("Target_Package", targetItem1.currentImageRef, storage)
        val sourcePath = PackageMediaResolver.resolve(targetItem1.candidates[0].sourcePackageName, targetItem1.candidates[0].imageRef, storage)

        assertNotNull(targetPath)
        assertNotNull(sourcePath)
        assertTrue(targetPath.toString().contains("Target_Package"))
        assertTrue(sourcePath.toString().contains("Source_Package"))
    }

    @Test
    fun `Undo Last Use navigation preserves independent target and source package resolution`() {
        val bytesPriorTarget = createTestImageBytes(18, 18, 0x444444)
        val bytesSource = createTestImageBytes(22, 22, 0x555555)

        storage.store("Target_Package", "old_photo.png", bytesPriorTarget)
        storage.store("Source_Package", "source_photo.png", bytesSource)

        val candidate = ImageReuseSourceCandidate(
            sourcePackageId = "src_1",
            sourcePackageName = "Source_Package",
            sourceContentId = "sc1",
            question = "photo",
            answer = "ảnh",
            translation = "ảnh",
            exampleText = "take a photo",
            partOfSpeech = "noun",
            imageRef = "source_photo.png"
        )

        // After undo, target restored to "old_photo.png"
        val restoredTargetImageRef = "old_photo.png"
        val resolvedRestoredTarget = PackageMediaResolver.resolve("Target_Package", restoredTargetImageRef, storage)
        val resolvedSource = PackageMediaResolver.resolve(candidate.sourcePackageName, candidate.imageRef, storage)

        assertNotNull(resolvedRestoredTarget)
        assertNotNull(resolvedSource)
        assertTrue(resolvedRestoredTarget.toString().contains("Target_Package"))
        assertTrue(resolvedSource.toString().contains("Source_Package"))
        assertNotEquals(resolvedRestoredTarget, resolvedSource)
    }
}
