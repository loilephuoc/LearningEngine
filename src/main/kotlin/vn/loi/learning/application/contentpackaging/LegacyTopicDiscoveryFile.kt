package vn.loi.learning.application.contentpackaging

data class LegacyTopicDiscoveryFile(
    val source: String,
    val fileName: String,
    val kind: LegacyTopicFileKind,
    val readable: Boolean = true,
    val supportedFormat: Boolean = true
) {

    init {
        require(source.isNotBlank()) {
            "Legacy topic file source must not be blank."
        }
        require(fileName.isNotBlank()) {
            "Legacy topic file name must not be blank."
        }
    }
}

enum class LegacyTopicFileKind {
    JSON,
    PKG,
    UNSUPPORTED
}
