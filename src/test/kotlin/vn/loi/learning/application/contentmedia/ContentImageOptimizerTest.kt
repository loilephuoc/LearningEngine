package vn.loi.learning.application.contentmedia

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ContentImageOptimizerTest {
    private val optimizer = ContentImageOptimizer()

    @Test
    fun `large PNG becomes a materially smaller valid JPEG`() = withTempImage("png") { file ->
        val image = noisyImage(2400, 1800)
        ImageIO.write(image, "png", file)

        val result = optimizer.optimize(file)
        val decoded = ImageIO.read(ByteArrayInputStream(result.bytes))

        assertEquals("jpg", result.extension)
        assertTrue(result.bytes.size < file.length() / 3)
        assertTrue(maxOf(decoded.width, decoded.height) in 1200..1600)
        assertEquals(4.0 / 3.0, decoded.width.toDouble() / decoded.height, 0.01)
    }

    @Test
    fun `large JPEG can be recompressed and verified`() = withTempImage("jpg") { file ->
        ImageIO.write(noisyImage(2200, 1500), "jpg", file)
        val result = optimizer.optimize(file)
        assertTrue(result.bytes.isNotEmpty())
        assertTrue(ImageIO.read(ByteArrayInputStream(result.bytes)) != null)
        assertEquals(result.bytes.size.toLong(), result.optimizedSizeBytes)
    }

    @Test
    fun `transparent PNG is flattened onto white instead of black`() = withTempImage("png") { file ->
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(50, 50, Color.RED.rgb)
        ImageIO.write(image, "png", file)
        val decoded = ImageIO.read(ByteArrayInputStream(optimizer.optimize(file).bytes))
        val corner = Color(decoded.getRGB(0, 0))
        assertTrue(corner.red > 240 && corner.green > 240 && corner.blue > 240)
    }

    @Test
    fun `small image is not upscaled or downscaled`() = withTempImage("png") { file ->
        ImageIO.write(noisyImage(640, 480), "png", file)
        val result = optimizer.optimize(file)
        assertEquals(640, result.width)
        assertEquals(480, result.height)
    }

    @Test
    fun `huge image respects long-side cap and aspect ratio`() = withTempImage("png") { file ->
        ImageIO.write(noisyImage(3200, 1800), "png", file)
        val result = optimizer.optimize(file)
        assertTrue(maxOf(result.width, result.height) in 1200..1600)
        assertTrue(abs(result.width.toDouble() / result.height - 16.0 / 9.0) < 0.01)
    }

    @Test
    fun `corrupt nominal image fails cleanly`() = withTempImage("jpg") { file ->
        file.writeBytes(byteArrayOf(1, 2, 3, 4))
        assertFailsWith<IllegalArgumentException> { optimizer.optimize(file) }
    }

    private fun noisyImage(width: Int, height: Int): BufferedImage {
        val random = Random(42)
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also { image ->
            for (y in 0 until height) for (x in 0 until width) {
                val base = (x * 255 / width)
                image.setRGB(x, y, Color(base, y * 255 / height, random.nextInt(256)).rgb)
            }
        }
    }

    private fun withTempImage(extension: String, block: (File) -> Unit) {
        val root = createTempDirectory("content-image-optimizer-")
        try {
            block(root.resolve("source.$extension").toFile())
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
