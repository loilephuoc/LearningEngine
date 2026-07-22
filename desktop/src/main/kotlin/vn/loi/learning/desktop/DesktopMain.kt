package vn.loi.learning.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import vn.loi.learning.desktop.ui.LearningApp
import vn.loi.learning.desktop.runtime.DesktopApplicationIdentity
import vn.loi.learning.desktop.runtime.DesktopRuntimeLifecycle

fun main() {
    val runtime = DesktopRuntimeLifecycle.start()

    try {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = DesktopApplicationIdentity.DISPLAY_NAME
            ) {
                LearningApp(
                    applicationContext = runtime.applicationContext,
                    engineName =
                        runtime.applicationContext.engine::class.simpleName
                            ?: "LearningEngine",
                    dashboardName =
                        runtime.applicationContext.dashboard::class.simpleName
                            ?: "LearningDashboardQueryService",
                    runtimeDiagnostics = runtime.diagnostics
                )
            }
        }
    } finally {
        runtime.close()
    }
}
