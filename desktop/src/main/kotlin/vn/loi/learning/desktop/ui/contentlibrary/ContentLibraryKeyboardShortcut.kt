package vn.loi.learning.desktop.ui.contentlibrary

enum class ContentLibraryKeyboardKey {
    R,
    I,
    ESCAPE
}

enum class ContentLibraryKeyboardAction {
    Refresh,
    ImportPackage,
    ClearLessonSelection,
    CloseLessonBrowser
}

data class ContentLibraryKeyboardContext(
    val dialogVisible: Boolean,
    val lessonBrowserOpen: Boolean,
    val lessonSelected: Boolean
)

fun resolveContentLibraryKeyboardAction(
    key: ContentLibraryKeyboardKey,
    controlPressed: Boolean,
    context: ContentLibraryKeyboardContext
): ContentLibraryKeyboardAction? {
    if (context.dialogVisible) {
        return null
    }

    return when {
        controlPressed && key == ContentLibraryKeyboardKey.R ->
            ContentLibraryKeyboardAction.Refresh

        controlPressed && key == ContentLibraryKeyboardKey.I ->
            ContentLibraryKeyboardAction.ImportPackage

        !controlPressed &&
            key == ContentLibraryKeyboardKey.ESCAPE &&
            context.lessonSelected ->
            ContentLibraryKeyboardAction.ClearLessonSelection

        !controlPressed &&
            key == ContentLibraryKeyboardKey.ESCAPE &&
            context.lessonBrowserOpen ->
            ContentLibraryKeyboardAction.CloseLessonBrowser

        else -> null
    }
}

fun contentLibraryKeyboardHint(): String =
    "Shortcuts: Ctrl+R refresh, Ctrl+I import package, Escape go back."
