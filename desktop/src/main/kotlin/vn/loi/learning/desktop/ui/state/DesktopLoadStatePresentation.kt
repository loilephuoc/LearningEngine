package vn.loi.learning.desktop.ui.state

data class DesktopLoadStatePresentation(
    val title: String,
    val description: String,
    val actionLabel: String?,
    val contentDescription: String
)

fun resolveDesktopLoadStatePresentation(
    state: DesktopLoadState,
    screenName: String
): DesktopLoadStatePresentation? {
    val normalizedScreenName =
        screenName.trim().ifBlank {
            "Screen"
        }

    return when (state) {
        DesktopLoadState.Loading ->
            DesktopLoadStatePresentation(
                title = "Loading $normalizedScreenName",
                description =
                    "Please wait while the latest data is loaded.",
                actionLabel = null,
                contentDescription =
                    "$normalizedScreenName is loading. " +
                        "Please wait while the latest data is loaded."
            )

        DesktopLoadState.Ready ->
            null

        is DesktopLoadState.Failed -> {
            val detail =
                state.message.trim().ifBlank {
                    "Unknown error"
                }

            DesktopLoadStatePresentation(
                title = "$normalizedScreenName could not be loaded",
                description =
                    "$detail. Your previous data remains available when possible.",
                actionLabel = "Retry",
                contentDescription =
                    "$normalizedScreenName could not be loaded. " +
                        "$detail. Previous data remains available when possible. " +
                        "Retry is available."
            )
        }
    }
}
