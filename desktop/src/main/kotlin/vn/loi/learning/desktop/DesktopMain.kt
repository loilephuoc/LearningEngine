package vn.loi.learning.desktop

import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.math.roundToInt
import java.awt.FileDialog
import java.nio.file.Path
import vn.loi.learning.desktop.ui.LearningApp
import vn.loi.learning.desktop.runtime.DesktopApplicationIdentity
import vn.loi.learning.desktop.runtime.DesktopRuntimeLifecycle

fun main() {
    val runtime = DesktopRuntimeLifecycle.start()

    try {
        application {
            var runtimeConfiguration by
                remember {
                    mutableStateOf(runtime.configuration)
                }
            val initialPlacement = runtime.windowPlacement.initial
            val windowState =
                rememberWindowState(
                    placement =
                        if (initialPlacement.maximized) {
                            WindowPlacement.Maximized
                        } else {
                            WindowPlacement.Floating
                        },
                    position =
                        if (initialPlacement.x != null && initialPlacement.y != null) {
                            WindowPosition(
                                initialPlacement.x.dp,
                                initialPlacement.y.dp
                            )
                        } else {
                            WindowPosition(Alignment.Center)
                        },
                    size =
                        DpSize(
                            initialPlacement.width.dp,
                            initialPlacement.height.dp
                        )
                )

            Window(
                onCloseRequest = {
                    val absolutePosition =
                        windowState.position as? WindowPosition.Absolute
                    runtime.windowPlacement.save(
                        vn.loi.learning.desktop.runtime.DesktopWindowPlacement(
                            width =
                                windowState.size.width.value.roundToInt().coerceIn(
                                    vn.loi.learning.desktop.runtime.DesktopWindowPlacement.MIN_WIDTH,
                                    vn.loi.learning.desktop.runtime.DesktopWindowPlacement.MAX_WIDTH
                                ),
                            height =
                                windowState.size.height.value.roundToInt().coerceIn(
                                    vn.loi.learning.desktop.runtime.DesktopWindowPlacement.MIN_HEIGHT,
                                    vn.loi.learning.desktop.runtime.DesktopWindowPlacement.MAX_HEIGHT
                                ),
                            x = absolutePosition?.x?.value?.roundToInt()?.coerceIn(
                                vn.loi.learning.desktop.runtime.DesktopWindowPlacement.MIN_POSITION,
                                vn.loi.learning.desktop.runtime.DesktopWindowPlacement.MAX_POSITION
                            ),
                            y = absolutePosition?.y?.value?.roundToInt()?.coerceIn(
                                vn.loi.learning.desktop.runtime.DesktopWindowPlacement.MIN_POSITION,
                                vn.loi.learning.desktop.runtime.DesktopWindowPlacement.MAX_POSITION
                            ),
                            maximized = windowState.placement == WindowPlacement.Maximized
                        )
                    )
                    exitApplication()
                },
                state = windowState,
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
                    runtimeDiagnostics = runtime.diagnostics,
                    runtimeConfiguration = runtimeConfiguration,
                    onRuntimeConfigurationChanged = { updated ->
                        runtime.updateConfiguration(updated)
                        runtimeConfiguration = updated
                    },
                    onExportDiagnostics = {
                        val dialog =
                            FileDialog(
                                window,
                                "Export Learning Engine diagnostics",
                                FileDialog.SAVE
                            ).apply {
                                file = vn.loi.learning.desktop.runtime.DesktopDiagnosticExporter.FILE_NAME
                                isVisible = true
                            }
                        val selectedDirectory = dialog.directory
                        val selectedFile = dialog.file
                        if (selectedDirectory == null || selectedFile == null) {
                            null
                        } else {
                            runtime.exportDiagnostics(
                                Path.of(selectedDirectory).resolve(selectedFile)
                            ).toString()
                        }
                    },
                    onCreateBackup = {
                        chooseRecoveryFile(window, FileDialog.SAVE)
                            ?.let(runtime.recovery::createBackup)
                            ?.toString()
                    },
                    onRestoreBackup = { operationActive ->
                        chooseRecoveryFile(window, FileDialog.LOAD)?.let { source ->
                            runtime.recovery.restore(source, operationActive)
                            exitApplication()
                            source.toString()
                        }
                    }
                )
            }
        }
    } finally {
        runtime.close()
    }
}

private fun chooseRecoveryFile(window: java.awt.Frame, mode: Int): Path? {
    val dialog =
        FileDialog(window, "Learning Engine backup", mode).apply {
            file =
                "learning-engine-backup." +
                    vn.loi.learning.desktop.runtime.DesktopRecoveryManager.FILE_EXTENSION
            isVisible = true
        }
    return if (dialog.directory == null || dialog.file == null) null
    else Path.of(dialog.directory).resolve(dialog.file)
}
