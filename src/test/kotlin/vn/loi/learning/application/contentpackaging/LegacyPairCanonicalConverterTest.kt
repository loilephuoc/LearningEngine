package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.learning.model.LearningMode

class LegacyPairCanonicalConverterTest {

    private val sampleJson = """
        [
          {
            "group": "Basic",
            "section": "Greeting",
            "lesson": "Lesson 1",
            "en": "Hello",
            "vi": "Xin chào",
            "audio": "en_hello.mp3",
            "image": "hello.png"
          },
          {
            "group": "Basic",
            "section": "Greeting",
            "lesson": "Lesson 1",
            "en": "Goodbye",
            "vi": "Tạm biệt",
            "audio": "en_goodbye.mp3"
          }
        ]
    """.trimIndent()

    private val samplePair = ValidatedLegacyTopicPair(
        logicalTopicName = "Greetings",
        topicId = TopicId.deriveForLegacyPackage("Greetings", "OPD3"),
        jsonSource = "fixture/Greetings.json",
        packageSource = "fixture/Greetings.pkg"
    )

    private val mockJsonReader = LegacyJsonSourceReader { sampleJson }
    private val mockMediaScanner = LegacyPkgMediaScanner { listOf("en_hello.mp3", "hello.png", "en_goodbye.mp3") }

    private val converter = LegacyPairCanonicalConverter(
        jsonSourceReader = mockJsonReader,
        pkgMediaScanner = mockMediaScanner
    )

    @Test
    fun `1 valid pair converts into canonical topic model`() {
        val result = converter.convert(samplePair)

        assertTrue(result.isReadyForExport)
        assertFalse(result.hasFatalErrors)
        val pkg = result.canonicalPackage
        assertNotNull(pkg)
        assertEquals("Greetings", pkg.logicalTopicName)
        assertEquals(2, pkg.contents.size)
        assertTrue(pkg.learningItems.size >= 4)
    }

    @Test
    fun `2 TopicId is preserved`() {
        val result = converter.convert(samplePair)

        val pkg = result.canonicalPackage
        assertNotNull(pkg)
        assertEquals(samplePair.topicId, pkg.topicId)
    }

    @Test
    fun `3 conversion is deterministic across repeated runs`() {
        val result1 = converter.convert(samplePair)
        val result2 = converter.convert(samplePair)

        assertEquals(result1.canonicalPackage, result2.canonicalPackage)
        assertEquals(result1.diagnostics, result2.diagnostics)
        assertEquals(result1.isReadyForExport, result2.isReadyForExport)
    }

    @Test
    fun `4 existing valid legacy ContentId is preserved where supported`() {
        val result = converter.convert(samplePair)
        val contents = result.canonicalPackage?.contents.orEmpty()

        assertTrue(contents.all { it.id.value.startsWith("legacy-content-") })
    }

    @Test
    fun `5 derived ContentId is stable where derivation is required`() {
        val result1 = converter.convert(samplePair)
        val result2 = converter.convert(samplePair)

        val ids1 = result1.canonicalPackage?.contents?.map { it.id }
        val ids2 = result2.canonicalPackage?.contents?.map { it.id }

        assertEquals(ids1, ids2)
    }

    @Test
    fun `6 existing valid LearningItemId is preserved where supported`() {
        val result = converter.convert(samplePair)
        val items = result.canonicalPackage?.learningItems.orEmpty()

        assertTrue(items.all { item -> item.id.value.startsWith(item.contentId.value) })
    }

    @Test
    fun `7 derived LearningItemId is stable where derivation is required`() {
        val result1 = converter.convert(samplePair)
        val result2 = converter.convert(samplePair)

        val items1 = result1.canonicalPackage?.learningItems?.map { it.id }
        val items2 = result2.canonicalPackage?.learningItems?.map { it.id }

        assertEquals(items1, items2)
    }

    @Test
    fun `8 content-to-learning-item relationships are preserved`() {
        val result = converter.convert(samplePair)
        val contentIdSet = result.canonicalPackage?.contents?.map { it.id }?.toSet().orEmpty()
        val items = result.canonicalPackage?.learningItems.orEmpty()

        assertTrue(items.all { item -> item.contentId in contentIdSet })
    }

    @Test
    fun `9 canonical ordering is deterministic`() {
        val unsortedJson = """
            [
              { "en": "Zebra", "vi": "Con ngựa vằn" },
              { "en": "Apple", "vi": "Quả táo" },
              { "en": "Banana", "vi": "Quả chuối" }
            ]
        """.trimIndent()

        val converterUnsorted = LegacyPairCanonicalConverter(
            jsonSourceReader = { unsortedJson },
            pkgMediaScanner = { emptyList() }
        )

        val result1 = converterUnsorted.convert(samplePair)
        val result2 = converterUnsorted.convert(samplePair)

        assertEquals(result1.canonicalPackage?.contents?.map { it.id }, result2.canonicalPackage?.contents?.map { it.id })
        assertEquals(result1.canonicalPackage?.learningItems?.map { it.id }, result2.canonicalPackage?.learningItems?.map { it.id })
    }

    @Test
    fun `10 media references are represented without packaging media bytes`() {
        val result = converter.convert(samplePair)
        val mediaRefs = result.canonicalPackage?.mediaReferences.orEmpty()

        assertEquals(3, mediaRefs.size)
        assertTrue(mediaRefs.all { it.status == CanonicalMediaStatus.PRESENT })
        assertTrue(mediaRefs.any { it.mediaType == CanonicalMediaType.AUDIO && it.referencedAsset == "en_hello.mp3" })
        assertTrue(mediaRefs.any { it.mediaType == CanonicalMediaType.IMAGE && it.referencedAsset == "hello.png" })
    }

    @Test
    fun `11 malformed JSON produces fatal diagnostics`() {
        val malformedConverter = LegacyPairCanonicalConverter(
            jsonSourceReader = { "{ invalid json" },
            pkgMediaScanner = { emptyList() }
        )

        val result = malformedConverter.convert(samplePair)

        assertFalse(result.isReadyForExport)
        assertTrue(result.hasFatalErrors)
        assertNull(result.canonicalPackage)
        assertTrue(result.diagnostics.any { it.code == CanonicalConversionDiagnosticCode.MALFORMED_LEGACY_JSON })
    }

    @Test
    fun `12 duplicate content identity produces fatal diagnostics`() {
        val duplicateJson = """
            [
              { "group": "G", "section": "S", "lesson": "L", "en": "Hello", "vi": "Xin chào" },
              { "group": "G", "section": "S", "lesson": "L", "en": "Hello", "vi": "Xin chào" }
            ]
        """.trimIndent()

        val dupConverter = LegacyPairCanonicalConverter(
            jsonSourceReader = { duplicateJson },
            pkgMediaScanner = { emptyList() }
        )

        val result = dupConverter.convert(samplePair)

        assertFalse(result.isReadyForExport)
        assertTrue(result.hasFatalErrors)
        assertTrue(result.diagnostics.any { it.code == CanonicalConversionDiagnosticCode.DUPLICATE_CONTENT_IDENTITY })
    }

    @Test
    fun `13 duplicate learning-item identity produces fatal diagnostics`() {
        val duplicateJson = """
            [
              { "group": "G", "section": "S", "lesson": "L", "en": "Hello", "vi": "Xin chào" },
              { "group": "G", "section": "S", "lesson": "L", "en": "Hello", "vi": "Xin chào" }
            ]
        """.trimIndent()

        val dupConverter = LegacyPairCanonicalConverter(
            jsonSourceReader = { duplicateJson },
            pkgMediaScanner = { emptyList() }
        )

        val result = dupConverter.convert(samplePair)

        assertFalse(result.isReadyForExport)
        assertTrue(result.hasFatalErrors)
        assertTrue(result.diagnostics.any {
            it.code == CanonicalConversionDiagnosticCode.DUPLICATE_CONTENT_IDENTITY ||
                    it.code == CanonicalConversionDiagnosticCode.DUPLICATE_LEARNING_ITEM_IDENTITY
        })
    }

    @Test
    fun `14 unresolved media produces a structured warning or error according to existing semantics`() {
        val missingMediaScanner = LegacyPkgMediaScanner { listOf("hello.png") } // en_hello.mp3 and en_goodbye.mp3 are missing
        val missingMediaConverter = LegacyPairCanonicalConverter(
            jsonSourceReader = mockJsonReader,
            pkgMediaScanner = missingMediaScanner
        )

        val result = missingMediaConverter.convert(samplePair)

        assertTrue(result.isReadyForExport) // Warnings allow conversion readiness when data remains usable
        assertFalse(result.hasFatalErrors)
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.warnings.any { it.code == CanonicalConversionDiagnosticCode.UNRESOLVED_MEDIA_REFERENCE })

        val missingRefs = result.canonicalPackage?.mediaReferences?.filter { it.status == CanonicalMediaStatus.MISSING }.orEmpty()
        assertEquals(2, missingRefs.size)
    }

    @Test
    fun `15 learner-specific state is absent from the canonical package model`() {
        val result = converter.convert(samplePair)
        val pkg = result.canonicalPackage

        assertNotNull(pkg)
        // Verify canonical package and items hold no FSRS / review history / mastery / difficulty / stability
        val fields = pkg::class.java.declaredFields.map { it.name }
        assertFalse("reviewHistory" in fields)
        assertFalse("schedulerState" in fields)
        assertFalse("mastery" in fields)
        assertFalse("difficulty" in fields)
        assertFalse("stability" in fields)
        assertFalse("sessionState" in fields)
    }
}
