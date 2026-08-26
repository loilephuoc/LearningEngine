package vn.loi.learning.desktop.ui.studio

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.Base64
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClipboardImageExtractorTest {
    @Test
    fun `image flavor is owned before clipboard expires and materializes valid image`() = runBlocking {
        val transferable = ExpiringTransferable(DataFlavor.imageFlavor to image())
        val extractor = ClipboardImageExtractor()
        val ready = assertIs<ClipboardImageSnapshotResult.Ready>(extractor.snapshot(transferable))
        transferable.invalidate()

        extractor.extract(ready.snapshot).use { extracted ->
            assertTrue(ImageIO.read(extracted.file) != null)
        }
        assertEquals(2, transferable.accessCount)
    }

    @Test
    fun `image InputStream and ByteBuffer flavors are copied within bounds`() = runBlocking {
        val bytes = pngBytes()
        val flavor = DataFlavor("image/png;class=java.io.InputStream")
        val extractor = ClipboardImageExtractor()
        val stream = assertIs<ClipboardImageSnapshotResult.Ready>(
            extractor.snapshot(FakeTransferable(flavor to ByteArrayInputStream(bytes)))
        )
        extractor.extract(stream.snapshot).use { assertTrue(ImageIO.read(it.file) != null) }

        val bufferFlavor = DataFlavor("image/png;class=java.nio.ByteBuffer")
        val buffer = assertIs<ClipboardImageSnapshotResult.Ready>(
            extractor.snapshot(FakeTransferable(bufferFlavor to ByteBuffer.wrap(bytes)))
        )
        extractor.extract(buffer.snapshot).use { assertTrue(ImageIO.read(it.file) != null) }
    }

    @Test
    fun `clipboard data URL is decoded while plain text is not intercepted`() = runBlocking {
        val bytes = pngBytes()
        val dataUrl = "data:image/png;base64,${Base64.getEncoder().encodeToString(bytes)}"
        val extractor = ClipboardImageExtractor()
        val ready = assertIs<ClipboardImageSnapshotResult.Ready>(
            extractor.snapshot(FakeTransferable(DataFlavor.stringFlavor to dataUrl))
        )
        extractor.extract(ready.snapshot).use { assertTrue(ImageIO.read(it.file) != null) }

        assertIs<ClipboardImageSnapshotResult.NoImage>(
            extractor.snapshot(FakeTransferable(DataFlavor.stringFlavor to "ordinary pasted text"))
        )
        Unit
    }

    @Test
    fun `corrupt and oversized clipboard image payloads fail safely`() = runBlocking {
        val flavor = DataFlavor("image/png;class=java.io.InputStream")
        val extractor = ClipboardImageExtractor()
        val corrupt = assertIs<ClipboardImageSnapshotResult.Ready>(
            extractor.snapshot(FakeTransferable(flavor to ByteArrayInputStream(byteArrayOf(1, 2, 3))))
        )
        assertFailsWith<BrowserImageDropException> { extractor.extract(corrupt.snapshot) }

        val oversizedBrowser = BrowserImageDropExtractor(maximumBytes = 8)
        val oversizedExtractor = ClipboardImageExtractor(oversizedBrowser)
        val oversized = oversizedExtractor.snapshot(
            FakeTransferable(flavor to ByteArrayInputStream(ByteArray(9)))
        )
        assertIs<ClipboardImageSnapshotResult.Failure>(oversized)
        Unit
    }

    @Test
    fun `paste policy never steals Ctrl V from editable text fields`() {
        assertTrue(ContentStudioImagePastePolicy.shouldAttempt(true, true, false, true, true))
        assertEquals(false, ContentStudioImagePastePolicy.shouldAttempt(true, true, true, true, true))
        assertEquals(false, ContentStudioImagePastePolicy.shouldAttempt(true, true, false, false, true))
        assertEquals(false, ContentStudioImagePastePolicy.shouldAttempt(true, true, false, true, false))
        assertEquals(false, ContentStudioImagePastePolicy.shouldAttempt(false, true, false, true, true))
    }

    private fun image(): BufferedImage = BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB)

    private fun pngBytes(): ByteArray = ByteArrayOutputStream().use { output ->
        ImageIO.write(image(), "png", output)
        output.toByteArray()
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
        var accessCount = 0
            private set
        fun invalidate() { valid = false }
        private fun access() {
            check(valid) { "Clipboard Transferable accessed after snapshot" }
            accessCount++
        }
        override fun getTransferDataFlavors(): Array<DataFlavor> { access(); return arrayOf(entry.first) }
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean { access(); return flavor == entry.first }
        override fun getTransferData(flavor: DataFlavor): Any {
            access()
            if (flavor != entry.first) throw UnsupportedFlavorException(flavor)
            return entry.second
        }
    }
}
