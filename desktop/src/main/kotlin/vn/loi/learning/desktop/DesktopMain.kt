package vn.loi.learning.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.nio.file.Files
import vn.loi.learning.desktop.ui.LearningApp
import vn.loi.learning.desktop.runtime.DesktopApplicationIdentity
import vn.loi.learning.desktop.runtime.DesktopRuntimeDirectoryResolver
import vn.loi.learning.infrastructure.LearningApplicationFactory

fun main() =
    application {
        val runtimeDirectories =
            DesktopRuntimeDirectoryResolver.resolve()

        Files.createDirectories(
            runtimeDirectories.data
        )

        val applicationContext =
            LearningApplicationFactory.createPersisted(
                persistenceDirectory =
                    runtimeDirectories.data
            )

        Window(
            onCloseRequest = ::exitApplication,
            title = DesktopApplicationIdentity.DISPLAY_NAME
        ) {
            LearningApp(
                applicationContext = applicationContext,
                engineName =
                    applicationContext.engine::class.simpleName
                        ?: "LearningEngine",
                dashboardName =
                    applicationContext.dashboard::class.simpleName
                        ?: "LearningDashboardQueryService"
            )
        }
    }
