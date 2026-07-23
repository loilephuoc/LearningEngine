package vn.loi.learning.application.contentpackaging

data class LegacyTopicDiscoveryDiagnostic(
    val code: LegacyTopicDiscoveryDiagnosticCode,
    val logicalTopicName: String?,
    val sources: List<String>,
    val message: String
) {

    init {
        require(sources == sources.sorted()) {
            "Legacy topic diagnostic sources must be sorted."
        }
        require(message.isNotBlank()) {
            "Legacy topic diagnostic message must not be blank."
        }
    }
}

enum class LegacyTopicDiscoveryDiagnosticCode {
    MISSING_JSON,
    MISSING_PKG,
    DUPLICATE_JSON,
    DUPLICATE_PKG,
    BASE_NAME_MISMATCH,
    UNREADABLE_FILE,
    UNSUPPORTED_FORMAT
}
