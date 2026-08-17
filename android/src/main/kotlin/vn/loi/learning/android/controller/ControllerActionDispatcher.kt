package vn.loi.learning.android.controller

import android.content.Context
import android.media.AudioManager
import android.util.Log
import vn.loi.learning.android.autoplay.AutoPlayEngineState
import vn.loi.learning.android.autoplay.AutoPlayRuntimeCoordinator
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Single authority executing semantic ControllerActions through existing LearningEngine authorities,
 * system volume controls, and platform system action bridges.
 */
class ControllerActionDispatcher(
    private val appContext: Context,
    private val studyBridge: StudyControllerBridge = StudyControllerBridge,
    private val systemActionBridge: ControllerSystemActionBridge = ControllerSystemActionBridge,
    private val autoPlayCoordinatorProvider: () -> AutoPlayRuntimeCoordinator? = {
        runCatching { AutoPlayRuntimeCoordinator.getInstance(appContext) }.getOrNull()
    },
    private val audioPolicy: vn.loi.learning.android.media.LearningEngineAudioPolicy = vn.loi.learning.android.media.LearningEngineAudioPolicy,
    private val systemMediaVolumeController: vn.loi.learning.android.media.SystemMediaVolumeController = vn.loi.learning.android.media.AndroidSystemMediaVolumeController(appContext),
    private val systemVolumeAdjuster: ((Int) -> Boolean)? = null
) {

    /**
     * Determines current active context across AutoPlay, Study, and Global.
     */
    fun detectCurrentContext(): ControllerContext {
        val autoPlay = autoPlayCoordinatorProvider()
        if (autoPlay != null && autoPlay.engineState.value !is AutoPlayEngineState.Idle) {
            return ControllerContext.AUTO_PLAY
        }

        val studyContext = studyBridge.currentContext()
        if (studyContext != ControllerContext.GLOBAL) {
            return studyContext
        }

        return ControllerContext.GLOBAL
    }

    /**
     * Dispatches a semantic action against existing application authorities.
     */
    suspend fun dispatch(action: ControllerAction, context: ControllerContext): ControllerActionResult {
        Log.d(ControllerInputDiagnostic.TAG, "Dispatching ControllerAction: $action in context: $context")

        return when (action) {
            ControllerAction.PLAY_PAUSE -> {
                val autoPlay = autoPlayCoordinatorProvider()
                if (autoPlay != null && autoPlay.engineState.value !is AutoPlayEngineState.Idle) {
                    val st = autoPlay.engineState.value
                    if (st is AutoPlayEngineState.Running) {
                        if (st.isPaused) autoPlay.resume() else autoPlay.pause()
                    }
                    ControllerActionResult.Executed(action, "AutoPlay")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "AutoPlay is not currently running")
                }
            }

            ControllerAction.MUTE_TOGGLE -> {
                val newMuted = audioPolicy.toggleMuted()
                ControllerDiagnosticsHolder.setGlobalMute(newMuted)
                ControllerActionResult.Executed(action, "App Audio: ${if (newMuted) "Muted" else "Unmuted"}")
            }

            ControllerAction.STOP_AUTO_PLAY -> {
                val autoPlay = autoPlayCoordinatorProvider()
                if (autoPlay != null && autoPlay.engineState.value !is AutoPlayEngineState.Idle) {
                    autoPlay.stop()
                    ControllerActionResult.Executed(action, "AutoPlay")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "AutoPlay is not active")
                }
            }

            ControllerAction.REVEAL_ANSWER -> {
                if (studyBridge.revealAnswer()) {
                    ControllerActionResult.Executed(action, "Study")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Current study state is not in a revealable question stage")
                }
            }

            ControllerAction.CONTINUE_CURRENT_MODE -> {
                if (studyBridge.continueCurrentMode()) {
                    ControllerActionResult.Executed(action, "Study: Continue Current Mode")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Continue action is not available in current study state")
                }
            }

            ControllerAction.RATE_AGAIN -> dispatchRating(action, ReviewRating.AGAIN)
            ControllerAction.RATE_HARD -> dispatchRating(action, ReviewRating.HARD)
            ControllerAction.RATE_GOOD -> dispatchRating(action, ReviewRating.GOOD)
            ControllerAction.RATE_EASY -> dispatchRating(action, ReviewRating.EASY)

            ControllerAction.NEXT_ITEM -> {
                val autoPlay = autoPlayCoordinatorProvider()
                if (autoPlay != null && autoPlay.engineState.value !is AutoPlayEngineState.Idle) {
                    autoPlay.next()
                    ControllerActionResult.Executed(action, "AutoPlay")
                } else if (studyBridge.next()) {
                    ControllerActionResult.Executed(action, "Study")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Next item is not available in current state")
                }
            }

            ControllerAction.PREVIOUS_ITEM -> {
                val autoPlay = autoPlayCoordinatorProvider()
                if (autoPlay != null && autoPlay.engineState.value !is AutoPlayEngineState.Idle) {
                    autoPlay.previous()
                    ControllerActionResult.Executed(action, "AutoPlay")
                } else if (studyBridge.previous()) {
                    ControllerActionResult.Executed(action, "Study")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Previous item is not available in current state")
                }
            }

            ControllerAction.REPLAY_PRIMARY_AUDIO -> {
                val autoPlay = autoPlayCoordinatorProvider()
                if (autoPlay != null && autoPlay.engineState.value !is AutoPlayEngineState.Idle) {
                    autoPlay.replay()
                    ControllerActionResult.Executed(action, "AutoPlay")
                } else if (studyBridge.replayAudio()) {
                    ControllerActionResult.Executed(action, "Study: Replay Audio")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Audio replay is not available in current state")
                }
            }

            ControllerAction.LOOP_PRIMARY_AUDIO -> {
                if (studyBridge.loopPrimaryAudio()) {
                    ControllerActionResult.Executed(action, "Study: Loop Primary Audio")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Primary audio loop is not available in current state")
                }
            }

            ControllerAction.PLAY_PRIMARY_EN -> {
                val autoPlay = autoPlayCoordinatorProvider()
                if (autoPlay != null && autoPlay.engineState.value !is AutoPlayEngineState.Idle) {
                    autoPlay.replay()
                    ControllerActionResult.Executed(action, "AutoPlay: Primary English")
                } else if (studyBridge.playPrimaryEnglish()) {
                    ControllerActionResult.Executed(action, "Study: Primary English")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Primary English audio is not available in current state")
                }
            }

            ControllerAction.PLAY_PRIMARY_VI -> {
                if (studyBridge.playPrimaryVietnamese()) {
                    ControllerActionResult.Executed(action, "Study: Primary Vietnamese")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Primary Vietnamese audio is not available in current state")
                }
            }

            ControllerAction.PLAY_EXAMPLE_EN -> {
                if (studyBridge.playExampleEnglish()) {
                    ControllerActionResult.Executed(action, "Study: English Example")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "English example audio is not available in current state")
                }
            }

            ControllerAction.LOOP_EXAMPLE_EN -> {
                if (studyBridge.loopExampleEnglish()) {
                    ControllerActionResult.Executed(action, "Study: Loop English Example")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "English example audio loop is not available in current state")
                }
            }

            ControllerAction.PLAY_EXAMPLE_VI -> {
                if (studyBridge.playExampleVietnamese()) {
                    ControllerActionResult.Executed(action, "Study: Vietnamese Example")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Vietnamese translation audio is not available in current state")
                }
            }

            ControllerAction.START_AUTO_PLAY -> {
                if (studyBridge.startAutoPlay()) {
                    ControllerActionResult.Executed(action, "Study: Started Auto Play")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Auto Play could not be started from current context")
                }
            }

            ControllerAction.SYSTEM_VOLUME_UP -> {
                if (systemVolumeAdjuster != null) {
                    if (systemVolumeAdjuster.invoke(AudioManager.ADJUST_RAISE)) {
                        ControllerActionResult.Executed(action, "System: Volume Raised")
                    } else {
                        ControllerActionResult.UnavailableInContext(action, "System volume adjustment unavailable")
                    }
                } else {
                    val isForeground = ControllerDiagnosticsHolder.state.value.isForeground
                    val result = systemMediaVolumeController.adjustVolume(AudioManager.ADJUST_RAISE, isForeground)
                    val status = systemMediaVolumeController.currentStatus()
                    ControllerDiagnosticsHolder.recordVolumeResult("UP", result, status)
                    when (result) {
                        is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.Success -> {
                            ControllerActionResult.Executed(action, "System: Volume Raised (${result.before} -> ${result.after})")
                        }
                        is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.Boundary -> {
                            ControllerActionResult.Executed(action, "System: ${result.message}")
                        }
                        is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.FixedVolume -> {
                            ControllerActionResult.UnavailableInContext(action, result.message)
                        }
                        is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.NoChange -> {
                            ControllerActionResult.UnavailableInContext(action, result.message)
                        }
                        is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.Error -> {
                            ControllerActionResult.UnavailableInContext(action, result.message)
                        }
                    }
                }
            }

            ControllerAction.SYSTEM_VOLUME_DOWN -> {
                if (systemVolumeAdjuster != null) {
                    if (systemVolumeAdjuster.invoke(AudioManager.ADJUST_LOWER)) {
                        ControllerActionResult.Executed(action, "System: Volume Lowered")
                    } else {
                        ControllerActionResult.UnavailableInContext(action, "System volume adjustment unavailable")
                    }
                } else {
                    val isForeground = ControllerDiagnosticsHolder.state.value.isForeground
                    val result = systemMediaVolumeController.adjustVolume(AudioManager.ADJUST_LOWER, isForeground)
                    val status = systemMediaVolumeController.currentStatus()
                    ControllerDiagnosticsHolder.recordVolumeResult("DOWN", result, status)
                    when (result) {
                        is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.Success -> {
                            ControllerActionResult.Executed(action, "System: Volume Lowered (${result.before} -> ${result.after})")
                        }
                        is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.Boundary -> {
                            ControllerActionResult.Executed(action, "System: ${result.message}")
                        }
                        is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.FixedVolume -> {
                            ControllerActionResult.UnavailableInContext(action, result.message)
                        }
                        is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.NoChange -> {
                            ControllerActionResult.UnavailableInContext(action, result.message)
                        }
                        is vn.loi.learning.android.media.SystemVolumeAdjustmentResult.Error -> {
                            ControllerActionResult.UnavailableInContext(action, result.message)
                        }
                    }
                }
            }

            ControllerAction.LOCK_SCREEN -> {
                if (systemActionBridge.lockScreen()) {
                    ControllerActionResult.Executed(action, "System: Screen Locked")
                } else {
                    ControllerActionResult.UnavailableInContext(action, "Accessibility service not connected or device lock unavailable")
                }
            }

            ControllerAction.REPLAY_SENTENCE,
            ControllerAction.TOGGLE_LOOP,
            ControllerAction.SPEED_UP,
            ControllerAction.SPEED_DOWN -> {
                ControllerActionResult.DeferredAction(action)
            }
        }
    }

    private suspend fun dispatchRating(action: ControllerAction, rating: ReviewRating): ControllerActionResult {
        return if (studyBridge.rate(rating)) {
            ControllerActionResult.Executed(action, "Study: ${rating.name}")
        } else {
            ControllerActionResult.UnavailableInContext(action, "Current study state cannot legally be rated (${rating.name})")
        }
    }
}
