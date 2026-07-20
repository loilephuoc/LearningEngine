package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class PackageUpgradePlannerTest {

    @Test
    fun `planner selects newest installed version below candidate`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "package-1",
                version =
                    "1.0.0"
            )
        )

        repository.save(
            contentPackage(
                id =
                    "package-2",
                version =
                    "1.5.0"
            )
        )

        val candidate =
            contentPackage(
                id =
                    "package-3",
                version =
                    "2.0.0"
            )

        val result =
            PackageUpgradePlanner(
                contentPackageRepository =
                    repository
            ).findUpgradeCandidate(
                candidate
            )

        assertEquals(
            PackageId(
                "package-2"
            ),
            result
                ?.installedPackage
                ?.id
        )

        assertEquals(
            candidate,
            result?.candidatePackage
        )
    }

    @Test
    fun `planner ignores different package names`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "package-1",
                name =
                    "Package A",
                version =
                    "1.0.0"
            )
        )

        val result =
            PackageUpgradePlanner(
                contentPackageRepository =
                    repository
            ).findUpgradeCandidate(
                contentPackage(
                    id =
                        "package-2",
                    name =
                        "Package B",
                    version =
                        "2.0.0"
                )
            )

        assertNull(
            result
        )
    }

    @Test
    fun `planner returns null when candidate is not newer`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id =
                    "package-1",
                version =
                    "2.0.0"
            )
        )

        val result =
            PackageUpgradePlanner(
                contentPackageRepository =
                    repository
            ).findUpgradeCandidate(
                contentPackage(
                    id =
                        "package-2",
                    version =
                        "1.0.0"
                )
            )

        assertNull(
            result
        )
    }

    private fun contentPackage(
        id: String,
        name: String = "English Package",
        version: String
    ): ContentPackage =
        ContentPackage(
            id =
                PackageId(
                    id
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