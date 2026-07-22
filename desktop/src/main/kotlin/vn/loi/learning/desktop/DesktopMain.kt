package vn.loi.learning.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.desktop.ui.LearningApp
import vn.loi.learning.desktop.runtime.DesktopApplicationIdentity
import vn.loi.learning.infrastructure.LearningApplicationFactory

fun main() =
    application {
        val persistenceDirectory =
            resolvePersistenceDirectory()

        Files.createDirectories(
            persistenceDirectory
        )

        val applicationContext =
            LearningApplicationFactory.createPersisted(
                persistenceDirectory =
                    persistenceDirectory
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

private fun resolvePersistenceDirectory(): Path =
    Path.of(
        System.getProperty("user.home"),
        ".learning-engine",
        "data"
    )
