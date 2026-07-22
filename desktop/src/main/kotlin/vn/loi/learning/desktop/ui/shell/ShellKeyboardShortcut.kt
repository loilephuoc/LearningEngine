package vn.loi.learning.desktop.ui.shell

import vn.loi.learning.desktop.ui.navigation.NavigationDestination

enum class ShellKeyboardKey {
    F1,
    F2,
    F3,
    F4,
    F5,
    F6,
    PAGE_UP,
    PAGE_DOWN,
    R
}

sealed interface ShellKeyboardAction {
    data class Navigate(
        val destination: NavigationDestination
    ) : ShellKeyboardAction

    data object NavigatePrevious :
        ShellKeyboardAction

    data object NavigateNext :
        ShellKeyboardAction

    data object RefreshCurrent :
        ShellKeyboardAction
}

fun resolveShellKeyboardAction(
    key: ShellKeyboardKey,
    controlPressed: Boolean,
    shiftPressed: Boolean
): ShellKeyboardAction? =
    when {
        !controlPressed &&
            !shiftPressed &&
            key == ShellKeyboardKey.F1 ->
            ShellKeyboardAction.Navigate(
                NavigationDestination.DASHBOARD
            )

        !controlPressed &&
            !shiftPressed &&
            key == ShellKeyboardKey.F2 ->
            ShellKeyboardAction.Navigate(
                NavigationDestination.STUDY
            )

        !controlPressed &&
            !shiftPressed &&
            key == ShellKeyboardKey.F3 ->
            ShellKeyboardAction.Navigate(
                NavigationDestination.STATISTICS
            )

        !controlPressed &&
            !shiftPressed &&
            key == ShellKeyboardKey.F4 ->
            ShellKeyboardAction.Navigate(
                NavigationDestination.REVIEW_HISTORY
            )

        !controlPressed &&
            !shiftPressed &&
            key == ShellKeyboardKey.F5 ->
            ShellKeyboardAction.Navigate(
                NavigationDestination.CONTENT_LIBRARY
            )

        !controlPressed &&
            !shiftPressed &&
            key == ShellKeyboardKey.F6 ->
            ShellKeyboardAction.Navigate(
                NavigationDestination.SETTINGS
            )

        controlPressed &&
            !shiftPressed &&
            key == ShellKeyboardKey.PAGE_UP ->
            ShellKeyboardAction.NavigatePrevious

        controlPressed &&
            !shiftPressed &&
            key == ShellKeyboardKey.PAGE_DOWN ->
            ShellKeyboardAction.NavigateNext

        controlPressed &&
            shiftPressed &&
            key == ShellKeyboardKey.R ->
            ShellKeyboardAction.RefreshCurrent

        else -> null
    }

fun shellKeyboardHint(): String =
    "Global shortcuts: F1 Home, F2 Learn, F3 Statistics, " +
        "F4 Review, F5 Library, F6 Settings, " +
        "Ctrl+PageUp previous screen, Ctrl+PageDown next screen, " +
        "Ctrl+Shift+R refresh current screen."

fun NavigationDestination.shortcutLabel(): String =
    when (this) {
        NavigationDestination.DASHBOARD -> "F1"
        NavigationDestination.STUDY -> "F2"
        NavigationDestination.STATISTICS -> "F3"
        NavigationDestination.REVIEW_HISTORY -> "F4"
        NavigationDestination.CONTENT_LIBRARY -> "F5"
        NavigationDestination.SETTINGS -> "F6"
    }
