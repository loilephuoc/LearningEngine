package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.model.ContentId

/**
 * Gói dữ liệu chứa kết quả đóng gói tài nguyên media cho một CanonicalTopicPackage.
 */
data class CanonicalMediaBundle(
    val manifest: CanonicalMediaManifest,
    val assets: List<CanonicalMediaAssetBytes>,
    val diagnostics: List<CanonicalConversionDiagnostic> = emptyList()
) {
    val totalSize: Long
        get() = assets.sumOf { it.size }

    val assetCount: Int
        get() = assets.size

    val hasUnresolvedAssets: Boolean
        get() = diagnostics.any { it.code == CanonicalConversionDiagnosticCode.UNRESOLVED_MEDIA_REFERENCE }
}

/**
 * Manifest danh mục thông tin tài nguyên media đã được đóng gói trong gói canonical.
 */
data class CanonicalMediaManifest(
    val entries: List<CanonicalMediaManifestEntry>
)

/**
 * Một mục thông tin tài nguyên media trong manifest.
 */
data class CanonicalMediaManifestEntry(
    val logicalPath: String,
    val mediaType: CanonicalMediaType,
    val size: Long,
    val sha256: String,
    val owningContentIds: List<ContentId>
) {
    init {
        require(logicalPath.isNotBlank()) {
            "Logical media path must not be blank."
        }
        require(sha256.isNotBlank()) {
            "SHA-256 checksum must not be blank."
        }
    }
}

/**
 * Dữ liệu byte thực sự của một tệp media trong gói đóng gói canonical.
 */
class CanonicalMediaAssetBytes(
    val logicalPath: String,
    val mediaType: CanonicalMediaType,
    val bytes: ByteArray,
    val size: Long = bytes.size.toLong(),
    val sha256: String
) {
    init {
        require(logicalPath.isNotBlank()) {
            "Logical media path must not be blank."
        }
        require(sha256.isNotBlank()) {
            "SHA-256 checksum must not be blank."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CanonicalMediaAssetBytes) return false
        return logicalPath == other.logicalPath && sha256 == other.sha256
    }

    override fun hashCode(): Int {
        var result = logicalPath.hashCode()
        result = 31 * result + sha256.hashCode()
        return result
    }

    override fun toString(): String =
        "CanonicalMediaAssetBytes(logicalPath='$logicalPath', size=$size, sha256='$sha256')"
}
