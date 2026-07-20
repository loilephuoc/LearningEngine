package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.PackageCatalogRecord
import vn.loi.learning.infrastructure.persistence.store.PackageCatalogStore

class JsonPackageCatalogStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : PackageCatalogStore {

    override fun loadAll(): List<PackageCatalogRecord> =
        JsonFileReader.read(
            filePath =
                filePath,
            emptyValue =
                emptyList()
        ) { content ->
            JsonPersistenceCodec.decode(
                filePath =
                    filePath,
                content =
                    content,
                decodeLegacy = { legacyContent ->
                    json.decodeFromString<
                            List<PackageCatalogRecord>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<PackageCatalogRecord>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )
        }

    override fun saveAll(
        records: List<PackageCatalogRecord>
    ) {
        val content =
            JsonPersistenceCodec.encode(
                records =
                    records
            ) { envelope ->
                json.encodeToString(
                    envelope
                )
            }

        JsonFileWriter.write(
            filePath =
                filePath,
            content =
                content
        )
    }

    companion object {

        private fun defaultJson(): Json =
            Json {
                prettyPrint = true
                prettyPrintIndent = "  "
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
    }
}