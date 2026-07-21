package vn.loi.learning.desktop.ui.contentlibrary

data class ContentLibraryHeaderAccessibility(
    val summary: String,
    val contentDescription: String
)

data class ContentLibraryMessageAccessibility(
    val message: String,
    val contentDescription: String
)

fun resolveContentLibraryHeaderAccessibility(
    libraryCount: Int,
    collectionCount: Int,
    packageCount: Int
): ContentLibraryHeaderAccessibility {
    val libraries = libraryCount.coerceAtLeast(0)
    val collections = collectionCount.coerceAtLeast(0)
    val packages = packageCount.coerceAtLeast(0)

    val libraryLabel =
        if (libraries == 1) {
            "1 library"
        } else {
            "$libraries libraries"
        }

    val collectionLabel =
        if (collections == 1) {
            "1 collection"
        } else {
            "$collections collections"
        }

    val packageLabel =
        if (packages == 1) {
            "1 installed package"
        } else {
            "$packages installed packages"
        }

    val summary =
        "$libraryLabel · $collectionLabel · $packageLabel"

    return ContentLibraryHeaderAccessibility(
        summary = summary,
        contentDescription =
            "Content Library. $libraryLabel. $collectionLabel. $packageLabel."
    )
}

fun resolveContentLibraryMessageAccessibility(
    message: String,
    isError: Boolean
): ContentLibraryMessageAccessibility {
    val normalizedMessage =
        message.trim().ifBlank {
            if (isError) {
                "An unknown import error occurred."
            } else {
                "Import completed."
            }
        }

    val prefix =
        if (isError) {
            "Import error"
        } else {
            "Import status"
        }

    return ContentLibraryMessageAccessibility(
        message = normalizedMessage,
        contentDescription =
            "$prefix. $normalizedMessage"
    )
}

fun resolveContentLibrarySectionContentDescription(
    title: String
): String {
    val normalizedTitle =
        title.trim().ifBlank {
            "Untitled section"
        }

    return "$normalizedTitle section."
}
