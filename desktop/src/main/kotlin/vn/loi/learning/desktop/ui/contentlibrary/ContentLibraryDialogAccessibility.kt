package vn.loi.learning.desktop.ui.contentlibrary

data class ContentLibraryDialogAccessibility(
    val title: String,
    val contentDescription: String
)

data class AttachPackageOptionAccessibility(
    val name: String,
    val contentDescription: String,
    val selected: Boolean
)

fun resolveCreateCollectionDialogAccessibility(
    libraryName: String
): ContentLibraryDialogAccessibility {
    val library =
        libraryName.trim().ifBlank {
            "Unnamed library"
        }

    return ContentLibraryDialogAccessibility(
        title = "Create Collection",
        contentDescription =
            "Create Collection dialog. Library: $library. Enter a collection name."
    )
}

fun resolveRenameCollectionDialogAccessibility(
    currentName: String
): ContentLibraryDialogAccessibility {
    val collection =
        currentName.trim().ifBlank {
            "Unnamed collection"
        }

    return ContentLibraryDialogAccessibility(
        title = "Rename Collection",
        contentDescription =
            "Rename Collection dialog. Current name: $collection. Enter a new collection name."
    )
}

fun resolveDeleteCollectionDialogAccessibility(
    collectionName: String
): ContentLibraryDialogAccessibility {
    val collection =
        collectionName.trim().ifBlank {
            "Unnamed collection"
        }

    return ContentLibraryDialogAccessibility(
        title = "Delete Collection",
        contentDescription =
            "Delete Collection dialog. Collection: $collection. This action cannot be undone."
    )
}

fun resolveDetachPackageDialogAccessibility(
    packageName: String,
    collectionName: String
): ContentLibraryDialogAccessibility {
    val pkg =
        packageName.trim().ifBlank {
            "Unnamed package"
        }

    val collection =
        collectionName.trim().ifBlank {
            "Unnamed collection"
        }

    return ContentLibraryDialogAccessibility(
        title = "Detach Package",
        contentDescription =
            "Detach Package dialog. Package: $pkg. Collection: $collection. The installed package will not be deleted."
    )
}

fun resolveAttachPackageDialogAccessibility(
    collectionName: String,
    availablePackageCount: Int
): ContentLibraryDialogAccessibility {
    val collection =
        collectionName.trim().ifBlank {
            "Unnamed collection"
        }

    val count =
        availablePackageCount.coerceAtLeast(0)

    val countLabel =
        if (count == 1) {
            "1 package available"
        } else {
            "$count packages available"
        }

    return ContentLibraryDialogAccessibility(
        title = "Attach Package",
        contentDescription =
            "Attach Package dialog. Collection: $collection. $countLabel. Select one package."
    )
}

fun resolveAttachPackageOptionAccessibility(
    packageName: String,
    selected: Boolean
): AttachPackageOptionAccessibility {
    val name =
        packageName.trim().ifBlank {
            "Unnamed package"
        }

    return AttachPackageOptionAccessibility(
        name = name,
        selected = selected,
        contentDescription =
            if (selected) {
                "$name. Selected package."
            } else {
                "$name. Package option."
            }
    )
}
