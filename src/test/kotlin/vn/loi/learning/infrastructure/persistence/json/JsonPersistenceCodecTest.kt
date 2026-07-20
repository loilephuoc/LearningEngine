package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JsonPersistenceCodecTest {

    private val json =
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
        }

    @Test
    fun `encodes records inside current schema envelope`() {
        val content =
            JsonPersistenceCodec.encode(
                records =
                    listOf(
                        "one",
                        "two"
                    )
            ) { envelope ->
                json.encodeToString(
                    envelope
                )
            }

        val envelope =
            json.decodeFromString<
                    JsonPersistenceEnvelope<List<String>>
                    >(
                content
            )

        assertEquals(
            JsonPersistenceCodec.CURRENT_SCHEMA_VERSION,
            envelope.schemaVersion
        )

        assertEquals(
            listOf(
                "one",
                "two"
            ),
            envelope.records
        )
    }

    @Test
    fun `decodes current schema envelope`() {
        val filePath =
            Path.of(
                "data.json"
            )

        val content =
            json.encodeToString(
                JsonPersistenceEnvelope(
                    schemaVersion =
                        JsonPersistenceCodec
                            .CURRENT_SCHEMA_VERSION,
                    records =
                        listOf(
                            "one",
                            "two"
                        )
                )
            )

        val records =
            JsonPersistenceCodec.decode(
                filePath =
                    filePath,
                content =
                    content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<String>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<String>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )

        assertEquals(
            listOf(
                "one",
                "two"
            ),
            records
        )
    }

    @Test
    fun `decodes legacy array without envelope`() {
        val filePath =
            Path.of(
                "legacy.json"
            )

        val records =
            JsonPersistenceCodec.decode(
                filePath =
                    filePath,
                content =
                    """["legacy-one","legacy-two"]""",
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<String>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<String>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )

        assertEquals(
            listOf(
                "legacy-one",
                "legacy-two"
            ),
            records
        )
    }

    @Test
    fun `detects legacy array after leading whitespace`() {
        val filePath =
            Path.of(
                "legacy-whitespace.json"
            )

        val records =
            JsonPersistenceCodec.decode(
                filePath =
                    filePath,
                content =
                    """
                    
                    
                    ["legacy"]
                    """.trimIndent(),
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<String>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<String>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )

        assertEquals(
            listOf(
                "legacy"
            ),
            records
        )
    }

    @Test
    fun `rejects unsupported schema version`() {
        val filePath =
            Path.of(
                "future.json"
            )

        val unsupportedVersion =
            JsonPersistenceCodec
                .CURRENT_SCHEMA_VERSION +
                    1

        val content =
            json.encodeToString(
                JsonPersistenceEnvelope(
                    schemaVersion =
                        unsupportedVersion,
                    records =
                        listOf(
                            "future"
                        )
                )
            )

        val failure =
            assertFailsWith<
                    UnsupportedJsonPersistenceSchemaException
                    > {
                JsonPersistenceCodec.decode(
                    filePath =
                        filePath,
                    content =
                        content,
                    decodeLegacy = { legacyContent ->
                        json.decodeFromString<
                                List<String>
                                >(
                            legacyContent
                        )
                    },
                    decodeEnvelope = { envelopeContent ->
                        json.decodeFromString<
                                JsonPersistenceEnvelope<
                                        List<String>
                                        >
                                >(
                            envelopeContent
                        )
                    }
                )
            }

        assertEquals(
            filePath,
            failure.filePath
        )

        assertEquals(
            unsupportedVersion,
            failure.schemaVersion
        )

        assertEquals(
            JsonPersistenceCodec.CURRENT_SCHEMA_VERSION,
            failure.supportedSchemaVersion
        )

        assertTrue(
            failure.message
                .orEmpty()
                .contains(
                    unsupportedVersion
                        .toString()
                )
        )
    }

    @Test
    fun `does not accept zero schema version`() {
        val filePath =
            Path.of(
                "zero-version.json"
            )

        val content =
            json.encodeToString(
                JsonPersistenceEnvelope(
                    schemaVersion =
                        0,
                    records =
                        emptyList<String>()
                )
            )

        val failure =
            assertFailsWith<
                    UnsupportedJsonPersistenceSchemaException
                    > {
                JsonPersistenceCodec.decode(
                    filePath =
                        filePath,
                    content =
                        content,
                    decodeLegacy = { legacyContent ->
                        json.decodeFromString<
                                List<String>
                                >(
                            legacyContent
                        )
                    },
                    decodeEnvelope = { envelopeContent ->
                        json.decodeFromString<
                                JsonPersistenceEnvelope<
                                        List<String>
                                        >
                                >(
                            envelopeContent
                        )
                    }
                )
            }

        assertEquals(
            0,
            failure.schemaVersion
        )
    }
}