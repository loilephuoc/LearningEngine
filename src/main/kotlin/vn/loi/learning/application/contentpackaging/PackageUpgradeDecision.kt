package vn.loi.learning.application.contentpackaging

sealed interface PackageUpgradeDecision {

    data object UpgradeAvailable :
        PackageUpgradeDecision

    data object SameVersion :
        PackageUpgradeDecision

    data object Downgrade :
        PackageUpgradeDecision

    data object DifferentPackage :
        PackageUpgradeDecision

    data object InvalidInstalledVersion :
        PackageUpgradeDecision

    data object InvalidCandidateVersion :
        PackageUpgradeDecision
}