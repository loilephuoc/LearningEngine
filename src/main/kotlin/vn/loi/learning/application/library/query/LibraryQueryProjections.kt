package vn.loi.learning.application.library.query

import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.InstalledPackage

/**
 * Các extension function ánh ánh từ Domain Aggregates sang Read DTOs (projections).
 */
fun InstalledPackage.toSummary(): InstalledPackageSummary =
    InstalledPackageSummary(
        id = id,
        libraryId = libraryId,
        packageId = packageId,
        topicId = topicId,
        name = name.value,
        version = version.value,
        state = state,
        installedAt = installedAt,
        contentCount = contentCount,
        learningItemCount = learningItemCount
    )

fun Collection.toSummary(): CollectionSummary =
    CollectionSummary(
        id = id,
        libraryId = libraryId,
        name = name.value,
        description = description,
        state = state,
        createdAt = createdAt,
        assignedPackageIds = assignedPackageIds,
        assignedPackagesCount = assignedPackageIds.size
    )
