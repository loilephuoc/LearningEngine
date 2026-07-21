package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository

class InstalledPackageQueryServiceTest {

    @Test
    fun `returns installed package index ordered by name`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            ContentPackage(
                id =
                    PackageId(
                        "package-vocabulary"
                    ),
                descriptor =
                    PackageDescriptor(
                        name =
                            "Vocabulary",
                        version =
                            "2.0.0",
                        format =
                            "OPD3"
                    ),
                libraryIds =
                    setOf(
                        ContentLibraryId(
                            "vocabulary-library"
                        )
                    )
            )
        )

        repository.save(
            ContentPackage(
                id =
                    PackageId(
                        "package-conversations"
                    ),
                descriptor =
                    PackageDescriptor(
                        name =
                            "Conversations",
                        version =
                            "1.0.0",
                        format =
                            "OPD3"
                    ),
                libraryIds =
                    setOf(
                        ContentLibraryId(
                            "conversation-library-2"
                        ),
                        ContentLibraryId(
                            "conversation-library-1"
                        )
                    )
            )
        )

        val result =
            InstalledPackageQueryService(
                contentPackageRepository =
                    repository
            ).query()

        assertEquals(
            2,
            result.size
        )

        assertEquals(
            "package-conversations",
            result[0].id
        )

        assertEquals(
            "Conversations",
            result[0].name
        )

        assertEquals(
            2,
            result[0].libraryCount
        )

        assertEquals(
            listOf(
                "conversation-library-1",
                "conversation-library-2"
            ),
            result[0].libraryIds
        )

        assertEquals(
            "package-vocabulary",
            result[1].id
        )

        assertEquals(
            listOf(
                "vocabulary-library"
            ),
            result[1].libraryIds
        )
    }

    @Test
    fun `projects descriptor compatibility metadata without reading package contents`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            ContentPackage(
                id =
                    PackageId(
                        "package-medical"
                    ),
                descriptor =
                    PackageDescriptor(
                        name =
                            "Medical English",
                        version =
                            "3.1.0",
                        format =
                            "OPD3",
                        schemaVersion =
                            1,
                        minimumEngineVersion =
                            "1.0.0",
                        maximumEngineVersion =
                            "3.0.0",
                        dependencies =
                            setOf(
                                PackageDependency(
                                    packageName =
                                        "medical-core",
                                    minimumVersion =
                                        "2.0.0"
                                )
                            )
                    ),
                libraryIds =
                    setOf(
                        ContentLibraryId(
                            "medical-library"
                        )
                    )
            )
        )

        val item =
            InstalledPackageQueryService(
                contentPackageRepository =
                    repository
            ).query().single()

        assertEquals(
            1,
            item.schemaVersion
        )

        assertEquals(
            "1.0.0",
            item.minimumEngineVersion
        )

        assertEquals(
            "3.0.0",
            item.maximumEngineVersion
        )

        assertEquals(
            1,
            item.dependencyCount
        )

        assertTrue(
            item.hasLibraries
        )

        assertTrue(
            item.hasDependencies
        )

        assertEquals(
            "1.0.0 - 3.0.0",
            item.engineCompatibilityDescription
        )
    }

    @Test
    fun `package without libraries or compatibility limits produces stable defaults`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            ContentPackage(
                id =
                    PackageId(
                        "empty-package"
                    ),
                descriptor =
                    PackageDescriptor(
                        name =
                            "Empty Package",
                        version =
                            "1.0.0",
                        format =
                            "OPD3"
                    )
            )
        )

        val item =
            InstalledPackageQueryService(
                contentPackageRepository =
                    repository
            ).query().single()

        assertEquals(
            0,
            item.libraryCount
        )

        assertEquals(
            emptyList(),
            item.libraryIds
        )

        assertFalse(
            item.hasLibraries
        )

        assertFalse(
            item.hasDependencies
        )

        assertEquals(
            "Any supported engine version",
            item.engineCompatibilityDescription
        )
    }

    @Test
    fun `describes minimum-only engine compatibility`() {
        val item =
            packageItem(
                minimumEngineVersion =
                    "2.0.0"
            )

        assertEquals(
            "2.0.0+",
            item.engineCompatibilityDescription
        )
    }

    @Test
    fun `describes maximum-only engine compatibility`() {
        val item =
            packageItem(
                maximumEngineVersion =
                    "4.0.0"
            )

        assertEquals(
            "Up to 4.0.0",
            item.engineCompatibilityDescription
        )
    }

    @Test
    fun `ordering remains deterministic when package names match`() {
        val repository =
            InMemoryContentPackageRepository()

        repository.save(
            contentPackage(
                id = "package-b",
                name = "Vocabulary",
                version = "2.0.0"
            )
        )

        repository.save(
            contentPackage(
                id = "package-c",
                name = "Vocabulary",
                version = "1.0.0"
            )
        )
        repository.save(
            contentPackage(
                id = "package-a",
                name = "Vocabulary",
                version = "1.0.0"
            )
        )

        val result =
            InstalledPackageQueryService(
                contentPackageRepository =
                    repository
            ).query()

        assertEquals(
            listOf(
                "package-a",
                "package-c",
                "package-b"
            ),
            result.map(
                InstalledPackageItem::id
            )
        )
    }

    @Test
    fun `returns empty list when no package is installed`() {
        val result =
            InstalledPackageQueryService(
                contentPackageRepository =
                    InMemoryContentPackageRepository()
            ).query()

        assertEquals(
            emptyList(),
            result
        )
    }

    private fun packageItem(
        minimumEngineVersion: String? =
            null,
        maximumEngineVersion: String? =
            null
    ): InstalledPackageItem =
        InstalledPackageItem(
            id =
                "package",
            name =
                "Package",
            version =
                "1.0.0",
            format =
                "OPD3",
            libraryCount =
                0,
            minimumEngineVersion =
                minimumEngineVersion,
            maximumEngineVersion =
                maximumEngineVersion
        )

    private fun contentPackage(
        id: String,
        name: String,
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