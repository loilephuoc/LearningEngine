package vn.loi.learning.desktop.ui

import androidx.compose.runtime.Composable
import vn.loi.learning.desktop.ui.shell.LearningShell
import vn.loi.learning.desktop.ui.theme.LearningTheme
import vn.loi.learning.infrastructure.LearningApplicationContext

@Composable
fun LearningApp(
    applicationContext: LearningApplicationContext,
    engineName: String,
    dashboardName: String
) {
    LearningTheme {
        LearningShell(
            applicationContext = applicationContext,
            engineName = engineName,
            dashboardName = dashboardName
        )
    }
}
