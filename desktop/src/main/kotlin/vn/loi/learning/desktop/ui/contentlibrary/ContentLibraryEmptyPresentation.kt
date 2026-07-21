package vn.loi.learning.desktop.ui.contentlibrary

data class ContentLibraryEmptyPresentation(
    val title: String,
    val description: String,
    val actionLabel: String,
    val contentDescription: String
)

fun resolveContentLibraryEmptyPresentation():
    ContentLibraryEmptyPresentation =
    ContentLibraryEmptyPresentation(
        title = "No content libraries",
        description =
            "Import a directory containing .opd3 or .pkg files " +
                "to create your first content library.",
        actionLabel = "Import First Package",
        contentDescription =
            "Content Library is empty. " +
                "Import a directory containing OPD3 or package files " +
                "to create your first content library."
    )
