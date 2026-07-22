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
import vn.loi.learning.desktop.ui.startup.OnboardingScreen

@Composable
fun LearningApp(
    applicationContext: LearningApplicationContext,
    engineName: String,
    dashboardName: String,
    runtimeDiagnostics: DesktopRuntimeDiagnostics,
    runtimeConfiguration: DesktopRuntimeConfiguration,
    onboardingRequired: Boolean,
    onCompleteOnboarding: (Boolean) -> Unit,
    onRuntimeConfigurationChanged: (DesktopRuntimeConfiguration) -> Unit,
    onExportDiagnostics: () -> String?,
    onCreateBackup: () -> String?,
    onRestoreBackup: (Boolean) -> String?
) {
    var startupState by remember { mutableStateOf(DesktopStartupState.STARTING) }
    var showOnboarding by remember { mutableStateOf(onboardingRequired) }
    LaunchedEffect(Unit) { startupState = startupState.complete() }

    LearningTheme(
        preference = runtimeConfiguration.theme
    ) {
        if (startupState == DesktopStartupState.STARTING) {
            StartupScreen(DesktopLocalization.strings(runtimeConfiguration.locale).startup)
        } else if (showOnboarding) {
            val strings = DesktopLocalization.strings(runtimeConfiguration.locale)
            OnboardingScreen(
                title = strings.onboardingTitle,
                message = strings.onboardingMessage,
                sampleLabel = strings.installSample,
                skipLabel = strings.skipSample,
                onInstallSample = {
                    onCompleteOnboarding(true)
                    showOnboarding = false
                },
                onSkip = {
                    onCompleteOnboarding(false)
                    showOnboarding = false
                }
            )
        } else {
            LearningShell(
                applicationContext = applicationContext,
                engineName = engineName,
                dashboardName = dashboardName,
                runtimeDiagnostics = runtimeDiagnostics,
                runtimeConfiguration = runtimeConfiguration,
                onRuntimeConfigurationChanged = onRuntimeConfigurationChanged,
                onExportDiagnostics = onExportDiagnostics,
                onCreateBackup = onCreateBackup,
                onRestoreBackup = onRestoreBackup
            )
        }
    }
}
