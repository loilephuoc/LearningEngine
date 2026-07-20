package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.ContentPackage

data class PackageUpgradeCandidate(
    val installedPackage: ContentPackage,
    val candidatePackage: ContentPackage
) {

    init {
        require(
            installedPackage.id != candidatePackage.id
        ) {
            "Installed package and candidate package must have different IDs."
        }
    }
}