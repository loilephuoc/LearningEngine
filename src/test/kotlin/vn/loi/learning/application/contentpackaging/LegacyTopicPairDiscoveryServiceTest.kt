package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.topic.model.TopicId

class LegacyTopicPairDiscoveryServiceTest {

    @Test
    fun `discovers one valid same-name pair`() {
        val result =
            discover(
                json("Vocabulary"),
                pkg("vocabulary")
            )

        assertTrue(result.isValid)
        assertEquals(emptyList(), result.diagnostics)
        assertEquals(1, result.pairs.size)
        assertEquals(
            "Vocabulary",
            result.pairs.single().logicalTopicName
        )
        assertEquals(
            TopicId.deriveForLegacyPackage(
                packageName = "Vocabulary",
                packageFormat = "OPD3"
            ),
            result.pairs.single().topicId
        )
    }

    @Test
    fun `reports missing json`() {
        val result =
            discover(
                pkg("Vocabulary")
            )

        assertEquals(
            listOf(
                LegacyTopicDiscoveryDiagnosticCode.MISSING_JSON
            ),
            result.codes()
        )
        assertEquals(emptyList(), result.pairs)
    }

    @Test
    fun `reports missing pkg`() {
        val result =
            discover(
                json("Vocabulary")
            )

        assertEquals(
            listOf(
                LegacyTopicDiscoveryDiagnosticCode.MISSING_PKG
            ),
            result.codes()
        )
        assertEquals(emptyList(), result.pairs)
    }

    @Test
    fun `reports duplicate json`() {
        val result =
            discover(
                json(
                    baseName = "Vocabulary",
                    source = "a/Vocabulary.json"
                ),
                json(
                    baseName = "vocabulary",
                    source = "a/vocabulary.JSON",
                    extension = "JSON"
                ),
                pkg("Vocabulary")
            )

        assertEquals(
            listOf(
                LegacyTopicDiscoveryDiagnosticCode.DUPLICATE_JSON
            ),
            result.codes()
        )
        assertEquals(emptyList(), result.pairs)
    }

    @Test
    fun `reports duplicate pkg`() {
        val result =
            discover(
                json("Vocabulary"),
                pkg(
                    baseName = "Vocabulary",
                    source = "a/Vocabulary.pkg"
                ),
                pkg(
                    baseName = "vocabulary",
                    source = "a/vocabulary.PKG",
                    extension = "PKG"
                )
            )

        assertEquals(
            listOf(
                LegacyTopicDiscoveryDiagnosticCode.DUPLICATE_PKG
            ),
            result.codes()
        )
        assertEquals(emptyList(), result.pairs)
    }

    @Test
    fun `reports mismatched base names without silently dropping either file`() {
        val result =
            discover(
                json("Vocabulary"),
                pkg("Grammar")
            )

        assertEquals(
            setOf(
                LegacyTopicDiscoveryDiagnosticCode.BASE_NAME_MISMATCH,
                LegacyTopicDiscoveryDiagnosticCode.MISSING_JSON,
                LegacyTopicDiscoveryDiagnosticCode.MISSING_PKG
            ),
            result.codes().toSet()
        )
        assertEquals(emptyList(), result.pairs)
    }

    @Test
    fun `sorts pairs and diagnostics deterministically`() {
        val files =
            listOf(
                pkg("Zulu"),
                json("alpha"),
                pkg("alpha"),
                json("Zulu"),
                json("Missing")
            )
        val first =
            discover(
                *files.toTypedArray()
            )
        val second =
            discover(
                *files.reversed().toTypedArray()
            )

        assertEquals(first, second)
        assertEquals(
            listOf(
                "alpha",
                "Zulu"
            ),
            first.pairs.map(
                ValidatedLegacyTopicPair::logicalTopicName
            )
        )
    }

    @Test
    fun `reports unreadable and unsupported files`() {
        val result =
            discover(
                json(
                    baseName = "Unreadable",
                    readable = false
                ),
                pkg("Unreadable"),
                LegacyTopicDiscoveryFile(
                    source = "a/notes.txt",
                    fileName = "notes.txt",
                    kind =
                        LegacyTopicFileKind.UNSUPPORTED,
                    supportedFormat = false
                )
            )

        assertFalse(result.isValid)
        assertEquals(
            setOf(
                LegacyTopicDiscoveryDiagnosticCode.UNREADABLE_FILE,
                LegacyTopicDiscoveryDiagnosticCode.UNSUPPORTED_FORMAT
            ),
            result.codes().toSet()
        )
        assertEquals(emptyList(), result.pairs)
    }

    @Test
    fun `reports unsupported pkg signature`() {
        val result =
            discover(
                json("Vocabulary"),
                pkg(
                    baseName = "Vocabulary",
                    supportedFormat = false
                )
            )

        assertEquals(
            listOf(
                LegacyTopicDiscoveryDiagnosticCode.UNSUPPORTED_FORMAT
            ),
            result.codes()
        )
        assertEquals(emptyList(), result.pairs)
    }

    private fun discover(
        vararg files: LegacyTopicDiscoveryFile
    ): LegacyTopicDiscoveryResult =
        LegacyTopicPairDiscoveryService(
            folderReader =
                LegacyTopicFolderReader {
                    files.toList()
                }
        ).discover(
            "fixture-folder"
        )

    private fun json(
        baseName: String,
        source: String = "a/$baseName.json",
        extension: String = "json",
        readable: Boolean = true
    ): LegacyTopicDiscoveryFile =
        LegacyTopicDiscoveryFile(
            source = source,
            fileName = "$baseName.$extension",
            kind = LegacyTopicFileKind.JSON,
            readable = readable
        )

    private fun pkg(
        baseName: String,
        source: String = "a/$baseName.pkg",
        extension: String = "pkg",
        supportedFormat: Boolean = true
    ): LegacyTopicDiscoveryFile =
        LegacyTopicDiscoveryFile(
            source = source,
            fileName = "$baseName.$extension",
            kind = LegacyTopicFileKind.PKG,
            supportedFormat = supportedFormat
        )

    private fun LegacyTopicDiscoveryResult.codes():
        List<LegacyTopicDiscoveryDiagnosticCode> =
        diagnostics.map(
            LegacyTopicDiscoveryDiagnostic::code
        )
}
