package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageUpgradePolicyTest {

    private val policy =
        PackageUpgradePolicy()

    @Test
    fun `newer package version is an upgrade`() {
        assertEquals(
            PackageUpgradeDecision.UpgradeAvailable,
            policy.evaluate(
                installedDescriptor =
                    descriptor(
                        version =
                            "1.0.0"
                    ),
                candidateDescriptor =
                    descriptor(
                        version =
                            "1.1.0"
                    )
            )
        )
    }

    @Test
    fun `same package version is not an upgrade`() {
        assertEquals(
            PackageUpgradeDecision.SameVersion,
            policy.evaluate(
                installedDescriptor =
                    descriptor(
                        version =
                            "1.0"
                    ),
                candidateDescriptor =
                    descriptor(
                        version =
                            "1.0.0"
                    )
            )
        )
    }

    @Test
    fun `older package version is downgrade`() {
        assertEquals(
            PackageUpgradeDecision.Downgrade,
            policy.evaluate(
                installedDescriptor =
                    descriptor(
                        version =
                            "2.0.0"
                    ),
                candidateDescriptor =
                    descriptor(
                        version =
                            "1.9.0"
                    )
            )
        )
    }

    @Test
    fun `different package name is rejected`() {
        assertEquals(
            PackageUpgradeDecision.DifferentPackage,
            policy.evaluate(
                installedDescriptor =
                    descriptor(
                        name =
                            "Package A",
                        version =
                            "1.0.0"
                    ),
                candidateDescriptor =
                    descriptor(
                        name =
                            "Package B",
                        version =
                            "2.0.0"
                    )
            )
        )
    }

    @Test
    fun `invalid installed version is reported`() {
        assertEquals(
            PackageUpgradeDecision.InvalidInstalledVersion,
            policy.evaluate(
                installedDescriptor =
                    descriptor(
                        version =
                            "invalid"
                    ),
                candidateDescriptor =
                    descriptor(
                        version =
                            "2.0.0"
                    )
            )
        )
    }

    @Test
    fun `invalid candidate version is reported`() {
        assertEquals(
            PackageUpgradeDecision.InvalidCandidateVersion,
            policy.evaluate(
                installedDescriptor =
                    descriptor(
                        version =
                            "1.0.0"
                    ),
                candidateDescriptor =
                    descriptor(
                        version =
                            "invalid"
                    )
            )
        )
    }

    private fun descriptor(
        name: String = "English Package",
        version: String,
        format: String = "OPD3"
    ): PackageDescriptor =
        PackageDescriptor(
            name =
                name,
            version =
                version,
            format =
                format
        )
}