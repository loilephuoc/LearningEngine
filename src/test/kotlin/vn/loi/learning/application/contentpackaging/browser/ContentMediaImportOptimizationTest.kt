package vn.loi.learning.application.contentpackaging.browser

import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

class ContentMediaImportOptimizationTest {
    private val service = ContentBrowserEditService(object : ContentRepository {
        override fun findById(contentId: ContentId): Content? = null
        override fun findAll(): List<Content> = emptyList()
        override fun save(content: Content) = Unit
        override fun deleteById(contentId: ContentId) = Unit
    })

    @Test
    fun `repeated image imports with identical basenames have distinct canonical JPEG paths`() {
        val root = createTempDirectory("duplicate-image-import-")
        try {
            val storage = JvmContentMediaStorage(root.resolve("media"))
            val refs = (1..24).map { index ->
                val source = root.resolve("source-$index").createDirectories()
                    .resolve("Gemini_Generated_Image.png").toFile()
                ImageIO.write(realImage(index), "png", source)
                service.importMediaAsset("Test Package", source, storage)
            }

            assertEquals(refs.size, refs.toSet().size)
            assertTrue(refs.all { it.endsWith(".jpg") })
            refs.forEach { ref -> assertNotNull(ImageIO.read(storage.resolve(ref)!!.toFile())) }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `corrupt image and unsupported extension fail before storage`() {
        val root = createTempDirectory("invalid-image-import-")
        try {
            val storageRoot = root.resolve("media")
            val storage = JvmContentMediaStorage(storageRoot)
            val corrupt = root.resolve("corrupt.jpg").toFile().apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val unsupported = root.resolve("image.gif").toFile().apply { writeBytes(byteArrayOf(4, 5, 6)) }

            assertFailsWith<IllegalArgumentException> { service.importMediaAsset("Pkg", corrupt, storage) }
            assertFailsWith<IllegalArgumentException> { service.importMediaAsset("Pkg", unsupported, storage) }
            assertEquals(0L, if (Files.exists(storageRoot)) Files.walk(storageRoot).use { it.filter(Files::isRegularFile).count() } else 0L)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `audio import remains byte exact`() {
        val root = createTempDirectory("audio-import-regression-")
        try {
            val storage = JvmContentMediaStorage(root.resolve("media"))
            val expected = byteArrayOf(9, 8, 7, 6, 5)
            val source = root.resolve("voice.mp3").toFile().apply { writeBytes(expected) }
            val ref = service.importMediaAsset("Pkg", source, storage)
            assertContentEquals(expected, Files.readAllBytes(storage.resolve(ref)!!))
            assertTrue(ref.endsWith("_voice.mp3"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun realImage(seed: Int): BufferedImage =
        BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB).also { image ->
            val graphics = image.createGraphics()
            try {
                graphics.color = Color(seed * 7 % 255, seed * 13 % 255, seed * 19 % 255)
                graphics.fillRect(0, 0, image.width, image.height)
            } finally {
                graphics.dispose()
            }
        }
}
