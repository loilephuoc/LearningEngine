package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.navigation.NavigationDestination

class FocusedStudyWorkspaceTest {
    @Test
    fun `active session suppresses dashboard metrics and shell chrome`() {
        val presentation = resolveFocusedStudyWorkspace(activeSession = true)

        assertTrue(presentation.active)
        assertFalse(presentation.showDashboardMetrics)
        assertFalse(presentation.showShellChrome)
        assertTrue(presentation.maxContentWidthDp <= 1040)
    }

    @Test
    fun `normal shell remains outside an active Learn destination`() {
        assertFalse(usesFocusedStudyShell(NavigationDestination.DASHBOARD, activeSession = true))
        assertFalse(usesFocusedStudyShell(NavigationDestination.STUDY, activeSession = false))
        assertTrue(usesFocusedStudyShell(NavigationDestination.STUDY, activeSession = true))
    }
}
