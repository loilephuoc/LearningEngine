package vn.loi.learning.desktop

import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.math.roundToInt
import java.awt.FileDialog
import java.awt.GraphicsConfiguration
import java.awt.Toolkit
import java.nio.file.Path
import vn.loi.learning.desktop.ui.LearningApp
import vn.loi.learning.desktop.notification.DesktopVocabularyReminderPopupState
import vn.loi.learning.desktop.notification.DesktopVocabularyReminderPopupWindow
import vn.loi.learning.desktop.notification.DesktopVocabularyReminderImageViewer
import vn.loi.learning.desktop.runtime.DesktopApplicationIdentity
import vn.loi.learning.desktop.runtime.DesktopRuntimeLifecycle
import vn.loi.learning.desktop.runtime.DesktopReleaseStartupVerification
import vn.loi.learning.infrastructure.contentmedia.JvmContentMediaStorage

fun main() {
    val runtime = DesktopRuntimeLifecycle.start()
    val contentMediaStorage = JvmContentMediaStorage(runtime.directories.data.resolve("media"))

    try {
        if (isDesktopStartupVerificationRequested()) {
            Toolkit.getDefaultToolkit()
            val verification = DesktopReleaseStartupVerification.verify()
            println("LE_STARTUP_VERIFICATION=passed")
            println("LE_AUDIO_PROVIDER_PROBE=passed")
            println("LE_AUDIO_FILE_READERS=${verification.audioFileReaders.joinToString(",")}")
            println("LE_AUDIO_CONVERSION_PROVIDERS=${verification.formatConversionProviders.joinToString(",")}")
            return
        }

        application {
            var runtimeConfiguration by
                remember {
                    mutableStateOf(runtime.configuration)
                }
            var mainGraphicsConfiguration by
                remember { mutableStateOf<GraphicsConfiguration?>(null) }
            var mainFrame by remember { mutableStateOf<java.awt.Frame?>(null) }
            var mainWindowVisible by remember { mutableStateOf(true) }
            var exitRequested by remember { mutableStateOf(false) }
            val popupController = runtime.vocabularyReminderPopupController
            val popupState =
                popupController?.state?.collectAsState()?.value
                    ?: DesktopVocabularyReminderPopupState.Hidden
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

            LaunchedEffect(windowState.isMinimized) {
                if (windowState.isMinimized && mainWindowVisible) {
                    mainWindowVisible = false
                    windowState.isMinimized = false
                    runtime.vocabularyReminderRuntime?.setBackgroundMode(true)
                }
            }

            val trayIcon = rememberVectorPainter(Icons.Default.School)
            Tray(
                icon = trayIcon,
                tooltip = DesktopApplicationIdentity.DISPLAY_NAME,
                onAction = {
                    mainWindowVisible = true
                    windowState.isMinimized = false
                    runtime.vocabularyReminderRuntime?.setBackgroundMode(false)
                    mainFrame?.run { toFront(); requestFocus() }
                },
                menu = {
                    Item("Open Learning Engine", onClick = {
                        mainWindowVisible = true
                        windowState.isMinimized = false
                        runtime.vocabularyReminderRuntime?.setBackgroundMode(false)
                        mainFrame?.run { toFront(); requestFocus() }
                    })
                    val paused = runtime.vocabularyReminderRuntime?.settings?.pausedUntil != null
                    Item(if (paused) "Resume reminders" else "Pause reminders", onClick = {
                        if (paused) runtime.vocabularyReminderRuntime?.resumeNow()
                        else runtime.vocabularyReminderRuntime?.pauseFor30Minutes()
                    })
                    Separator()
                    Item("Exit", onClick = { exitRequested = true })
                }
            )
            if (exitRequested) exitApplication()

            Window(
                visible = mainWindowVisible,
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
                SideEffect {
                    mainGraphicsConfiguration = window.graphicsConfiguration
                    mainFrame = window
                }
                LearningApp(
                    applicationContext = runtime.applicationContext,
                    contentMediaStorage = contentMediaStorage,
                    engineName =
                        runtime.applicationContext.engine::class.simpleName
                            ?: "LearningEngine",
                    dashboardName =
                        runtime.applicationContext.dashboard::class.simpleName
                            ?: "LearningDashboardQueryService",
                    runtimeDiagnostics = runtime.diagnostics,
                    runtimeConfiguration = runtimeConfiguration,
                    vocabularyReminderSettingsController = runtime.vocabularyReminderSettingsController,
                    studySessionPolicyProvider = runtime::loadStudySessionPolicy,
                    onboardingRequired =
                        runtime.onboarding.initial ==
                            vn.loi.learning.desktop.runtime.DesktopOnboardingState.REQUIRED,
                    onCompleteOnboarding = runtime::completeOnboarding,
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

            if (popupController != null && popupState is DesktopVocabularyReminderPopupState.Visible) {
                DesktopVocabularyReminderPopupWindow(
                    visible = popupState,
                    controller = popupController,
                    contentMediaStorage = contentMediaStorage,
                    mainGraphicsConfiguration = mainGraphicsConfiguration,
                    themePreference = runtimeConfiguration.theme,
                    onLocationChanged = { newLoc ->
                        runtime.vocabularyReminderSettingsController?.updatePopupLocation(newLoc)
                    }
                )
            }
            if (popupController != null && popupState is DesktopVocabularyReminderPopupState.FullImage) {
                DesktopVocabularyReminderImageViewer(
                    visible = popupState,
                    controller = popupController,
                    storage = contentMediaStorage,
                    graphicsConfiguration = mainGraphicsConfiguration
                )
            }
        }
    } finally {
        runtime.close()
    }
}

internal fun isDesktopStartupVerificationRequested(
    value: String? = System.getProperty("learningEngine.startupVerification")
): Boolean = value?.toBooleanStrictOrNull() == true

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
