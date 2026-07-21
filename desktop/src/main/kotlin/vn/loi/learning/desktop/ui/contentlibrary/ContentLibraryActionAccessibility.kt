package vn.loi.learning.desktop.ui.contentlibrary

enum class ContentLibraryAction {
    Refresh,
    ImportPackage,
    RetryLoad,
    OpenLibrary,
    CreateCollection,
    AttachPackage,
    RenameCollection,
    DeleteCollection,
    DetachPackage
}

data class ContentLibraryActionAccessibility(
    val contentDescription: String
)

fun resolveContentLibraryActionAccessibility(
    action: ContentLibraryAction,
    targetName: String? = null
): ContentLibraryActionAccessibility {
    val target =
        targetName
            ?.trim()
            ?.takeIf(String::isNotBlank)

    val description =
        when (action) {
            ContentLibraryAction.Refresh ->
                "Refresh Content Library data."

            ContentLibraryAction.ImportPackage ->
                "Import a package directory into Content Library."

            ContentLibraryAction.RetryLoad ->
                "Retry loading Content Library data."

            ContentLibraryAction.OpenLibrary ->
                "Open library ${target ?: "Unnamed library"} and browse its lessons."

            ContentLibraryAction.CreateCollection ->
                "Create a collection in library ${target ?: "Unnamed library"}."

            ContentLibraryAction.AttachPackage ->
                "Attach an installed package to collection ${target ?: "Unnamed collection"}."

            ContentLibraryAction.RenameCollection ->
                "Rename collection ${target ?: "Unnamed collection"}."

            ContentLibraryAction.DeleteCollection ->
                "Delete collection ${target ?: "Unnamed collection"}. This action requires confirmation."

            ContentLibraryAction.DetachPackage ->
                "Detach package ${target ?: "Unnamed package"} from its collection. This action requires confirmation."
        }

    return ContentLibraryActionAccessibility(
        contentDescription = description
    )
}
