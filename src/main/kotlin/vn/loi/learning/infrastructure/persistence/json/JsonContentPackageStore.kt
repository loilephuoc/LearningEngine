package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.infrastructure.persistence.record.PackageRecord
import vn.loi.learning.infrastructure.persistence.store.ContentPackageStore

class JsonContentPackageStore(
    private val filePath: Path,
    private val json: Json = defaultJson()
) : ContentPackageStore {

    override fun loadAll(): List<PackageRecord> =
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
                            List<PackageRecord>
                            >(
                        legacyContent
                    )
                },
                decodeEnvelope = { envelopeContent ->
                    json.decodeFromString<
                            JsonPersistenceEnvelope<
                                    List<PackageRecord>
                                    >
                            >(
                        envelopeContent
                    )
                }
            )
        }

    override fun saveAll(
        records: List<PackageRecord>
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