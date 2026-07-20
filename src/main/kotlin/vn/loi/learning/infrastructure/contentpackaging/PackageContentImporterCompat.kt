package vn.loi.learning.infrastructure.contentpackaging

import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.application.contentpackaging.PackageContentImporter
import vn.loi.learning.application.contentpackaging.PackageScanCandidate

class PackageContentImporterCompat(
    private val bundleImporter: PackageBundleImporter,
    private val legacyImporter: JvmPackageContentImporter
) : PackageContentImporter {

    override fun importContent(
        candidate: PackageScanCandidate
    ): ImportedPackageContent {
        if (isBundlePackage(candidate)) {
            return bundleImporter.importContent(candidate)
        }

        return try {
            bundleImporter.importContent(candidate)
        } catch (ex: IllegalArgumentException) {
            if (ex.message?.startsWith("Missing package file:") == true) {
                legacyImporter.importContent(candidate)
            } else {
                throw ex
            }
        }
    }

    private fun isBundlePackage(
        candidate: PackageScanCandidate
    ): Boolean =
        candidate.source.endsWith(
            ".opd3",
            ignoreCase = true
        )
}