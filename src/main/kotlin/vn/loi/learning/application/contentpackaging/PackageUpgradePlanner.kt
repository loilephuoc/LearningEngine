package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage

class PackageUpgradePlanner(
    private val contentPackageRepository:
    ContentPackageRepository,
    private val upgradePolicy:
    PackageUpgradePolicy =
        PackageUpgradePolicy()
) {

    fun findUpgradeCandidate(
        candidatePackage: ContentPackage
    ): PackageUpgradeCandidate? =
        contentPackageRepository
            .findAll()
            .asSequence()
            .map { installedPackage ->
                installedPackage to
                        upgradePolicy.evaluate(
                            installedDescriptor =
                                installedPackage.descriptor,
                            candidateDescriptor =
                                candidatePackage.descriptor
                        )
            }
            .filter { (_, decision) ->
                decision ==
                        PackageUpgradeDecision.UpgradeAvailable
            }
            .map { (installedPackage, _) ->
                PackageUpgradeCandidate(
                    installedPackage =
                        installedPackage,
                    candidatePackage =
                        candidatePackage
                )
            }
            .maxByOrNull { upgradeCandidate ->
                NumericPackageVersion.parse(
                    upgradeCandidate
                        .installedPackage
                        .version
                )
            }
}