package vn.loi.learning.desktop.notification

import java.time.Clock
import java.time.Duration
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun interface DesktopVocabularyReminderSelectionSource {
    fun select(settings: DesktopVocabularyReminderSettings): DesktopVocabularyCandidateSelectionResult
}

interface DesktopVocabularyReminderSink {
    val isReminderActive: Boolean
    fun dispatch(candidate: DesktopVocabularyCandidate, displayDurationMillis: Long, autoPlayPronunciation: Boolean)

    fun invalidate() = Unit

    fun settingsUpdated(settings: DesktopVocabularyReminderSettings) = Unit

    fun close() = Unit
}

fun interface DesktopVocabularyReminderScheduledTask {
    fun cancel()
}

fun interface DesktopVocabularyReminderDelayScheduler {
    fun schedule(delayMillis: Long, action: () -> Unit): DesktopVocabularyReminderScheduledTask
}

class CoroutineDesktopVocabularyReminderDelayScheduler :
    DesktopVocabularyReminderDelayScheduler,
    AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun schedule(
        delayMillis: Long,
        action: () -> Unit
    ): DesktopVocabularyReminderScheduledTask {
        val job = scope.launch {
            delay(delayMillis)
            action()
        }
        return DesktopVocabularyReminderScheduledTask(job::cancel)
    }

    override fun close() = scope.cancel()
}

class DesktopVocabularyReminderRuntime(
    private val settingsRepository: DesktopVocabularyReminderSettingsRepository,
    private val selector: DesktopVocabularyReminderSelectionSource,
    private val sink: DesktopVocabularyReminderSink,
    private val delayScheduler: DesktopVocabularyReminderDelayScheduler,
    private val clock: Clock,
    private val zoneId: () -> ZoneId,
    private val onFailure: (Throwable) -> Unit = {}
) : AutoCloseable {
    @Volatile
    var settings: DesktopVocabularyReminderSettings = safeLoad()
        private set
    private var scheduledTask: DesktopVocabularyReminderScheduledTask? = null
    private var started = false
    private var closed = false
    private var tickInFlight = false
    private var backgroundMode = true

    @Synchronized
    fun start() {
        if (started || closed) return
        started = true
        reschedule()
    }

    @Synchronized
    fun setBackgroundMode(background: Boolean) {
        if (closed || backgroundMode == background) return
        backgroundMode = background
        scheduledTask?.cancel()
        scheduledTask = null
        if (!background) sink.invalidate() else if (started) reschedule()
    }

    @Synchronized
    fun updateSettings(updated: DesktopVocabularyReminderSettings): Boolean {
        if (closed) return false
        val previous = settings
        runCatching { settingsRepository.save(updated) }
            .onFailure(onFailure)
            .getOrElse { return false }
        settings = updated
        sink.settingsUpdated(updated)
        if (
            !updated.enabled ||
            updated.selectedPackageId != previous.selectedPackageId ||
            updated.selectionMode != previous.selectionMode
        ) {
            sink.invalidate()
        }
        if (started) reschedule()
        return true
    }

    @Synchronized
    fun pauseFor30Minutes() = pauseFor(Duration.ofMinutes(30))

    @Synchronized
    fun pauseForOneHour() = pauseFor(Duration.ofHours(1))

    @Synchronized
    fun pauseToday() =
        updateSettings(DesktopVocabularyReminderSchedule.pauseToday(settings, clock.instant(), zoneId()))

    @Synchronized
    fun resumeNow() = updateSettings(settings.copy(pausedUntil = null))

    private fun pauseFor(duration: Duration) =
        updateSettings(DesktopVocabularyReminderSchedule.pauseFor(settings, clock.instant(), duration))

    private fun reschedule() {
        scheduledTask?.cancel()
        scheduledTask = null
        if (!closed && backgroundMode && settings.enabled) {
            scheduledTask = delayScheduler.schedule(settings.intervalMinutes * MILLIS_PER_MINUTE, ::tick)
        }
    }

    @Synchronized
    private fun tick() {
        scheduledTask = null
        if (closed || !started || !backgroundMode || !settings.enabled) return
        if (!tickInFlight) {
            tickInFlight = true
            try {
                dispatchIfEligible()
            } catch (failure: Throwable) {
                onFailure(failure)
            } finally {
                tickInFlight = false
            }
        }
        if (!closed && started && backgroundMode && settings.enabled) reschedule()
    }

    private fun dispatchIfEligible() {
        if (settings.selectedPackageId == null) return
        if (!DesktopVocabularyReminderSchedule.isActiveAt(settings, clock.instant(), zoneId())) return
        if (sink.isReminderActive) return
        when (val result = selector.select(settings)) {
            is DesktopVocabularyCandidateSelectionResult.Selected ->
                sink.dispatch(result.candidate, settings.displayDurationMillis, settings.autoPlayPronunciation)
            is DesktopVocabularyCandidateSelectionResult.NoCandidate -> Unit
        }
    }

    private fun safeLoad(): DesktopVocabularyReminderSettings =
        runCatching(settingsRepository::load).onFailure(onFailure).getOrDefault(DesktopVocabularyReminderSettings())

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        scheduledTask?.cancel()
        scheduledTask = null
        sink.close()
        (delayScheduler as? AutoCloseable)?.runCatching { close() }?.onFailure(onFailure)
    }

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}

object NoOpDesktopVocabularyReminderSink : DesktopVocabularyReminderSink {
    override val isReminderActive = false
    override fun dispatch(candidate: DesktopVocabularyCandidate, displayDurationMillis: Long, autoPlayPronunciation: Boolean) = Unit
}
