package vn.loi.learning.domain.content.packaging.model

/**
 * Metadata mô tả một Content Package.
 *
 * Descriptor chỉ chứa thông tin logic ổn định của package.
 * Đường dẫn file, cache và trạng thái cài đặt thuộc Infrastructure.
 */
data class PackageDescriptor(
    val name: String,
    val version: String,
    val format: String,
    val schemaVersion: Int =
        CURRENT_SCHEMA_VERSION,
    val minimumEngineVersion: String? =
        null,
    val maximumEngineVersion: String? =
        null,
    val dependencies: Set<PackageDependency> =
        emptySet()
) {

    init {
        require(name.isNotBlank()) {
            "Package name must not be blank."
        }

        require(version.isNotBlank()) {
            "Package version must not be blank."
        }

        require(format.isNotBlank()) {
            "Package format must not be blank."
        }

        require(schemaVersion > 0) {
            "Package schema version must be positive."
        }

        require(
            minimumEngineVersion == null ||
                    minimumEngineVersion.isNotBlank()
        ) {
            "Minimum engine version must be null or non-blank."
        }

        require(
            maximumEngineVersion == null ||
                    maximumEngineVersion.isNotBlank()
        ) {
            "Maximum engine version must be null or non-blank."
        }

        require(
            dependencies.none { dependency ->
                dependency.packageName == name
            }
        ) {
            "Package must not depend on itself."
        }

        require(
            dependencies
                .groupBy { dependency ->
                    dependency.packageName
                }
                .none { (_, matchingDependencies) ->
                    matchingDependencies.size > 1
                }
        ) {
            "Package dependencies must have unique package names."
        }
    }

    companion object {

        const val CURRENT_SCHEMA_VERSION =
            1
    }
}