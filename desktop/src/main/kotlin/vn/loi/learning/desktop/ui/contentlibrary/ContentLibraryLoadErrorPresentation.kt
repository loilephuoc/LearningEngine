package vn.loi.learning.desktop.ui.contentlibrary

data class ContentLibraryLoadErrorPresentation(
    val title: String,
    val message: String,
    val guidance: String,
    val actionLabel: String,
    val contentDescription: String
)

fun resolveContentLibraryLoadErrorPresentation(
    rawMessage: String
): ContentLibraryLoadErrorPresentation {
    val normalizedMessage =
        rawMessage.trim().ifBlank {
            "The content library could not be loaded."
        }

    return ContentLibraryLoadErrorPresentation(
        title = "Content Library unavailable",
        message = normalizedMessage,
        guidance =
            "Check that the local content data is accessible, " +
                "then retry loading the library.",
        actionLabel = "Retry",
        contentDescription =
            "Content Library unavailable. " +
                normalizedMessage +
                " Check that the local content data is accessible, " +
                "then retry loading the library."
    )
}
