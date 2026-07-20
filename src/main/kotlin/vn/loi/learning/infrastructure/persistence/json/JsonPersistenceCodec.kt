package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path

/**
 * Codec trung gian cho schema-versioned JSON persistence.
 *
 * Hỗ trợ:
 * - đọc định dạng legacy là JSON array trực tiếp;
 * - đọc định dạng envelope có schemaVersion;
 * - ghi dữ liệu mới dưới dạng envelope;
 * - từ chối rõ ràng schema version chưa được hỗ trợ.
 */
internal object JsonPersistenceCodec {

    const val CURRENT_SCHEMA_VERSION: Int =
        1

    fun <T> encode(
        records: T,
        encodeEnvelope: (
            JsonPersistenceEnvelope<T>
        ) -> String
    ): String =
        encodeEnvelope(
            JsonPersistenceEnvelope(
                schemaVersion =
                    CURRENT_SCHEMA_VERSION,
                records =
                    records
            )
        )

    fun <T> decode(
        filePath: Path,
        content: String,
        decodeLegacy: (String) -> T,
        decodeEnvelope: (
            String
        ) -> JsonPersistenceEnvelope<T>
    ): T {
        if (
            isLegacyArray(
                content
            )
        ) {
            return decodeLegacy(
                content
            )
        }

        val envelope =
            decodeEnvelope(
                content
            )

        requireSupportedSchema(
            filePath =
                filePath,
            schemaVersion =
                envelope.schemaVersion
        )

        return envelope.records
    }

    private fun isLegacyArray(
        content: String
    ): Boolean =
        content
            .trimStart()
            .startsWith(
                "["
            )

    private fun requireSupportedSchema(
        filePath: Path,
        schemaVersion: Int
    ) {
        if (
            schemaVersion !=
            CURRENT_SCHEMA_VERSION
        ) {
            throw UnsupportedJsonPersistenceSchemaException(
                filePath =
                    filePath,
                schemaVersion =
                    schemaVersion,
                supportedSchemaVersion =
                    CURRENT_SCHEMA_VERSION
            )
        }
    }
}