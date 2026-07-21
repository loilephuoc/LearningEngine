package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage

data class InstalledPackageItem(
    val id: String,
    val name: String,
    val version: String,
    val format: String,
    val libraryCount: Int,
    val libraryIds: List<String> =
        emptyList(),
    val schemaVersion: Int =
        1,
    val minimumEngineVersion: String? =
        null,
    val maximumEngineVersion: String? =
        null,
    val dependencyCount: Int =
        0
) {

    val hasLibraries: Boolean
        get() =
            libraryCount > 0

    val hasDependencies: Boolean
        get() =
            dependencyCount > 0

    val engineCompatibilityDescription: String
        get() =
            when {
                minimumEngineVersion != null &&
                        maximumEngineVersion != null ->
                    "$minimumEngineVersion - $maximumEngineVersion"

                minimumEngineVersion != null ->
                    "$minimumEngineVersion+"

                maximumEngineVersion != null ->
                    "Up to $maximumEngineVersion"

                else ->
                    "Any supported engine version"
            }

    fun containsLibrary(
        libraryId: String
    ): Boolean =
        libraryIds.any { installedLibraryId ->
            installedLibraryId ==
                    libraryId
        }
}

class InstalledPackageQueryService(
    private val contentPackageRepository:
    ContentPackageRepository
) {

    fun query(): List<InstalledPackageItem> =
        contentPackageRepository
            .findAll()
            .map { contentPackage ->
                contentPackage.toInstalledPackageItem()
            }
            .sortedWith(
                installedPackageComparator
            )

    fun findById(
        packageId: String
    ): InstalledPackageItem? {
        val normalizedPackageId =
            packageId.trim()

        if (
            normalizedPackageId.isEmpty()
        ) {
            return null
        }

        return query()
            .firstOrNull { item ->
                item.id ==
                        normalizedPackageId
            }
    }

    fun findByLibraryId(
        libraryId: String
    ): InstalledPackageItem? {
        val normalizedLibraryId =
            libraryId.trim()

        if (
            normalizedLibraryId.isEmpty()
        ) {
            return null
        }

        return query()
            .firstOrNull { item ->
                item.containsLibrary(
                    normalizedLibraryId
                )
            }
    }

    fun queryByLibraryIds(
        libraryIds: Collection<String>
    ): List<InstalledPackageItem> {
        val normalizedLibraryIds =
            libraryIds
                .asSequence()
                .map(
                    String::trim
                )
                .filter(
                    String::isNotEmpty
                )
                .toSet()

        if (
            normalizedLibraryIds.isEmpty()
        ) {
            return emptyList()
        }

        return query()
            .filter { item ->
                item.libraryIds.any { libraryId ->
                    libraryId in
                            normalizedLibraryIds
                }
            }
    }

    fun queryByFormat(
        format: String
    ): List<InstalledPackageItem> {
        val normalizedFormat =
            format.trim()

        if (
            normalizedFormat.isEmpty()
        ) {
            return emptyList()
        }

        return query()
            .filter { item ->
                item.format.equals(
                    other =
                        normalizedFormat,
                    ignoreCase =
                        true
                )
            }
    }

    fun queryWithLibraries():
            List<InstalledPackageItem> =
        query()
            .filter(
                InstalledPackageItem::hasLibraries
            )

    fun queryWithoutLibraries():
            List<InstalledPackageItem> =
        query()
            .filterNot(
                InstalledPackageItem::hasLibraries
            )

    fun queryWithDependencies():
            List<InstalledPackageItem> =
        query()
            .filter(
                InstalledPackageItem::hasDependencies
            )

    fun queryWithoutDependencies():
            List<InstalledPackageItem> =
        query()
            .filterNot(
                InstalledPackageItem::hasDependencies
            )

    fun search(
        text: String
    ): List<InstalledPackageItem> {
        val normalizedText =
            text.trim()

        if (
            normalizedText.isEmpty()
        ) {
            return query()
        }

        return query()
            .filter { item ->
                item.matches(
                    normalizedText
                )
            }
    }

    private fun InstalledPackageItem.matches(
        text: String
    ): Boolean =
        id.contains(
            other =
                text,
            ignoreCase =
                true
        ) ||
                name.contains(
                    other =
                        text,
                    ignoreCase =
                        true
                ) ||
                version.contains(
                    other =
                        text,
                    ignoreCase =
                        true
                ) ||
                format.contains(
                    other =
                        text,
                    ignoreCase =
                        true
                ) ||
                libraryIds.any { libraryId ->
                    libraryId.contains(
                        other =
                            text,
                        ignoreCase =
                            true
                    )
                }

    private companion object {

        val installedPackageComparator:
                Comparator<InstalledPackageItem> =
            compareBy(
                { item ->
                    item.name.lowercase()
                },
                { item ->
                    item.version.lowercase()
                },
                InstalledPackageItem::id
            )

        fun ContentPackage.toInstalledPackageItem():
                InstalledPackageItem {
            val sortedLibraryIds =
                libraryIds
                    .map { libraryId ->
                        libraryId.toString()
                    }
                    .sorted()

            return InstalledPackageItem(
                id =
                    id.toString(),
                name =
                    name,
                version =
                    version,
                format =
                    format,
                libraryCount =
                    sortedLibraryIds.size,
                libraryIds =
                    sortedLibraryIds,
                schemaVersion =
                    descriptor.schemaVersion,
                minimumEngineVersion =
                    descriptor.minimumEngineVersion,
                maximumEngineVersion =
                    descriptor.maximumEngineVersion,
                dependencyCount =
                    descriptor.dependencies.size
            )
        }
    }
}