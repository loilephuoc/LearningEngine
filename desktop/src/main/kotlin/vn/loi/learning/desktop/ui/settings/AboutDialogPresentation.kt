package vn.loi.learning.desktop.ui.settings

import vn.loi.learning.desktop.runtime.DesktopRuntimeDiagnostics

data class AboutDialogPresentation(
    val title: String,
    val version: String,
    val supportSummary: String
)

fun resolveAboutDialogPresentation(
    diagnostics: DesktopRuntimeDiagnostics
): AboutDialogPresentation =
    AboutDialogPresentation(
        title = diagnostics.applicationName,
        version = "Version ${diagnostics.version}",
        supportSummary = diagnostics.supportSummary()
    )
