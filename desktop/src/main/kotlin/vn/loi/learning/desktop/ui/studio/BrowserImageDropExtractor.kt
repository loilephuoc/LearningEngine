package vn.loi.learning.desktop.ui.studio

import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.ByteBuffer
import java.util.Base64
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class BrowserImageDropException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

internal sealed interface BrowserDropPayload {
    data class LocalFiles(val files: List<File>) : BrowserDropPayload
    data class ImagePixels(val image: BufferedImage) : BrowserDropPayload
    data class ImageBytes(val bytes: ByteArray) : BrowserDropPayload
    data class Html(val value: String) : BrowserDropPayload
    data class UriList(val value: String) : BrowserDropPayload
    data class Text(val value: String) : BrowserDropPayload
}

internal data class BrowserDropSnapshot(
    val payloads: List<BrowserDropPayload>,
    val flavorSummary: List<String>
)

internal data class ExtractedDroppedImage(
    val file: File,
    private val temporary: Boolean
) : java.io.Closeable {
    override fun close() {
        if (temporary) file.delete()
    }
}

/** Acquires browser drag payloads and materializes them for the canonical file import pipeline. */
internal class BrowserImageDropExtractor(
    private val connectTimeoutMillis: Int = 7_000,
    private val readTimeoutMillis: Int = 15_000,
    private val maximumBytes: Int = 25 * 1024 * 1024,
    private val maximumRedirects: Int = 4
) {
    /** Must be called synchronously while the AWT drop callback still owns the Transferable. */
    fun snapshot(transferable: Transferable): BrowserDropSnapshot {
        val flavors = try {
            transferable.transferDataFlavors.orEmpty()
        } catch (failure: Exception) {
            throw BrowserImageDropException("Could not read image from browser drag.", failure)
        }
        val summary = flavors.map(::describeFlavor)
        val payloads = mutableListOf<BrowserDropPayload>()
        var relevantFailure: BrowserImageDropException? = null

        flavors.firstOrNull { it == DataFlavor.javaFileListFlavor }?.let { fileFlavor ->
            try {
                val files = (transferable.getTransferData(fileFlavor) as? List<*>)
                    ?.filterIsInstance<File>()?.toList().orEmpty()
                if (files.any(DragDropUtils::isSupportedImage)) {
                    return BrowserDropSnapshot(listOf(BrowserDropPayload.LocalFiles(files)), summary)
                }
                if (files.isNotEmpty()) payloads += BrowserDropPayload.LocalFiles(files)
            } catch (failure: Exception) {
                LOGGER.warning("Browser drop local-file snapshot failed: ${failure.javaClass.simpleName}")
            }
        }

        for (flavor in flavors) {
            if (flavor == DataFlavor.javaFileListFlavor) continue
            try {
                when {
                    flavor == DataFlavor.imageFlavor -> {
                        val value = transferable.getTransferData(flavor) as? Image
                        if (value != null) payloads += BrowserDropPayload.ImagePixels(copyImage(value))
                    }
                    flavor.primaryType.equals("image", true) &&
                        flavor.mimeType.substringBefore(';').lowercase() in SUPPORTED_MIME_TYPES -> {
                        when (val value = transferable.getTransferData(flavor)) {
                            is ByteArray -> payloads += BrowserDropPayload.ImageBytes(value.copyOf().also(::requireBoundedBytes))
                            is InputStream -> payloads += BrowserDropPayload.ImageBytes(readBounded(value))
                            is ByteBuffer -> {
                                val owned = value.asReadOnlyBuffer()
                                if (owned.remaining() > maximumBytes) {
                                    throw BrowserImageDropException("Clipboard image is too large.")
                                }
                                payloads += BrowserDropPayload.ImageBytes(ByteArray(owned.remaining()).also(owned::get))
                            }
                        }
                    }
                    flavor.primaryType.equals("text", true) && flavor.subType.equals("html", true) ->
                        payloads += BrowserDropPayload.Html(readTextBounded(transferable.getTransferData(flavor)))
                    flavor.primaryType.equals("text", true) && flavor.subType.equals("uri-list", true) ->
                        payloads += BrowserDropPayload.UriList(readTextBounded(transferable.getTransferData(flavor)))
                    flavor == DataFlavor.stringFlavor ->
                        payloads += BrowserDropPayload.Text(readTextBounded(transferable.getTransferData(flavor)))
                }
            } catch (failure: Exception) {
                if (failure is BrowserImageDropException) relevantFailure = failure
                LOGGER.warning("Browser drop flavor snapshot failed: ${describeFlavor(flavor)}; reason=${failure.javaClass.simpleName}")
            }
        }
        if (payloads.isEmpty()) {
            relevantFailure?.let { throw it }
            throw BrowserImageDropException("Dropped data does not contain a supported image.")
        }
        return BrowserDropSnapshot(payloads.toList(), summary)
    }

    suspend fun extract(snapshot: BrowserDropSnapshot): ExtractedDroppedImage = withContext(Dispatchers.IO) {
        try {
            snapshot.payloads.filterIsInstance<BrowserDropPayload.LocalFiles>()
                .flatMap { it.files }
                .firstOrNull(DragDropUtils::isSupportedImage)
                ?.let { return@withContext ExtractedDroppedImage(it, temporary = false) }

            snapshot.payloads.asSequence().mapNotNull(::localFileFromPayload)
                .firstOrNull(DragDropUtils::isSupportedImage)
                ?.let { return@withContext ExtractedDroppedImage(it, temporary = false) }

            snapshot.payloads.filterIsInstance<BrowserDropPayload.ImagePixels>().firstOrNull()
                ?.let { return@withContext materialize(it.image) }
            snapshot.payloads.filterIsInstance<BrowserDropPayload.ImageBytes>().firstOrNull()
                ?.let { return@withContext materialize(decodeImage(it.bytes)) }

            remoteCandidate(snapshot)?.let { candidate ->
                return@withContext acquireCandidate(candidate)
            }
            throw BrowserImageDropException("Dropped data does not contain a supported image.")
        } catch (failure: BrowserImageDropException) {
            logUnsupported(snapshot.flavorSummary, failure.message.orEmpty())
            throw failure
        } catch (failure: Exception) {
            logUnsupported(snapshot.flavorSummary, failure.javaClass.simpleName)
            throw BrowserImageDropException("Could not read image from browser drag.", failure)
        }
    }

    internal fun remoteCandidate(snapshot: BrowserDropSnapshot): String? {
        snapshot.payloads.filterIsInstance<BrowserDropPayload.Html>().forEach { payload ->
            val source = IMG_SOURCE.find(payload.value)?.groupValues?.get(2)?.trim().orEmpty()
            source.replace("&amp;", "&").takeIf { it.isNotBlank() }?.let { return it }
        }
        return snapshot.payloads.flatMap { payload ->
            when (payload) {
                is BrowserDropPayload.UriList -> payload.value.lines()
                is BrowserDropPayload.Text -> payload.value.lines()
                else -> emptyList()
            }
        }.map(String::trim)
            .firstOrNull { it.isNotBlank() && !it.startsWith("#") && (isRemoteUrl(it) || it.startsWith("data:image/", true)) }
    }

    internal fun hasImageCandidate(snapshot: BrowserDropSnapshot): Boolean =
        snapshot.payloads.filterIsInstance<BrowserDropPayload.LocalFiles>()
            .flatMap { it.files }.any(DragDropUtils::isSupportedImage) ||
            snapshot.payloads.any { it is BrowserDropPayload.ImagePixels || it is BrowserDropPayload.ImageBytes } ||
            snapshot.payloads.any { localFileFromPayload(it)?.let(DragDropUtils::isSupportedImage) == true } ||
            remoteCandidate(snapshot) != null

    private fun copyImage(image: Image): BufferedImage {
        val loaded = if (image is BufferedImage) image else ImageIcon(image).image
        val width = loaded.getWidth(null)
        val height = loaded.getHeight(null)
        if (width <= 0 || height <= 0) throw BrowserImageDropException("Could not read image from browser drag.")
        if (width.toLong() * height.toLong() > MAXIMUM_SNAPSHOT_PIXELS) {
            throw BrowserImageDropException("Dropped browser image dimensions are too large.")
        }
        return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { output ->
            val graphics = output.createGraphics()
            try {
                graphics.drawImage(loaded, 0, 0, null)
            } finally {
                graphics.dispose()
            }
        }
    }

    private fun acquireCandidate(candidate: String): ExtractedDroppedImage = when {
        candidate.startsWith("data:image/", true) -> materialize(decodeDataUrl(candidate))
        isRemoteUrl(candidate) -> materialize(download(candidate))
        else -> throw BrowserImageDropException("Dropped data does not contain a supported image.")
    }

    private fun decodeDataUrl(value: String): BufferedImage {
        val comma = value.indexOf(',')
        if (comma <= 0) throw BrowserImageDropException("Dropped image data URL is invalid.")
        val metadata = value.substring(5, comma).lowercase()
        if (!metadata.endsWith(";base64") || metadata.substringBefore(';') !in SUPPORTED_MIME_TYPES) {
            throw BrowserImageDropException("Dropped image data URL uses an unsupported format.")
        }
        val encoded = value.substring(comma + 1)
        if (encoded.length > maximumBytes * 4L / 3L + 4L) {
            throw BrowserImageDropException("Dropped browser image exceeds the 25 MiB limit.")
        }
        val bytes = try {
            Base64.getDecoder().decode(encoded)
        } catch (failure: IllegalArgumentException) {
            throw BrowserImageDropException("Dropped image data URL is invalid.", failure)
        }
        return decodeImage(bytes)
    }

    private fun download(rawUrl: String): BufferedImage {
        var current = URI(rawUrl)
        repeat(maximumRedirects + 1) { redirectCount ->
            val connection = current.toURL().openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = readTimeoutMillis
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "image/*")
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    if (redirectCount >= maximumRedirects) {
                        throw BrowserImageDropException("Dropped browser image URL exceeded the redirect limit.")
                    }
                    val location = connection.getHeaderField("Location")
                        ?: throw BrowserImageDropException("Dropped browser image URL returned an invalid redirect.")
                    current = current.resolve(location)
                    if (!isRemoteUrl(current.toString())) {
                        throw BrowserImageDropException("Dropped browser image URL redirected to an unsupported scheme.")
                    }
                    return@repeat
                }
                if (status !in 200..299) {
                    throw BrowserImageDropException(BROWSER_DRAG_FALLBACK_MESSAGE)
                }
                val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
                if (type != null && type !in SUPPORTED_MIME_TYPES) {
                    throw BrowserImageDropException("Dropped URL did not return a supported image.")
                }
                val declaredLength = connection.contentLengthLong
                if (declaredLength > maximumBytes) {
                    throw BrowserImageDropException("Dropped browser image exceeds the 25 MiB limit.")
                }
                return decodeImage(readBounded(connection.inputStream))
            } catch (failure: BrowserImageDropException) {
                throw failure
            } catch (failure: java.net.SocketTimeoutException) {
                throw BrowserImageDropException("$BROWSER_DRAG_FALLBACK_MESSAGE The request timed out.", failure)
            } catch (failure: Exception) {
                throw BrowserImageDropException(BROWSER_DRAG_FALLBACK_MESSAGE, failure)
            } finally {
                connection.disconnect()
            }
        }
        error("Redirect loop should have terminated.")
    }

    private fun readBounded(input: InputStream): ByteArray = input.use { source ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = source.read(buffer)
            if (read < 0) break
            total += read
            if (total > maximumBytes) throw BrowserImageDropException("Dropped browser image exceeds the 25 MiB limit.")
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    }

    private fun decodeImage(bytes: ByteArray): BufferedImage {
        if (bytes.isEmpty()) throw BrowserImageDropException("Dropped browser image contained no data.")
        return ImageIO.read(ByteArrayInputStream(bytes))
            ?: throw BrowserImageDropException("Dropped browser data could not be decoded as an image.")
    }

    private fun materialize(image: BufferedImage): ExtractedDroppedImage {
        val file = File.createTempFile("learning-engine-browser-image-", ".png")
        try {
            check(ImageIO.write(image, "png", file)) { "PNG writer is unavailable." }
            return ExtractedDroppedImage(file, temporary = true)
        } catch (failure: Exception) {
            file.delete()
            throw BrowserImageDropException("Could not materialize image from browser drag.", failure)
        }
    }

    private fun readTextBounded(value: Any?): String = when (value) {
        is String -> value.also(::requireBoundedText)
        is Reader -> value.use(::readReaderBounded)
        is InputStream -> value.bufferedReader(Charsets.UTF_8).use(::readReaderBounded)
        else -> ""
    }

    private fun readReaderBounded(reader: Reader): String {
        val output = StringBuilder()
        val buffer = CharArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = reader.read(buffer)
            if (read < 0) break
            if (output.length + read > maximumTextChars) {
                throw BrowserImageDropException("Dropped browser text payload is too large.")
            }
            output.append(buffer, 0, read)
        }
        return output.toString()
    }

    private fun requireBoundedText(value: String) {
        if (value.length > maximumTextChars) throw BrowserImageDropException("Dropped browser text payload is too large.")
    }

    private fun requireBoundedBytes(value: ByteArray) {
        if (value.size > maximumBytes) throw BrowserImageDropException("Dropped browser image exceeds the 25 MiB limit.")
    }

    private fun localFileFromPayload(payload: BrowserDropPayload): File? {
        val values = when (payload) {
            is BrowserDropPayload.UriList -> payload.value.lines()
            is BrowserDropPayload.Text -> payload.value.lines()
            else -> return null
        }
        return values.asSequence().map(String::trim).filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { raw ->
                runCatching {
                    val cleaned = raw.removeSurrounding("\"")
                    if (cleaned.startsWith("file:", true)) File(URI(cleaned)) else File(cleaned)
                }.getOrNull()
            }.firstOrNull { it.exists() && it.isFile }
    }

    private fun isRemoteUrl(value: String): Boolean = runCatching {
        URI(value).scheme?.lowercase() in setOf("http", "https")
    }.getOrDefault(false)

    private fun describeFlavor(flavor: DataFlavor): String =
        "${flavor.mimeType.substringBefore(';')}[${flavor.representationClass.name}]"

    private fun logUnsupported(flavors: List<String>, reason: String) {
        LOGGER.warning("Browser image drop rejected: $reason; flavors=${flavors.joinToString()}")
    }

    private companion object {
        val IMG_SOURCE = Regex("""<img\b[^>]*\bsrc\s*=\s*(['\"])(.*?)\1""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val SUPPORTED_MIME_TYPES = setOf("image/png", "image/jpeg", "image/webp")
        const val MAXIMUM_SNAPSHOT_PIXELS = 100_000_000L
        const val BROWSER_DRAG_FALLBACK_MESSAGE =
            "Direct browser image drag could not fetch this image. Try Copy image and Ctrl+V."
        val LOGGER: Logger = Logger.getLogger(BrowserImageDropExtractor::class.java.name)
    }

    private val maximumTextChars: Int = maximumBytes * 4 / 3 + 4096
}
