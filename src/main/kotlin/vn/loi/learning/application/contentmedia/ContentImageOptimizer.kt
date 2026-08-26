package vn.loi.learning.application.contentmedia

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

data class OptimizedContentImage(
    val bytes: ByteArray,
    val extension: String,
    val width: Int,
    val height: Int,
    val quality: Float,
    val originalSizeBytes: Long,
    val optimizedSizeBytes: Long
)

/** Canonicalizes imported Content images to verified, high-quality JPEG bytes. */
class ContentImageOptimizer(
    private val preferredQuality: Float = 0.92f,
    private val minimumQuality: Float = 0.80f,
    private val softTargetBytes: Int = 250 * 1024,
    private val maximumLongSide: Int = 1600,
    private val minimumLongSide: Int = 1200
) {
    fun optimize(sourceFile: File): OptimizedContentImage {
        require(sourceFile.exists() && sourceFile.isFile) {
            "Source file does not exist: ${sourceFile.absolutePath}"
        }
        val decoded = ImageIO.read(sourceFile)
            ?: throw IllegalArgumentException("Image cannot be decoded: ${sourceFile.name}")
        require(decoded.width > 0 && decoded.height > 0) { "Image has invalid dimensions." }

        var normalized = normalizeRgb(decoded)
        normalized = resizeToLongSide(normalized, minOf(longSide(normalized), maximumLongSide))

        var quality = preferredQuality
        var encoded = encodeJpeg(normalized, quality)
        while (encoded.size > softTargetBytes && quality - 0.03f >= minimumQuality) {
            quality = (quality - 0.03f).coerceAtLeast(minimumQuality)
            encoded = encodeJpeg(normalized, quality)
        }

        var targetLongSide = longSide(normalized)
        while (encoded.size > softTargetBytes && targetLongSide > minimumLongSide) {
            targetLongSide = maxOf(minimumLongSide, (targetLongSide * 0.90).toInt())
            normalized = resizeToLongSide(normalized, targetLongSide)
            encoded = encodeJpeg(normalized, quality)
        }

        val verified = ImageIO.read(ByteArrayInputStream(encoded))
            ?: throw IllegalStateException("Optimized JPEG verification failed.")
        check(verified.width == normalized.width && verified.height == normalized.height) {
            "Optimized JPEG dimensions failed verification."
        }
        return OptimizedContentImage(
            bytes = encoded,
            extension = "jpg",
            width = verified.width,
            height = verified.height,
            quality = quality,
            originalSizeBytes = sourceFile.length(),
            optimizedSizeBytes = encoded.size.toLong()
        )
    }

    private fun normalizeRgb(source: BufferedImage): BufferedImage =
        BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB).also { destination ->
            destination.createGraphics().use { graphics ->
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, destination.width, destination.height)
                graphics.drawImage(source, 0, 0, null)
            }
        }

    private fun resizeToLongSide(source: BufferedImage, requestedLongSide: Int): BufferedImage {
        val currentLongSide = longSide(source)
        if (requestedLongSide >= currentLongSide) return source
        val scale = requestedLongSide.toDouble() / currentLongSide
        val width = maxOf(1, (source.width * scale).toInt())
        val height = maxOf(1, (source.height * scale).toInt())
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also { destination ->
            destination.createGraphics().use { graphics ->
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                graphics.drawImage(source, 0, 0, width, height, null)
            }
        }
    }

    private fun encodeJpeg(image: BufferedImage, quality: Float): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().firstOrNull()
            ?: throw IllegalStateException("JPEG writer is unavailable.")
        return try {
            ByteArrayOutputStream().use { output ->
                ImageIO.createImageOutputStream(output).use { imageOutput ->
                    writer.output = imageOutput
                    val parameters = writer.defaultWriteParam.apply {
                        compressionMode = ImageWriteParam.MODE_EXPLICIT
                        compressionQuality = quality
                    }
                    writer.write(null, IIOImage(image, null, null), parameters)
                    imageOutput.flush()
                }
                output.toByteArray().also { check(it.isNotEmpty()) { "JPEG encoding produced no bytes." } }
            }
        } catch (failure: Exception) {
            throw IllegalStateException("JPEG encoding failed: ${failure.message}", failure)
        } finally {
            writer.dispose()
        }
    }

    private fun longSide(image: BufferedImage): Int = maxOf(image.width, image.height)
}

private inline fun <T : java.awt.Graphics2D, R> T.use(block: (T) -> R): R =
    try {
        block(this)
    } finally {
        dispose()
    }
