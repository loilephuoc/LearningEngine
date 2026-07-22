package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import vn.loi.learning.application.port.ContentMediaStorage

class LessonThumbnailLoader(
    private val mediaStorage: ContentMediaStorage,
    private val maximumDimension: Int = 96,
    maximumEntries: Int = 64
) {
    private val cache = object : LinkedHashMap<String, ImageBitmap>(maximumEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
            size > maximumEntries
    }

    init {
        require(maximumDimension > 0)
        require(maximumEntries > 0)
    }

    fun load(reference: String): ThumbnailResult {
        synchronized(cache) { cache[reference] }?.let { return ThumbnailResult.Ready(it) }
        val path = mediaStorage.resolve(reference) ?: return ThumbnailResult.Unavailable
        return try {
            val source = readBounded(path) ?: return ThumbnailResult.Unavailable
            val scale = minOf(1.0, maximumDimension.toDouble() / maxOf(source.width, source.height))
            val width = maxOf(1, (source.width * scale).toInt())
            val height = maxOf(1, (source.height * scale).toInt())
            val thumbnail = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val graphics = thumbnail.createGraphics()
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                graphics.drawImage(source, 0, 0, width, height, null)
            } finally {
                graphics.dispose()
            }
            val encoded = ByteArrayOutputStream().use { output ->
                ImageIO.write(thumbnail, "png", output)
                output.toByteArray()
            }
            val bitmap = SkiaImage.makeFromEncoded(encoded).toComposeImageBitmap()
            synchronized(cache) { cache[reference] = bitmap }
            ThumbnailResult.Ready(bitmap)
        } catch (_: Exception) {
            ThumbnailResult.Unavailable
        }
    }

    private fun readBounded(path: java.nio.file.Path): BufferedImage? =
        ImageIO.createImageInputStream(path.toFile()).use { input ->
            val readers = ImageIO.getImageReaders(input)
            if (!readers.hasNext()) return null
            val reader = readers.next()
            try {
                reader.input = input
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                val sample = maxOf(1, kotlin.math.ceil(maxOf(width, height).toDouble() / maximumDimension).toInt())
                val parameters = reader.defaultReadParam
                parameters.setSourceSubsampling(sample, sample, 0, 0)
                reader.read(0, parameters)
            } finally {
                reader.dispose()
            }
        }
}

sealed interface ThumbnailResult {
    data object Loading : ThumbnailResult
    data class Ready(val bitmap: ImageBitmap) : ThumbnailResult
    data object Unavailable : ThumbnailResult
}

@Composable
fun LessonThumbnail(reference: String?, loader: LessonThumbnailLoader) {
    val result by produceState<ThumbnailResult>(ThumbnailResult.Loading, reference) {
        value = if (reference.isNullOrBlank()) ThumbnailResult.Unavailable
        else withContext(Dispatchers.IO) { loader.load(reference) }
    }
    Box(
        modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        when (val current = result) {
            ThumbnailResult.Loading -> Text("…")
            ThumbnailResult.Unavailable -> Text("No image")
            is ThumbnailResult.Ready -> Image(
                bitmap = current.bitmap,
                contentDescription = "Lesson thumbnail",
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}
