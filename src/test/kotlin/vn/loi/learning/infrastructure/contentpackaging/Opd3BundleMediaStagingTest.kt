package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import java.util.Comparator
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

class Opd3BundleMediaStagingTest {
    @Test
    fun `prepared media does not touch canonical bytes and rollback preserves old file`() {
        val root = Files.createTempDirectory("prepared-media-old-")
        try {
            val mediaRoot = root.resolve("media")
            val storage = JvmContentMediaStorage(mediaRoot)
            val old = byteArrayOf(1, 2, 3, 4)
            val fresh = byteArrayOf(9, 8, 7, 6)
            val oldAsset = storage.store("My_Package", "audio/foo.mp3", old)
            val archive = packageArchive(root, fresh)

            val prepared = Opd3BundleMediaExtractor(mediaStorage = storage).prepare(archive, "My Package")
            assertContentEquals(old, Files.readAllBytes(requireNotNull(storage.resolve(oldAsset.relativePath))))
            prepared.rollback()
            assertContentEquals(old, Files.readAllBytes(requireNotNull(storage.resolve(oldAsset.relativePath))))
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun `different bytes collision rejects publish and preserves canonical bytes`() {
        val root = Files.createTempDirectory("prepared-media-collision-")
        try {
            val storage = JvmContentMediaStorage(root.resolve("media"))
            val old = byteArrayOf(11, 12, 13)
            val asset = storage.store("My_Package", "audio/foo.mp3", old)
            val prepared = Opd3BundleMediaExtractor(mediaStorage = storage)
                .prepare(packageArchive(root, byteArrayOf(21, 22, 23)), "My Package")

            assertFailsWith<IllegalArgumentException> { prepared.commit() }
            assertContentEquals(old, Files.readAllBytes(requireNotNull(storage.resolve(asset.relativePath))))
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun `exact bytes collision is idempotent and new media publishes only on commit`() {
        val root = Files.createTempDirectory("prepared-media-idempotent-")
        try {
            val storage = JvmContentMediaStorage(root.resolve("media"))
            val bytes = byteArrayOf(31, 32, 33)
            val archive = packageArchive(root, bytes)
            val first = Opd3BundleMediaExtractor(mediaStorage = storage).prepare(archive, "My Package")
            assertNull(storage.resolve("My_Package/audio/foo.mp3"))
            first.commit()
            val path = requireNotNull(storage.resolve("My_Package/audio/foo.mp3"))
            assertContentEquals(bytes, Files.readAllBytes(path))

            Opd3BundleMediaExtractor(mediaStorage = storage).prepare(archive, "My Package").commit()
            assertContentEquals(bytes, Files.readAllBytes(path))
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun `later staging write failure cleans attempt directory and leaves canonical media unchanged`() {
        val root = Files.createTempDirectory("prepared-media-write-failure-")
        val staging = root.resolve("attempt")
        try {
            val storage = JvmContentMediaStorage(root.resolve("media"))
            val old = byteArrayOf(41, 42)
            val oldAsset = storage.store("My_Package", "audio/foo.mp3", old)
            val archive = packageArchive(root, byteArrayOf(51), byteArrayOf(61))
            var writes = 0
            val extractor = Opd3BundleMediaExtractor(
                mediaStorage = storage,
                stagingDirectoryFactory = { staging },
                stagedFileWriter = { path, bytes ->
                    writes += 1
                    if (writes == 2) error("injected later staging write failure")
                    Files.write(path, bytes)
                }
            )

            assertFailsWith<IllegalStateException> { extractor.prepare(archive, "My Package") }
            assertFalse(Files.exists(staging))
            assertContentEquals(old, Files.readAllBytes(requireNotNull(storage.resolve(oldAsset.relativePath))))
        } finally {
            deleteTree(root)
        }
    }

    @Test
    fun `staging directory creation failure leaves canonical state untouched`() {
        val root = Files.createTempDirectory("prepared-media-directory-failure-")
        try {
            val storage = JvmContentMediaStorage(root.resolve("media"))
            val old = byteArrayOf(71, 72)
            val oldAsset = storage.store("My_Package", "audio/foo.mp3", old)
            val extractor = Opd3BundleMediaExtractor(
                mediaStorage = storage,
                stagingDirectoryFactory = { error("injected staging directory failure") }
            )
            assertFailsWith<IllegalStateException> {
                extractor.prepare(packageArchive(root, byteArrayOf(81)), "My Package")
            }
            assertContentEquals(old, Files.readAllBytes(requireNotNull(storage.resolve(oldAsset.relativePath))))
        } finally {
            deleteTree(root)
        }
    }

    private fun packageArchive(root: java.nio.file.Path, bytes: ByteArray): java.nio.file.Path {
        return packageArchive(root, bytes, null)
    }

    private fun packageArchive(root: java.nio.file.Path, first: ByteArray, second: ByteArray?): java.nio.file.Path {
        val archive = Files.createTempFile(root, "media-", ".opd3")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            zip.putNextEntry(ZipEntry("media/My_Package/audio/foo.mp3"))
            zip.write(first)
            zip.closeEntry()
            second?.let {
                zip.putNextEntry(ZipEntry("media/My_Package/audio/bar.mp3"))
                zip.write(it)
                zip.closeEntry()
            }
        }
        return archive
    }

    private fun deleteTree(root: java.nio.file.Path) {
        if (Files.exists(root)) Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
