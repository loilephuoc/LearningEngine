package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path

/**
 * Báo hiệu file persistence tồn tại và đọc được,
 * nhưng nội dung không thể giải mã thành record mong đợi.
 *
 * Exception giữ nguyên cause từ kotlinx.serialization
 * để phục vụ chẩn đoán và logging.
 */
class InvalidJsonPersistenceException(
    val filePath: Path,
    cause: Throwable
) : IllegalStateException(
    "Invalid JSON persistence file: " +
            filePath
                .toAbsolutePath()
                .normalize(),
    cause
)