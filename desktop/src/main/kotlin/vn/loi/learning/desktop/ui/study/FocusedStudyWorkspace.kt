package vn.loi.learning.desktop.ui.study

import vn.loi.learning.desktop.ui.navigation.NavigationDestination

data class FocusedStudyWorkspacePresentation(
    val active: Boolean,
    val showDashboardMetrics: Boolean,
    val showShellChrome: Boolean,
    val maxContentWidthDp: Int
)

fun resolveFocusedStudyWorkspace(activeSession: Boolean) =
    FocusedStudyWorkspacePresentation(
        active = activeSession,
        showDashboardMetrics = !activeSession,
        showShellChrome = !activeSession,
        maxContentWidthDp = if (activeSession) 1040 else 1280
    )

fun usesFocusedStudyShell(
    destination: NavigationDestination,
    activeSession: Boolean
): Boolean = destination == NavigationDestination.STUDY && activeSession
