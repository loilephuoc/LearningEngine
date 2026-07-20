package vn.loi.learning.infrastructure.persistence.json

import kotlinx.serialization.Serializable

/**
 * Envelope chuẩn cho một file JSON persistence.
 *
 * schemaVersion cho phép thay đổi cấu trúc record trong tương lai
 * mà vẫn nhận biết chính xác phiên bản dữ liệu đang được đọc.
 */
@Serializable
internal data class JsonPersistenceEnvelope<T>(
    val schemaVersion: Int,
    val records: T
)