package vn.loi.learning.infrastructure.contentpackaging

import java.nio.file.Path
import java.util.Locale
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageScanCandidate

/**
 * Router tương thích giữa bundle OPD3 và standalone legacy package.
 *
 * Định dạng được xác định từ phần mở rộng của source:
 *
 * - `.opd3` được nhập bởi [PackageBundleImporter];
 * - `.pkg` được nhập bởi [JvmPackageContentImporter].
 *
 * Legacy package theo cặp `.json + .pkg` không đi qua adapter này.
 * Workflow đó sử dụng [JvmLegacyPackageScanner] và
 * [LegacyOpd3PackageImporter] riêng.
 */
class PackageContentImporterCompat(
    private val bundleImporter: PackageBundleImporter,
    private val legacyImporter: JvmPackageContentImporter
) : PackageContentImporter {

    override fun importContent(
        candidate: PackageScanCandidate
    ): ImportedPackageContent =
        when (
            candidate.source.packageExtension()
        ) {
            OPD3_EXTENSION ->
                bundleImporter.importContent(
                    candidate
                )

            LEGACY_PACKAGE_EXTENSION ->
                legacyImporter.importContent(
                    candidate
                )

            else ->
                throw IllegalArgumentException(
                    "Unsupported package format: ${candidate.source}"
                )
        }

    private fun String.packageExtension(): String {
        val fileName =
            Path.of(this)
                .fileName
                .toString()

        val extensionSeparatorIndex =
            fileName.lastIndexOf(
                '.'
            )

        if (
            extensionSeparatorIndex < 0 ||
            extensionSeparatorIndex == fileName.lastIndex
        ) {
            return ""
        }

        return fileName
            .substring(
                extensionSeparatorIndex
            )
            .lowercase(
                Locale.ROOT
            )
    }

    private companion object {

        const val OPD3_EXTENSION =
            ".opd3"

        const val LEGACY_PACKAGE_EXTENSION =
            ".pkg"
    }
}