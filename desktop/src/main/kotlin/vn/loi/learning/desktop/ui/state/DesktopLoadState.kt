package vn.loi.learning.desktop.ui.state

sealed interface DesktopLoadState {
    data object Loading : DesktopLoadState

    data object Ready : DesktopLoadState

    data class Failed(
        val message: String
    ) : DesktopLoadState
}

fun Throwable.toDesktopFailureMessage(): String =
    message
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: this::class.simpleName
            ?.trim()
            ?.takeIf(String::isNotBlank)
        ?: "Unknown error"
