package vn.loi.learning.desktop.ui.startup

enum class DesktopStartupState {
    STARTING,
    READY;

    fun complete(): DesktopStartupState = READY
}
