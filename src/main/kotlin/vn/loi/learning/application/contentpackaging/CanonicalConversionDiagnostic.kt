package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.model.ContentId

/**
 * Diagnostic cấu trúc cho quá trình chuyển đổi canonical gói legacy.
 */
data class CanonicalConversionDiagnostic(
    val code: CanonicalConversionDiagnosticCode,
    val severity: CanonicalConversionDiagnosticSeverity,
    val message: String,
    val contentId: ContentId? = null,
    val source: String? = null
) {

    init {
        require(message.isNotBlank()) {
            "Diagnostic message must not be blank."
        }
    }
}

/**
 * Mức độ nghiêm trọng của chẩn đoán chuyển đổi.
 */
enum class CanonicalConversionDiagnosticSeverity {
    FATAL,
    WARNING
}

/**
 * Mã chẩn đoán chi tiết cho quy trình chuyển đổi legacy pair sang canonical package.
 */
enum class CanonicalConversionDiagnosticCode {
    MALFORMED_LEGACY_JSON,
    UNSUPPORTED_LEGACY_SCHEMA,
    INVALID_REQUIRED_FIELD,
    DUPLICATE_CONTENT_IDENTITY,
    DUPLICATE_LEARNING_ITEM_IDENTITY,
    UNRESOLVED_CONTENT_ITEM_RELATIONSHIP,
    UNRESOLVED_MEDIA_REFERENCE,
    UNSUPPORTED_LEGACY_CONSTRUCT,
    IDENTITY_COLLISION
}
