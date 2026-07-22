package vn.loi.learning.desktop.ui.shell

enum class ShellFocusRegion {
    NAVIGATION,
    CONTENT;

    fun next(): ShellFocusRegion =
        when (this) {
            NAVIGATION -> CONTENT
            CONTENT -> NAVIGATION
        }

    fun previous(): ShellFocusRegion = next()
}
