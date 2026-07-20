package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path

/**
 * Báo hiệu file persistence sử dụng schema version
 * mà phiên bản Learning Engine hiện tại chưa hỗ trợ.
 */
class UnsupportedJsonPersistenceSchemaException(
    val filePath: Path,
    val schemaVersion: Int,
    val supportedSchemaVersion: Int
) : IllegalStateException(
    buildString {
        append(
            "Unsupported JSON persistence schema version "
        )
        append(
            schemaVersion
        )
        append(
            " in file: "
        )
        append(
            filePath
                .toAbsolutePath()
                .normalize()
        )
        append(
            ". Supported schema version: "
        )
        append(
            supportedSchemaVersion
        )
        append(
            "."
        )
    }
)