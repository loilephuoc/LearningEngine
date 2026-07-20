package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

/**
 * Manifest OPD3 được ghi vào manifest.json.
 *
 * manifest.json không tự hash chính nó để tránh phụ thuộc vòng.
 */
data class PackageExportManifest(
    val name: String,
    val version: String,
    val format: String,
    val contentCount: Int,
    val learningItemCount: Int,
    val schemaVersion: Int =
        PackageDescriptor.CURRENT_SCHEMA_VERSION,
    val minimumEngineVersion: String? =
        null,
    val maximumEngineVersion: String? =
        null,
    val dependencies: Set<PackageDependency> =
        emptySet(),
    val hashAlgorithm: String? =
        null,
    val fileHashes: Map<String, String> =
        emptyMap()
)