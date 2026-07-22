package vn.loi.learning.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import vn.loi.learning.desktop.ui.shell.LearningShell
import vn.loi.learning.desktop.ui.theme.LearningTheme
import vn.loi.learning.desktop.runtime.DesktopRuntimeDiagnostics
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfiguration
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.desktop.ui.localization.DesktopLocalization
import vn.loi.learning.desktop.ui.startup.DesktopStartupState
import vn.loi.learning.desktop.ui.startup.StartupScreen

@Composable
fun LearningApp(
    applicationContext: LearningApplicationContext,
    engineName: String,
    dashboardName: String,
    runtimeDiagnostics: DesktopRuntimeDiagnostics,
    runtimeConfiguration: DesktopRuntimeConfiguration,
    onRuntimeConfigurationChanged: (DesktopRuntimeConfiguration) -> Unit
) {
    var startupState by remember { mutableStateOf(DesktopStartupState.STARTING) }
    LaunchedEffect(Unit) { startupState = startupState.complete() }

    LearningTheme(
        preference = runtimeConfiguration.theme
    ) {
        if (startupState == DesktopStartupState.STARTING) {
            StartupScreen(DesktopLocalization.strings(runtimeConfiguration.locale).startup)
        } else {
            LearningShell(
            applicationContext = applicationContext,
            engineName = engineName,
            dashboardName = dashboardName,
            runtimeDiagnostics = runtimeDiagnostics,
            runtimeConfiguration = runtimeConfiguration,
            onRuntimeConfigurationChanged = onRuntimeConfigurationChanged
            )
        }
    }
}
