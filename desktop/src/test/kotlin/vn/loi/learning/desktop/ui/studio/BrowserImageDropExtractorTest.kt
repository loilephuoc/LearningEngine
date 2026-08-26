package vn.loi.learning.desktop.ui.studio

import com.sun.net.httpserver.HttpServer
import java.awt.Color
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.net.InetSocketAddress
import java.util.Base64
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

class BrowserImageDropExtractorTest {
    @Test
    fun `local file flavor wins over browser payload and is never deleted`() = runBlocking {
        val local = File.createTempFile("browser-drop-local-", ".png")
        ImageIO.write(image(), "png", local)
        try {
            val transferable = FakeTransferable(
                DataFlavor.javaFileListFlavor to listOf(local),
                htmlFlavor to "<img src=\"https://example.test/ignored.png\">"
            )
            BrowserImageDropExtractor().extractOwned(transferable).use { extracted ->
                assertEquals(local, extracted.file)
            }
            assertTrue(local.exists())
        } finally {
            local.delete()
        }
    }

    @Test
    fun `local file URI remains supported`() = runBlocking {
        val local = File.createTempFile("browser-drop-uri-", ".jpg")
        ImageIO.write(image(), "jpg", local)
        try {
            val uriFlavor = DataFlavor("text/uri-list;class=java.lang.String")
            BrowserImageDropExtractor().extractOwned(FakeTransferable(uriFlavor to local.toURI().toString())).use {
                assertEquals(local.canonicalFile, it.file.canonicalFile)
            }
        } finally {
            local.delete()
        }
    }

    @Test
    fun `HTML image URL and uri-list HTTPS URL are identified`() {
        val extractor = BrowserImageDropExtractor()
        assertEquals(
            "https://example.test/image.png?token=secret",
            extractor.remoteCandidate(extractor.snapshot(FakeTransferable(htmlFlavor to "<img alt='x' src='https://example.test/image.png?token=secret'>")))
        )

        val uriFlavor = DataFlavor("text/uri-list;class=java.lang.String")
        assertEquals(
            "https://example.test/from-uri.jpg",
            extractor.remoteCandidate(extractor.snapshot(FakeTransferable(uriFlavor to "#comment\nhttps://example.test/from-uri.jpg")))
        )
    }

    @Test
    fun `HTML data URL and direct image flavor materialize temporary PNG files`() = runBlocking {
        val bytes = pngBytes()
        val dataUrl = "data:image/png;base64,${Base64.getEncoder().encodeToString(bytes)}"
        val extractor = BrowserImageDropExtractor()

        val dataExtracted = extractor.extractOwned(FakeTransferable(htmlFlavor to "<img src=\"$dataUrl\">"))
        val dataPath = dataExtracted.file.toPath()
        assertTrue(ImageIO.read(dataExtracted.file) != null)
        dataExtracted.close()
        assertFalse(dataPath.toFile().exists())

        val imageExtracted = extractor.extractOwned(FakeTransferable(DataFlavor.imageFlavor to image()))
        val imagePath = imageExtracted.file.toPath()
        assertTrue(ImageIO.read(imageExtracted.file) != null)
        imageExtracted.close()
        assertFalse(imagePath.toFile().exists())

        val streamFlavor = DataFlavor("image/png;class=java.io.InputStream")
        BrowserImageDropExtractor().extractOwned(
            FakeTransferable(streamFlavor to ByteArrayInputStream(bytes))
        ).use { assertTrue(ImageIO.read(it.file) != null) }
    }

    @Test
    fun `unsupported transferable fails clearly`() = runBlocking {
        val failure = assertFailsWith<BrowserImageDropException> {
            BrowserImageDropExtractor().extractOwned(FakeTransferable(DataFlavor.stringFlavor to "not an image"))
        }
        assertTrue(failure.message!!.contains("supported image"))
    }

    @Test
    fun `async extraction never accesses an expired AWT transferable`() = runBlocking {
        val transferable = ExpiringTransferable(DataFlavor.imageFlavor to image())
        val extractor = BrowserImageDropExtractor()

        val snapshot = extractor.snapshot(transferable)
        transferable.invalidate()

        extractor.extract(snapshot).use { extracted ->
            assertTrue(ImageIO.read(extracted.file) != null)
        }
        assertEquals(2, transferable.accessCount, "Only flavor enumeration and synchronous data copy are allowed")
    }

    @Test
    fun `HTTP image succeeds while non-image and oversized responses are rejected`() = withServer { server, base ->
        server.createContext("/image") { exchange -> respond(exchange, 200, "image/png", pngBytes()) }
        server.createContext("/text") { exchange -> respond(exchange, 200, "text/plain", "nope".toByteArray()) }
        server.createContext("/large") { exchange -> respond(exchange, 200, "image/png", ByteArray(2048)) }
        server.createContext("/denied") { exchange -> respond(exchange, 403, "text/plain", ByteArray(0)) }
        server.start()

        runBlocking {
            BrowserImageDropExtractor().extractOwned(urlTransferable("$base/image")).use {
                assertTrue(ImageIO.read(it.file) != null)
            }
            assertFailsWith<BrowserImageDropException> {
                BrowserImageDropExtractor().extractOwned(urlTransferable("$base/text"))
            }
            assertFailsWith<BrowserImageDropException> {
                BrowserImageDropExtractor(maximumBytes = 1024).extractOwned(urlTransferable("$base/large"))
            }
            val denied = assertFailsWith<BrowserImageDropException> {
                BrowserImageDropExtractor().extractOwned(urlTransferable("$base/denied"))
            }
            assertTrue(denied.message!!.contains("Ctrl+V"))
        }
    }

    @Test
    fun `HTTP timeout and redirect bound terminate deterministically`() = withServer { server, base ->
        server.createContext("/slow") { exchange ->
            Thread.sleep(250)
            runCatching { respond(exchange, 200, "image/png", pngBytes()) }
        }
        server.createContext("/loop") { exchange ->
            exchange.responseHeaders.add("Location", "/loop")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.start()

        runBlocking {
            val timeout = assertFailsWith<BrowserImageDropException> {
                BrowserImageDropExtractor(readTimeoutMillis = 50).extractOwned(urlTransferable("$base/slow"))
            }
            assertTrue(timeout.message!!.contains("timed out"))
            val redirect = assertFailsWith<BrowserImageDropException> {
                BrowserImageDropExtractor(maximumRedirects = 2).extractOwned(urlTransferable("$base/loop"))
            }
            assertTrue(redirect.message!!.contains("redirect limit"))
        }
    }

    @Test
    fun `repeated browser URL imports receive distinct canonical media references`() = withServer { server, base ->
        server.createContext("/same-name.png") { exchange -> respond(exchange, 200, "image/png", pngBytes()) }
        server.start()
        val root = createTempDirectory("browser-duplicate-import-")
        try {
            val context = LearningApplicationFactory.createInMemory()
            val service = ContentBrowserEditService(context.contentRepository!!)
            val storage = JvmContentMediaStorage(root)
            val refs = runBlocking {
                (1..16).map {
                    BrowserImageDropExtractor().extractOwned(urlTransferable("$base/same-name.png")).use { extracted ->
                        service.importMediaAsset("Browser Package", extracted.file, storage)
                    }
                }
            }
            assertEquals(16, refs.toSet().size)
            assertTrue(refs.all { it.endsWith(".jpg") && storage.exists(it) })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun urlTransferable(url: String): Transferable =
        FakeTransferable(DataFlavor("text/uri-list;class=java.lang.String") to url)

    private fun withServer(block: (HttpServer, String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        try {
            block(server, "http://127.0.0.1:${server.address.port}")
        } finally {
            server.stop(0)
        }
    }

    private fun respond(exchange: com.sun.net.httpserver.HttpExchange, status: Int, type: String, bytes: ByteArray) {
        exchange.responseHeaders.add("Content-Type", type)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun image(): BufferedImage = BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB).also {
        val graphics = it.createGraphics()
        try {
            graphics.color = Color(60, 120, 180)
            graphics.fillRect(0, 0, it.width, it.height)
        } finally {
            graphics.dispose()
        }
    }

    private fun pngBytes(): ByteArray = ByteArrayOutputStream().use {
        ImageIO.write(image(), "png", it)
        it.toByteArray()
    }

    private class FakeTransferable(vararg entries: Pair<DataFlavor, Any>) : Transferable {
        private val values = entries.toMap()
        override fun getTransferDataFlavors(): Array<DataFlavor> = values.keys.toTypedArray()
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor in values
        override fun getTransferData(flavor: DataFlavor): Any =
            values[flavor] ?: throw UnsupportedFlavorException(flavor)
    }

    private class ExpiringTransferable(private val entry: Pair<DataFlavor, Any>) : Transferable {
        private var valid = true
        var accessCount: Int = 0
            private set

        fun invalidate() {
            valid = false
        }

        private fun checkValid() {
            check(valid) { "Transferable was accessed after the AWT drop callback ended" }
            accessCount++
        }

        override fun getTransferDataFlavors(): Array<DataFlavor> {
            checkValid()
            return arrayOf(entry.first)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            checkValid()
            return flavor == entry.first
        }

        override fun getTransferData(flavor: DataFlavor): Any {
            checkValid()
            if (flavor != entry.first) throw UnsupportedFlavorException(flavor)
            return entry.second
        }
    }

    private companion object {
        val htmlFlavor = DataFlavor("text/html;class=java.lang.String")
    }
}

private suspend fun BrowserImageDropExtractor.extractOwned(transferable: Transferable): ExtractedDroppedImage =
    extract(snapshot(transferable))
