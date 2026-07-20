package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

/**
 * Persistence DTO của ContentPackage.
 *
 * Chỉ dùng cho persistence layer.
 * Không chứa logic nghiệp vụ.
 */
@Serializable
data class PackageRecord(
    val id: String,
    val name: String,
    val version: String,
    val format: String,
    val libraryIds: Set<String> =
        emptySet(),
    val schemaVersion: Int =
        PackageDescriptor.CURRENT_SCHEMA_VERSION,
    val minimumEngineVersion: String? =
        null,
    val maximumEngineVersion: String? =
        null,
    val dependencies: Set<PackageDependencyRecord> =
        emptySet()
)

@Serializable
data class PackageDependencyRecord(
    val packageName: String,
    val minimumVersion: String? =
        null,
    val maximumVersion: String? =
        null
)