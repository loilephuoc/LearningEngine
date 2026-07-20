package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId

class PackageDependencyVersionMatcherTest {

    @Test
    fun `matches package inside inclusive version range`() {
        assertTrue(
            PackageDependencyVersionMatcher.matches(
                contentPackage =
                    contentPackage(
                        name =
                            "Core English",
                        version =
                            "2.0.0"
                    ),
                dependency =
                    PackageDependency(
                        packageName =
                            "Core English",
                        minimumVersion =
                            "2.0.0",
                        maximumVersion =
                            "2.0.0"
                    )
            )
        )
    }

    @Test
    fun `rejects package with different name`() {
        assertFalse(
            PackageDependencyVersionMatcher.matches(
                contentPackage =
                    contentPackage(
                        name =
                            "Grammar",
                        version =
                            "2.0.0"
                    ),
                dependency =
                    PackageDependency(
                        packageName =
                            "Core English",
                        minimumVersion =
                            "1.0.0"
                    )
            )
        )
    }

    @Test
    fun `rejects version below minimum`() {
        assertFalse(
            PackageDependencyVersionMatcher.matchesVersion(
                installedVersion =
                    "1.9.9",
                dependency =
                    PackageDependency(
                        packageName =
                            "Core English",
                        minimumVersion =
                            "2.0.0"
                    )
            )
        )
    }

    @Test
    fun `rejects version above maximum`() {
        assertFalse(
            PackageDependencyVersionMatcher.matchesVersion(
                installedVersion =
                    "3.0.0",
                dependency =
                    PackageDependency(
                        packageName =
                            "Core English",
                        maximumVersion =
                            "2.9.9"
                    )
            )
        )
    }

    @Test
    fun `rejects invalid installed version`() {
        assertFalse(
            PackageDependencyVersionMatcher.matchesVersion(
                installedVersion =
                    "latest",
                dependency =
                    PackageDependency(
                        packageName =
                            "Core English"
                    )
            )
        )
    }

    @Test
    fun `rejects invalid minimum version`() {
        assertFalse(
            PackageDependencyVersionMatcher.matchesVersion(
                installedVersion =
                    "2.0.0",
                dependency =
                    PackageDependency(
                        packageName =
                            "Core English",
                        minimumVersion =
                            "latest"
                    )
            )
        )
    }

    @Test
    fun `rejects invalid maximum version`() {
        assertFalse(
            PackageDependencyVersionMatcher.matchesVersion(
                installedVersion =
                    "2.0.0",
                dependency =
                    PackageDependency(
                        packageName =
                            "Core English",
                        maximumVersion =
                            "latest"
                    )
            )
        )
    }

    @Test
    fun `rejects reversed version range`() {
        assertFalse(
            PackageDependencyVersionMatcher.matchesVersion(
                installedVersion =
                    "2.5.0",
                dependency =
                    PackageDependency(
                        packageName =
                            "Core English",
                        minimumVersion =
                            "3.0.0",
                        maximumVersion =
                            "2.0.0"
                    )
            )
        )
    }

    @Test
    fun `accepts valid version when range is unrestricted`() {
        assertTrue(
            PackageDependencyVersionMatcher.matchesVersion(
                installedVersion =
                    "1.0.0",
                dependency =
                    PackageDependency(
                        packageName =
                            "Core English"
                    )
            )
        )
    }

    private fun contentPackage(
        name: String,
        version: String
    ): ContentPackage =
        ContentPackage(
            id =
                PackageId(
                    "$name-$version"
                        .lowercase()
                        .replace(
                            " ",
                            "-"
                        )
                ),
            descriptor =
                PackageDescriptor(
                    name =
                        name,
                    version =
                        version,
                    format =
                        "OPD3"
                )
        )
}