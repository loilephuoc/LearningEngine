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
    fun dispatch(candidate: DesktopVocabularyCandidate)
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

    @Synchronized
    fun start() {
        if (started || closed) return
        started = true
        reschedule()
    }

    @Synchronized
    fun updateSettings(updated: DesktopVocabularyReminderSettings) {
        if (closed) return
        runCatching { settingsRepository.save(updated) }
            .onFailure(onFailure)
            .getOrElse { return }
        settings = updated
        if (started) reschedule()
    }

    @Synchronized
    fun pauseFor30Minutes() = pauseFor(Duration.ofMinutes(30))

    @Synchronized
    fun pauseForOneHour() = pauseFor(Duration.ofHours(1))

    @Synchronized
    fun pauseToday() {
        updateSettings(DesktopVocabularyReminderSchedule.pauseToday(settings, clock.instant(), zoneId()))
    }

    private fun pauseFor(duration: Duration) {
        updateSettings(DesktopVocabularyReminderSchedule.pauseFor(settings, clock.instant(), duration))
    }

    private fun reschedule() {
        scheduledTask?.cancel()
        scheduledTask = null
        if (!closed && settings.enabled) {
            scheduledTask = delayScheduler.schedule(settings.intervalMinutes * MILLIS_PER_MINUTE, ::tick)
        }
    }

    @Synchronized
    private fun tick() {
        scheduledTask = null
        if (closed || !started || !settings.enabled) return
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
        if (!closed && started && settings.enabled) reschedule()
    }

    private fun dispatchIfEligible() {
        if (settings.selectedPackageId == null) return
        if (!DesktopVocabularyReminderSchedule.isActiveAt(settings, clock.instant(), zoneId())) return
        if (sink.isReminderActive) return
        when (val result = selector.select(settings)) {
            is DesktopVocabularyCandidateSelectionResult.Selected -> sink.dispatch(result.candidate)
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
        (delayScheduler as? AutoCloseable)?.runCatching { close() }?.onFailure(onFailure)
    }

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}

object NoOpDesktopVocabularyReminderSink : DesktopVocabularyReminderSink {
    override val isReminderActive = false
    override fun dispatch(candidate: DesktopVocabularyCandidate) = Unit
}
