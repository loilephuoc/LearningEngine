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
            "Import a directory containing an OPD3 (.opd3) bundle, a supported ZIP .pkg, " +
                "or a matching JSON + OPD3 .pkg pair " +
                "to create your first content library.",
        actionLabel = "Import First Package",
        contentDescription =
            "Content Library is empty. " +
                "Import a directory containing an OPD3 bundle, supported ZIP package, " +
                "or matching JSON and OPD3 PKG pair " +
                "to create your first content library."
    )
