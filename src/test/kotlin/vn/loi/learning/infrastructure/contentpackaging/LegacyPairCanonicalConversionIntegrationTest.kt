package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.CanonicalMediaStatus
import vn.loi.learning.application.contentpackaging.LegacyPairCanonicalConverter
import vn.loi.learning.application.contentpackaging.LegacyTopicPairDiscoveryService

class LegacyPairCanonicalConversionIntegrationTest {

    @Test
    fun `validated JSON+PKG pair converts to equivalent canonical topic model on repeated conversion`() {
        val tempDir = Files.createTempDirectory("legacy-canonical-conversion-test")

        try {
            val jsonFile = tempDir.resolve("Animals.json")
            val packageFile = tempDir.resolve("Animals.pkg")

            val jsonContent = """
                [
                  {
                    "group": "Fauna",
                    "section": "Mammals",
                    "lesson": "Lesson 1",
                    "en": "Cat",
                    "vi": "Con mèo",
                    "audio": "cat.mp3",
                    "image": "cat.png"
                  },
                  {
                    "group": "Fauna",
                    "section": "Mammals",
                    "lesson": "Lesson 1",
                    "en": "Dog",
                    "vi": "Con chó",
                    "audio": "dog.mp3"
                  }
                ]
            """.trimIndent()

            Files.writeString(jsonFile, jsonContent)

            val opd3Header = byteArrayOf(
                'O'.code.toByte(), 'P'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(),
                0, 0, 0, 1, // version 1
                0, 0, 0, 0  // 0 entries (synthetic OPD3 binary header)
            )
            Files.write(packageFile, opd3Header)

            // Step 1: Discovery via Beta-L02A
            val discoveryService = LegacyTopicPairDiscoveryService(JvmLegacyTopicFolderReader())
            val discoveryResult = discoveryService.discover(tempDir.toString())

            assertTrue(discoveryResult.isValid)
            assertEquals(1, discoveryResult.pairs.size)

            val validatedPair = discoveryResult.pairs.single()
            assertEquals("Animals", validatedPair.logicalTopicName)

            val converter = LegacyPairCanonicalConverter(
                jsonImporter = vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter(),
                jsonSourceReader = { path -> vn.loi.learning.adapter.jvm.JvmJsonFileReader().read(java.nio.file.Paths.get(path)) },
                pkgMediaScanner = JvmLegacyPkgMediaScanner()
            )
            val conversionResult1 = converter.convert(validatedPair)
            val conversionResult2 = converter.convert(validatedPair)

            assertTrue(conversionResult1.isReadyForExport)
            val pkg1 = conversionResult1.canonicalPackage
            val pkg2 = conversionResult2.canonicalPackage
            assertNotNull(pkg1)
            assertNotNull(pkg2)

            // Verify equivalent identity, structure, contents, items, media references
            assertEquals(pkg1.topicId, pkg2.topicId)
            assertEquals(pkg1.logicalTopicName, pkg2.logicalTopicName)
            assertEquals(pkg1.contents, pkg2.contents)
            assertEquals(pkg1.learningItems, pkg2.learningItems)
            assertEquals(pkg1.mediaReferences, pkg2.mediaReferences)
            assertEquals(pkg1.tags, pkg2.tags)

            // Verify content details
            assertEquals(2, pkg1.contents.size)
            assertTrue(pkg1.learningItems.size >= 4)
            assertEquals(3, pkg1.mediaReferences.size)

            // Since synthetic OPD3 binary file has 0 entries, media references are detected as MISSING
            assertTrue(pkg1.mediaReferences.all { it.status == CanonicalMediaStatus.MISSING })
            assertEquals(3, conversionResult1.warnings.size)
        } finally {
            Files.walk(tempDir).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }
}
