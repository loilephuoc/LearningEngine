package vn.loi.learning.desktop.ui.contentlibrary

data class ContentLibraryCardAccessibility(
    val title: String,
    val contentDescription: String
)

data class ContentLibraryPropertyAccessibility(
    val label: String,
    val value: String,
    val contentDescription: String
)

fun resolveLibraryCardAccessibility(
    item: ContentLibraryItem
): ContentLibraryCardAccessibility {
    val title =
        item.name.trim().ifBlank {
            "Unnamed library"
        }

    val contentCount =
        item.contentCount.coerceAtLeast(0)

    val learningItemCount =
        item.learningItemCount.coerceAtLeast(0)

    val collectionCount =
        item.collectionCount.coerceAtLeast(0)

    return ContentLibraryCardAccessibility(
        title = title,
        contentDescription =
            "$title. " +
                "${pluralize(contentCount, "content", "contents")}. " +
                "${pluralize(learningItemCount, "learning item", "learning items")}. " +
                "${pluralize(collectionCount, "collection", "collections")}."
    )
}

fun resolveCollectionCardAccessibility(
    item: ContentLibraryCollectionItem
): ContentLibraryCardAccessibility {
    val title =
        item.name.trim().ifBlank {
            "Unnamed collection"
        }

    val packageCount =
        item.packageCount.coerceAtLeast(0)

    val packageSummary =
        if (packageCount == 0) {
            "No packages attached"
        } else {
            pluralize(
                packageCount,
                "package attached",
                "packages attached"
            )
        }

    return ContentLibraryCardAccessibility(
        title = title,
        contentDescription =
            "$title. $packageSummary."
    )
}

fun resolveAttachedPackageCardAccessibility(
    item: ContentLibraryAttachedPackageItem
): ContentLibraryCardAccessibility {
    val title =
        item.name.trim().ifBlank {
            "Unnamed package"
        }

    val details =
        buildList {
            item.version
                .trim()
                .takeIf(String::isNotBlank)
                ?.let {
                    add("Version $it")
                }

            item.format
                .trim()
                .takeIf(String::isNotBlank)
                ?.let {
                    add("Format $it")
                }
        }

    val description =
        if (details.isEmpty()) {
            "$title. Attached package."
        } else {
            "$title. ${details.joinToString(". ")}. Attached package."
        }

    return ContentLibraryCardAccessibility(
        title = title,
        contentDescription = description
    )
}

fun resolveInstalledPackageCardAccessibility(
    item: ContentLibraryPackageItem
): ContentLibraryCardAccessibility {
    val title =
        item.name.trim().ifBlank {
            "Unnamed package"
        }

    val version =
        item.version.trim().ifBlank {
            "Unavailable"
        }

    val format =
        item.format.trim().ifBlank {
            "Unavailable"
        }

    val libraryCount =
        item.libraryCount.coerceAtLeast(0)

    return ContentLibraryCardAccessibility(
        title = title,
        contentDescription =
            "$title. Version $version. Format $format. " +
                "${pluralize(libraryCount, "library", "libraries")}."
    )
}

fun resolveContentLibraryPropertyAccessibility(
    label: String,
    value: String
): ContentLibraryPropertyAccessibility {
    val normalizedLabel =
        label.trim().ifBlank {
            "Property"
        }

    val normalizedValue =
        value.trim().ifBlank {
            "Unavailable"
        }

    return ContentLibraryPropertyAccessibility(
        label = normalizedLabel,
        value = normalizedValue,
        contentDescription =
            "$normalizedLabel: $normalizedValue."
    )
}

private fun pluralize(
    count: Int,
    singular: String,
    plural: String
): String =
    if (count == 1) {
        "1 $singular"
    } else {
        "$count $plural"
    }
