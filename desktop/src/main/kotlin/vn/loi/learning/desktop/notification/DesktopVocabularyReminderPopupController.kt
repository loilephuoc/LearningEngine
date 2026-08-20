package vn.loi.learning.desktop.notification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.swing.SwingUtilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface DesktopVocabularyReminderPopupState {
    data object Hidden : DesktopVocabularyReminderPopupState

    data class Visible(
        val candidate: DesktopVocabularyCandidate,
        val generation: Long,
        val remainingMillis: Long,
        val hovered: Boolean,
        val audioAvailable: Boolean,
        val audioPlaying: Boolean,
        val autoPlayPronunciation: Boolean,
        val markedDifficult: Boolean,
        val popupLocation: DesktopVocabularyReminderPopupLocation = DesktopVocabularyReminderPopupLocation(),
        val dragging: Boolean = false,
        val popupLayout: DesktopVocabularyReminderPopupLayout = DesktopVocabularyReminderPopupLayout.COMPACT,
        val englishTextFontSizeSp: Float = DesktopVocabularyReminderSettings.DEFAULT_ENGLISH_FONT_SIZE_SP
    ) : DesktopVocabularyReminderPopupState

    data class FullImage(
        val candidate: DesktopVocabularyCandidate,
        val generation: Long
    ) : DesktopVocabularyReminderPopupState
}

fun interface DesktopVocabularyReminderUiDispatcher {
    fun dispatch(action: () -> Unit)
}

object SwingDesktopVocabularyReminderUiDispatcher : DesktopVocabularyReminderUiDispatcher {
    override fun dispatch(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) action() else SwingUtilities.invokeLater(action)
    }
}

fun interface DesktopVocabularyReminderMonotonicClock {
    fun nowMillis(): Long
}

fun interface DesktopVocabularyReminderPopupTimer {
    fun schedule(delayMillis: Long, action: () -> Unit): DesktopVocabularyReminderScheduledTask
}

interface DesktopVocabularyReminderAutoPlayAuthority {
    fun current(): Boolean
    fun update(enabled: Boolean): Boolean
}

fun interface DesktopVocabularyReminderLayoutAuthority {
    fun update(layout: DesktopVocabularyReminderPopupLayout): Boolean
}

fun interface DesktopVocabularyReminderSnoozeAuthority {
    fun snooze(durationMinutes: Int): Boolean
}

class DesktopVocabularyReminderPopupController(
    private val uiDispatcher: DesktopVocabularyReminderUiDispatcher,
    private val clock: DesktopVocabularyReminderMonotonicClock,
    private val timer: DesktopVocabularyReminderPopupTimer,
    private val audio: DesktopVocabularyReminderAudioLifecycle = NoOpDesktopVocabularyReminderAudioLifecycle,
    private val difficultMarkers: DesktopVocabularyReminderDifficultMarkers? = null,
    private val autoPlayAuthority: DesktopVocabularyReminderAutoPlayAuthority? = null,
    private val layoutAuthority: DesktopVocabularyReminderLayoutAuthority? = null,
    private val snoozeAuthority: DesktopVocabularyReminderSnoozeAuthority? = null
) : DesktopVocabularyReminderSink {
    private val active = AtomicBoolean(false)
    private val generation = AtomicLong()
    private val lock = Any()
    private val mutableState = MutableStateFlow<DesktopVocabularyReminderPopupState>(
        DesktopVocabularyReminderPopupState.Hidden
    )
    val state: StateFlow<DesktopVocabularyReminderPopupState> = mutableState.asStateFlow()

    private var closed = false
    private var currentCandidate: DesktopVocabularyCandidate? = null
    private var currentGeneration = 0L
    private var remainingMillis = 0L
    private var deadlineMillis = 0L
    private var hovered = false
    private var dragging = false
    private var audioPlaying = false
    private var autoPlayPronunciation = false
    private var markedDifficult = false
    private var currentPopupLocation = DesktopVocabularyReminderPopupLocation()
    private var currentPopupLayout = DesktopVocabularyReminderPopupLayout.COMPACT
    private var playVietnameseAudio = false
    private var vietnameseAudioDelayMillis = DesktopVocabularyReminderSettings.DEFAULT_VIETNAMESE_AUDIO_DELAY_MILLIS
    private var englishTextFontSizeSp = DesktopVocabularyReminderSettings.DEFAULT_ENGLISH_FONT_SIZE_SP
    private var popupMuted = false
    private var displayDeadlineExpired = false
    private var pendingVietnamese = false
    private var hideTask: DesktopVocabularyReminderScheduledTask? = null
    private var vietnameseTask: DesktopVocabularyReminderScheduledTask? = null
    private val audioRegistration = audio.listen { playing ->
        uiDispatcher.dispatch {
            synchronized(lock) {
                if (
                    active.get() &&
                    mutableState.value is DesktopVocabularyReminderPopupState.Visible &&
                    audioPlaying != playing
                ) {
                    audioPlaying = playing
                    publishVisible()
                    if (!playing && !pendingVietnamese && displayDeadlineExpired && !hovered && !dragging) {
                        hide(currentGeneration)
                    }
                }
            }
        }
    }

    override val isReminderActive: Boolean
        get() = active.get()

    override fun dispatch(
        candidate: DesktopVocabularyCandidate,
        displayDurationMillis: Long,
        autoPlayPronunciation: Boolean,
        popupLocation: DesktopVocabularyReminderPopupLocation,
        popupLayout: DesktopVocabularyReminderPopupLayout,
        playVietnameseAudio: Boolean,
        vietnameseAudioDelayMillis: Long,
        englishTextFontSizeSp: Float
    ) {
        require(displayDurationMillis in DesktopVocabularyReminderSettings.MIN_DISPLAY_DURATION_MILLIS..
            DesktopVocabularyReminderSettings.MAX_DISPLAY_DURATION_MILLIS)
        if (!active.compareAndSet(false, true)) return
        val token = generation.incrementAndGet()
        uiDispatcher.dispatch {
            synchronized(lock) {
                if (closed || generation.get() != token) {
                    active.set(false)
                    return@synchronized
                }
                vietnameseTask?.cancel()
                vietnameseTask = null
                pendingVietnamese = false
                displayDeadlineExpired = false
                currentCandidate = candidate
                currentGeneration = token
                remainingMillis = displayDurationMillis
                hovered = false
                dragging = false
                popupMuted = false
                currentPopupLocation = popupLocation
                currentPopupLayout = popupLayout
                this@DesktopVocabularyReminderPopupController.playVietnameseAudio = playVietnameseAudio
                this@DesktopVocabularyReminderPopupController.vietnameseAudioDelayMillis = vietnameseAudioDelayMillis
                this@DesktopVocabularyReminderPopupController.englishTextFontSizeSp = englishTextFontSizeSp
                this@DesktopVocabularyReminderPopupController.autoPlayPronunciation = autoPlayPronunciation
                audioPlaying = this@DesktopVocabularyReminderPopupController.autoPlayPronunciation &&
                    candidate.primaryAudioReference != null
                markedDifficult = difficultMarkers?.isMarked(candidate.contentId) == true
                publishVisible()

                fun scheduleVietnamese(targetToken: Long) {
                    if (!this@DesktopVocabularyReminderPopupController.playVietnameseAudio ||
                        candidate.translatedAudioReference == null ||
                        popupMuted
                    ) {
                        pendingVietnamese = false
                        if (displayDeadlineExpired && !audioPlaying && !hovered && !dragging) {
                            hide(targetToken)
                        }
                        return
                    }
                    pendingVietnamese = true
                    vietnameseTask?.cancel()
                    vietnameseTask = timer.schedule(this@DesktopVocabularyReminderPopupController.vietnameseAudioDelayMillis) {
                        uiDispatcher.dispatch {
                            synchronized(lock) {
                                if (!active.get() || currentGeneration != targetToken ||
                                    popupMuted || closed ||
                                    mutableState.value is DesktopVocabularyReminderPopupState.FullImage) {
                                    pendingVietnamese = false
                                    if (displayDeadlineExpired && !audioPlaying && !hovered && !dragging) {
                                        hide(targetToken)
                                    }
                                    return@synchronized
                                }
                                val ref = candidate.translatedAudioReference
                                if (ref != null) {
                                    audio.runCatching {
                                        start(ref) {
                                            uiDispatcher.dispatch {
                                                synchronized(lock) {
                                                    pendingVietnamese = false
                                                    audioPlaying = false
                                                    if (active.get() && currentGeneration == targetToken && !closed) {
                                                        if (displayDeadlineExpired && !hovered && !dragging) {
                                                            hide(targetToken)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }.onFailure {
                                        pendingVietnamese = false
                                        audioPlaying = false
                                        if (displayDeadlineExpired && !hovered && !dragging) {
                                            hide(targetToken)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (this@DesktopVocabularyReminderPopupController.autoPlayPronunciation &&
                    candidate.primaryAudioReference != null) {
                    val reference = candidate.primaryAudioReference
                    audio.runCatching {
                        start(reference) {
                            uiDispatcher.dispatch {
                                synchronized(lock) {
                                    audioPlaying = false
                                    if (active.get() && currentGeneration == token && !closed) {
                                        scheduleVietnamese(token)
                                    }
                                }
                            }
                        }
                    }.onFailure {
                        audioPlaying = false
                        publishVisible()
                        if (this@DesktopVocabularyReminderPopupController.playVietnameseAudio &&
                            candidate.translatedAudioReference != null) {
                            scheduleVietnamese(token)
                        } else {
                            if (displayDeadlineExpired && !hovered && !dragging) {
                                hide(token)
                            }
                        }
                    }
                } else if (this@DesktopVocabularyReminderPopupController.playVietnameseAudio &&
                    candidate.translatedAudioReference != null) {
                    scheduleVietnamese(token)
                }

                scheduleHide(token, remainingMillis)
            }
        }
    }

    fun dragStarted() = uiDispatcher.dispatch {
        synchronized(lock) {
            if (!active.get() || dragging) return@synchronized
            dragging = true
            remainingMillis = (deadlineMillis - clock.nowMillis()).coerceAtLeast(0L)
            hideTask?.cancel()
            hideTask = null
            publishVisible()
        }
    }

    fun dragEnded() = uiDispatcher.dispatch {
        synchronized(lock) {
            if (!active.get() || !dragging) return@synchronized
            dragging = false
            publishVisible()
            if (!hovered) {
                if (remainingMillis <= 0L) {
                    displayDeadlineExpired = true
                    if (!isAudioActiveOrPending()) hide(currentGeneration)
                } else {
                    displayDeadlineExpired = false
                    scheduleHide(currentGeneration, remainingMillis)
                }
            }
        }
    }

    fun pointerEntered() = uiDispatcher.dispatch {
        synchronized(lock) {
            if (!active.get() || hovered) return@synchronized
            if (!dragging) {
                remainingMillis = (deadlineMillis - clock.nowMillis()).coerceAtLeast(0L)
                hideTask?.cancel()
                hideTask = null
            }
            hovered = true
            publishVisible()
        }
    }

    fun pointerExited() = uiDispatcher.dispatch {
        synchronized(lock) {
            if (!active.get() || !hovered) return@synchronized
            hovered = false
            publishVisible()
            if (!dragging) {
                if (remainingMillis <= 0L) {
                    displayDeadlineExpired = true
                    if (!isAudioActiveOrPending()) hide(currentGeneration)
                } else {
                    displayDeadlineExpired = false
                    scheduleHide(currentGeneration, remainingMillis)
                }
            }
        }
    }

    fun closePopup() = invalidate()

    fun toggleAudio() = uiDispatcher.dispatch {
        synchronized(lock) {
            val candidate = currentCandidate ?: return@synchronized
            val enable = !autoPlayPronunciation
            if (!enable) {
                popupMuted = true
                vietnameseTask?.cancel()
                vietnameseTask = null
                pendingVietnamese = false
                audio.runCatching { stop() }
                audioPlaying = false
                val saved = autoPlayAuthority?.update(false) ?: true
                if (!saved) {
                    publishVisible()
                    return@synchronized
                }
                autoPlayPronunciation = false
                publishVisible()
                if (displayDeadlineExpired && !hovered && !dragging) {
                    hide(currentGeneration)
                }
                return@synchronized
            } else {
                popupMuted = false
                val saved = autoPlayAuthority?.update(true) ?: true
                if (!saved) {
                    publishVisible()
                    return@synchronized
                }
                autoPlayPronunciation = true
                candidate.primaryAudioReference?.let { reference ->
                    audio.runCatching { start(reference) }.onSuccess { audioPlaying = true }
                }
                publishVisible()
            }
        }
    }

    fun toggleLayout() = uiDispatcher.dispatch {
        synchronized(lock) {
            val nextLayout = if (currentPopupLayout == DesktopVocabularyReminderPopupLayout.COMPACT) {
                DesktopVocabularyReminderPopupLayout.LARGE_IMAGE_VERTICAL
            } else {
                DesktopVocabularyReminderPopupLayout.COMPACT
            }
            currentPopupLayout = nextLayout
            layoutAuthority?.update(nextLayout)
            if (active.get()) publishVisible()
        }
    }

    fun snooze(durationMinutes: Int = 5) = uiDispatcher.dispatch {
        synchronized(lock) {
            snoozeAuthority?.snooze(durationMinutes)
            invalidate()
        }
    }

    override fun settingsUpdated(settings: DesktopVocabularyReminderSettings) = uiDispatcher.dispatch {
        synchronized(lock) {
            autoPlayPronunciation = settings.autoPlayPronunciation
            currentPopupLocation = settings.popupLocation
            currentPopupLayout = settings.popupLayout
            playVietnameseAudio = settings.playVietnameseAudio
            vietnameseAudioDelayMillis = settings.vietnameseAudioDelayMillis
            englishTextFontSizeSp = settings.englishTextFontSizeSp
            if (active.get()) publishVisible()
        }
    }

    fun toggleDifficultMarker() = uiDispatcher.dispatch {
        synchronized(lock) {
            val candidate = currentCandidate ?: return@synchronized
            markedDifficult = difficultMarkers?.runCatching { toggle(candidate.contentId) }?.getOrNull() ?: return@synchronized
            publishVisible()
        }
    }

    fun openFullImage() = uiDispatcher.dispatch {
        synchronized(lock) {
            val candidate = currentCandidate ?: return@synchronized
            if (candidate.imageReference == null || mutableState.value is DesktopVocabularyReminderPopupState.FullImage) {
                return@synchronized
            }
            hideTask?.cancel()
            hideTask = null
            vietnameseTask?.cancel()
            vietnameseTask = null
            pendingVietnamese = false
            displayDeadlineExpired = false
            audio.runCatching { stop() }
            audioPlaying = false
            mutableState.value = DesktopVocabularyReminderPopupState.FullImage(candidate, currentGeneration)
            candidate.primaryAudioReference?.let { audio.runCatching { startLoop(it) } }
        }
    }

    fun closeFullImage() = invalidate()

    override fun invalidate() {
        val token = generation.incrementAndGet()
        active.set(false)
        uiDispatcher.dispatch {
            synchronized(lock) { hide(token, force = true) }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
        }
        invalidate()
        (timer as? AutoCloseable)?.runCatching { close() }
        audioRegistration.runCatching { close() }
        audio.runCatching { close() }
    }

    private fun isAudioActiveOrPending(): Boolean =
        audioPlaying || pendingVietnamese

    private fun scheduleHide(token: Long, delayMillis: Long) {
        hideTask?.cancel()
        deadlineMillis = clock.nowMillis() + delayMillis
        hideTask = timer.schedule(delayMillis) {
            uiDispatcher.dispatch {
                synchronized(lock) {
                    if (currentGeneration != token || !active.get()) return@synchronized
                    displayDeadlineExpired = true
                    if (!isAudioActiveOrPending() && !hovered && !dragging) {
                        hide(token)
                    }
                }
            }
        }
    }

    private fun hide(token: Long, force: Boolean = false) {
        if (generation.get() != token && !force) return
        if (!force && mutableState.value is DesktopVocabularyReminderPopupState.FullImage) return
        if (!force && (token != currentGeneration || hovered || dragging)) return
        hideTask?.cancel()
        hideTask = null
        vietnameseTask?.cancel()
        vietnameseTask = null
        pendingVietnamese = false
        displayDeadlineExpired = false
        audio.runCatching { stop() }
        currentCandidate = null
        remainingMillis = 0L
        hovered = false
        dragging = false
        audioPlaying = false
        active.set(false)
        mutableState.value = DesktopVocabularyReminderPopupState.Hidden
    }

    private fun publishVisible() {
        if (mutableState.value is DesktopVocabularyReminderPopupState.FullImage) return
        val candidate = currentCandidate ?: return
        mutableState.value = DesktopVocabularyReminderPopupState.Visible(
            candidate = candidate,
            generation = currentGeneration,
            remainingMillis = remainingMillis,
            hovered = hovered,
            audioAvailable = candidate.primaryAudioReference != null,
            audioPlaying = audioPlaying,
            autoPlayPronunciation = autoPlayPronunciation,
            markedDifficult = markedDifficult,
            popupLocation = currentPopupLocation,
            dragging = dragging,
            popupLayout = currentPopupLayout,
            englishTextFontSizeSp = englishTextFontSizeSp
        )
    }
}
