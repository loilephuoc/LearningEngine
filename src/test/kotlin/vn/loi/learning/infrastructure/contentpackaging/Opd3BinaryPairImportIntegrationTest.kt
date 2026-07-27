package vn.loi.learning.infrastructure.contentpackaging

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.CRC32
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.AmbiguousOpd3JsonPairException
import vn.loi.learning.application.contentpackaging.InvalidOpd3BinaryPackageException
import vn.loi.learning.application.contentpackaging.MissingOpd3JsonPairException
import vn.loi.learning.application.contentpackaging.PackageScanCandidate
import vn.loi.learning.application.contentpackaging.UnsupportedPackageTypeException
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentmedia.LegacyOpd3MediaArchiveReader

class Opd3BinaryPairImportIntegrationTest {

    @Test
    fun `desktop composition imports JSON OPD3 pair exposes media and starts learning`() {
        val root = Files.createTempDirectory("Learning Engine Unicode đường dẫn ")
        val persistence = root.resolve("user data")
        val packages = root.resolve("gói học")
        Files.createDirectories(packages)
        val json = packages.resolve("OPD_2nd.json")
        val pkg = packages.resolve("OPD_2nd.pkg")
        val audio = "audio/hello.mp3" to "audio-bytes".toByteArray()
        val image = "images/hello.png" to "image-bytes".toByteArray()

        try {
            Files.writeString(json, legacyJson(audio.first, image.first))
            Files.write(pkg, packageBytes(listOf(Entry(audio.first, 1, audio.second), Entry(image.first, 2, image.second))))

            val context = LearningApplicationFactory.createPersisted(persistence)
            val result = context.packageImporter(packages).importAllDetailed(PackageCatalogId("desktop-content-library"))

            assertEquals(1, result.successfulImports.size, result.failures.toString())
            assertTrue(result.failures.isEmpty())
            assertEquals("OPD3", result.successfulImports.single().contentPackage.descriptor.format)
            assertEquals(1, context.installedPackages.query().size)
            assertEquals(1, context.contentLibraries.query().size)
            assertEquals(5, context.contentLibraries.query().single().learningItemCount)
            assertTrue(Files.isRegularFile(persistence.resolve("media/OPD_2nd/${audio.first}")))
            assertTrue(Files.isRegularFile(persistence.resolve("media/OPD_2nd/${image.first}")))

            val session = context.engine.startSession(
                StartStudySessionCommand(
                    sessionId = SessionId("binary-pair-session"),
                    learnerId = LearnerId("binary-pair-learner"),
                    startedAt = Moment(1_000L),
                    policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 0)
                )
            )
            assertNotNull(context.engine.getNextSessionItem(session.id, Moment(1_001L)))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `binary package is content-routed and reimport repairs orphan content graph without duplicates`() {
        val root = Files.createTempDirectory("opd3-pair-routing")
        val packages = root.resolve("packages").also(Files::createDirectories)
        val pkg = packages.resolve("topic.pkg")
        try {
            Files.writeString(packages.resolve("topic.json"), legacyJson("", ""))
            Files.write(pkg, packageBytes(emptyList()))
            assertEquals(JvmPackageFormat.OPD3_BINARY_PAIR, JvmPackageFormatDetector().detect(pkg))

            val context = LearningApplicationFactory.createPersisted(root.resolve("data"))
            val first = context.packageImporter(packages).importAllDetailed(PackageCatalogId("catalog"))
            assertEquals(1, first.successfulImports.size, first.failures.toString())
            val second = context.packageImporter(packages).importAllDetailed(PackageCatalogId("catalog"))
            assertEquals(1, second.successfulImports.size, second.failures.toString())
            assertTrue(second.failures.isEmpty())
            assertEquals(1, context.installedPackages.query().size)
            assertEquals(1, context.contentPackageRepository!!.findAll().size)
            assertEquals(1, context.contentLibraryRepository!!.findAll().size)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `pair resolver requires deterministic same basename JSON`() {
        val root = Files.createTempDirectory("opd3-pair-match")
        val pkg = root.resolve("Topic.pkg")
        Files.write(pkg, packageBytes(emptyList()))
        try {
            kotlin.test.assertFailsWith<MissingOpd3JsonPairException> { JvmOpd3PairResolver().resolve(pkg) }
            Files.writeString(root.resolve("Different.json"), "[]")
            kotlin.test.assertFailsWith<MissingOpd3JsonPairException> { JvmOpd3PairResolver().resolve(pkg) }
            val json = root.resolve("topic.JSON")
            Files.writeString(json, "[]")
            assertEquals(json.toAbsolutePath().toString(), JvmOpd3PairResolver().resolve(pkg).jsonSource)

            val resolver = JvmOpd3PairResolver { listOf(json, root.resolve("TOPIC.json")) }
            kotlin.test.assertFailsWith<AmbiguousOpd3JsonPairException> { resolver.resolve(pkg) }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `detector rejects invalid signature without opening ZipFile`() {
        val file = Files.createTempFile("invalid-package", ".pkg")
        try {
            Files.write(file, "NOPE".toByteArray())
            val error = kotlin.test.assertFailsWith<UnsupportedPackageTypeException> {
                JvmPackageFormatDetector().detect(file)
            }
            assertTrue(error.message.orEmpty().contains("Unsupported package format"))
            assertTrue(!error.message.orEmpty().contains("zip END header"))
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun `binary reader rejects version truncation bounds CRC and media type`() {
        val root = Files.createTempDirectory("opd3-binary-invalid")
        val reader = LegacyOpd3MediaArchiveReader()
        try {
            val version = root.resolve("version.pkg")
            Files.write(version, packageBytes(emptyList(), version = 2))
            assertInvalid(reader, version, "version 2")

            val truncated = root.resolve("truncated.pkg")
            Files.write(truncated, packageBytes(listOf(Entry("a.mp3", 1, byteArrayOf(1)))).copyOf(20))
            assertInvalid(reader, truncated, "index")

            val bounds = root.resolve("bounds.pkg")
            val validBounds = packageBytes(listOf(Entry("a.mp3", 1, byteArrayOf(1))))
            Files.write(bounds, validBounds.copyOf(validBounds.size - 1))
            val boundsError = kotlin.test.assertFailsWith<IllegalArgumentException> { reader.readEntries(bounds) }
            assertTrue(boundsError.message.orEmpty().contains("bounds"))

            val crc = root.resolve("crc.pkg")
            val crcBytes = packageBytes(listOf(Entry("a.mp3", 1, byteArrayOf(1))))
            crcBytes[crcBytes.lastIndex] = 2
            Files.write(crc, crcBytes)
            val crcEntry = reader.readEntries(crc).single()
            assertTrue(kotlin.test.assertFailsWith<IllegalArgumentException> { reader.readBytes(crc, crcEntry) }.message.orEmpty().contains("CRC32"))

            val media = root.resolve("media.pkg")
            Files.write(media, packageBytes(listOf(Entry("a.bin", 9, byteArrayOf(1)))))
            assertInvalid(reader, media, "media type")
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun assertInvalid(reader: LegacyOpd3MediaArchiveReader, file: Path, text: String) {
        val error = kotlin.test.assertFailsWith<InvalidOpd3BinaryPackageException> { reader.readEntries(file) }
        assertTrue(error.message.orEmpty().contains(text, ignoreCase = true))
    }

    private fun legacyJson(audio: String, image: String): String =
        """
        [{
          "group":"Release Candidate", "section":"Import", "lesson":"Pair",
          "en":"Hello", "vi":"Xin chao", "audio":"$audio", "image":"$image"
        }]
        """.trimIndent()

    private fun packageBytes(entries: List<Entry>, version: Int = 1): ByteArray {
        val names = entries.map { it.name.toByteArray(StandardCharsets.UTF_8) }
        var offset = 12L + entries.indices.sumOf { 23L + names[it].size }
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.write("OPD3".toByteArray(StandardCharsets.US_ASCII))
                output.writeInt(version)
                output.writeInt(entries.size)
                entries.forEachIndexed { index, entry ->
                    output.writeShort(names[index].size)
                    output.writeByte(entry.type)
                    output.writeLong(offset)
                    output.writeLong(entry.bytes.size.toLong())
                    output.writeInt(CRC32().apply { update(entry.bytes) }.value.toInt())
                    output.write(names[index])
                    offset += entry.bytes.size
                }
                entries.forEach { output.write(it.bytes) }
            }
            bytes.toByteArray()
        }
    }

    private data class Entry(val name: String, val type: Int, val bytes: ByteArray)
}
