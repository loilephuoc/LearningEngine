package vn.loi.learning.application.contentpackaging.export

import java.nio.file.Path

sealed interface ExportContentPackageResult {
    data class Success(
        val outputPath: Path,
        val sha256Checksum: String,
        val contentCount: Int,
        val learningItemCount: Int,
        val mediaAssetCount: Int
    ) : ExportContentPackageResult

    sealed interface Failure : ExportContentPackageResult {
        val message: String

        data class PackageNotFound(
            val installedPackageId: String,
            override val message: String = "Package '$installedPackageId' not found."
        ) : Failure

        data class PackageNotExportable(
            val installedPackageId: String,
            val state: String,
            override val message: String = "Package '$installedPackageId' is in state '$state' and cannot be exported. Only ACTIVE or ARCHIVED packages are exportable."
        ) : Failure

        data class MissingMedia(
            val assetPath: String,
            val contentId: String,
            override val message: String = "Referenced media asset '$assetPath' for content '$contentId' is missing from storage."
        ) : Failure

        data class DestinationExists(
            val destinationPath: String,
            override val message: String = "Destination file '$destinationPath' already exists."
        ) : Failure

        data class InvalidDestination(
            val destinationPath: String,
            override val message: String = "Destination path '$destinationPath' is invalid."
        ) : Failure

        data class ValidationFailed(
            val details: String,
            override val message: String = "Exported package validation failed: $details"
        ) : Failure

        data class Cancellation(
            override val message: String = "Export operation was cancelled."
        ) : Failure

        data class IOFailure(
            val cause: Throwable,
            override val message: String = "File I/O failure during package export: ${cause.message}"
        ) : Failure
    }
}
