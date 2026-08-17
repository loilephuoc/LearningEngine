package vn.loi.learning.desktop.ui.contentlibrary

import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage

class LessonThumbnailLoaderTest {
    @Test
    fun `thumbnail decode is bounded cached and missing media is safe`() {
        val directory = Files.createTempDirectory("thumbnail-loader")
        val image = directory.resolve("large.png")
        try {
            ImageIO.write(BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB), "png", image.toFile())
            val storage = RecordingStorage(image)
            val loader = LessonThumbnailLoader(storage, maximumDimension = 96, maximumEntries = 2)
            val first = assertIs<ThumbnailResult.Ready>(loader.load("large.png"))
            assertTrue(first.bitmap.width <= 96)
            assertTrue(first.bitmap.height <= 96)
            assertIs<ThumbnailResult.Ready>(loader.load("large.png"))
            assertEquals(2, storage.resolveCount)
            assertIs<ThumbnailResult.Unavailable>(loader.load("missing.png"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `same logical reference reloads when physical file is modified`() {
        val directory = Files.createTempDirectory("thumbnail-reload")
        val image = directory.resolve("item.png")
        try {
            ImageIO.write(BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB), "png", image.toFile())
            val storage = RecordingStorage(image)
            val loader = LessonThumbnailLoader(storage, maximumDimension = 96, maximumEntries = 10)
            val first = assertIs<ThumbnailResult.Ready>(loader.load("large.png"))
            val firstWidth = first.bitmap.width

            // Modify physical file with different dimensions and lastModified
            Thread.sleep(20) // Ensure lastModified changes
            ImageIO.write(BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB), "png", image.toFile())
            Files.setLastModifiedTime(image, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 1000))

            val reloaded = assertIs<ThumbnailResult.Ready>(loader.load("large.png"))
            // Reloaded thumbnail reflects new aspect ratio
            assertTrue(reloaded.bitmap.height < firstWidth)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `no_image sentinel returns Unavailable immediately without resolving`() {
        val directory = Files.createTempDirectory("thumbnail-sentinel")
        val image = directory.resolve("item.png")
        try {
            val storage = RecordingStorage(image)
            val loader = LessonThumbnailLoader(storage, maximumDimension = 96, maximumEntries = 10)
            assertIs<ThumbnailResult.Unavailable>(loader.load("no_image.jpg"))
            assertIs<ThumbnailResult.Unavailable>(loader.load("no_image.png"))
            assertEquals(0, storage.resolveCount)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private class RecordingStorage(private val image: java.nio.file.Path) : ContentMediaStorage {
        var resolveCount = 0
        override fun resolve(relativePath: String): java.nio.file.Path? {
            resolveCount++
            return image.takeIf { relativePath == "large.png" }
        }
        override fun exists(relativePath: String) = resolve(relativePath) != null
        override fun store(packageName: String, fileName: String, content: ByteArray) =
            ContentMediaAsset(packageName, fileName, fileName)
    }
}
