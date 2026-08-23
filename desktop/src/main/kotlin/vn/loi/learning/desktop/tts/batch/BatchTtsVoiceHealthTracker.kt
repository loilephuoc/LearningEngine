package vn.loi.learning.desktop.tts.batch

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Health lifecycle state of a TTS voice candidate during a batch execution.
 */
enum class VoiceHealthState {
    HEALTHY,
    DEGRADED,
    CIRCUIT_OPEN,
    HALF_OPEN_PROBE
}

/**
 * Snapshot of health status for a specific voice candidate.
 */
data class VoiceHealthStatus(
    val voiceId: String,
    val state: VoiceHealthState,
    val consecutiveHardTimeouts: Int,
    val consecutiveFastFailures: Int,
    val lastFailureTimestamp: Long = 0L,
    val lastSuccessTimestamp: Long = 0L,
    val circuitOpenUntilTimestamp: Long = 0L
)

/**
 * Batch-scoped, thread-safe, language-aware health tracker and circuit breaker for TTS voice candidates.
 *
 * Prevents large-batch stalls when a primary voice provider is unhealthy or unresponsive
 * by deprioritizing broken candidates and routing immediately to healthy fallbacks.
 */
class BatchTtsVoiceHealthTracker(
    private val circuitOpenCooldownMillis: Long = 30_000L,
    private val maxConsecutiveHardTimeoutsBeforeOpen: Int = 1,
    private val maxConsecutiveFastFailuresBeforeOpen: Int = 3,
    private val eventLogger: BatchTtsEventLogger = BatchTtsEventLogger.NoOp
) {
    private val statuses = ConcurrentHashMap<String, VoiceHealthStatus>()
    private val probeInFlight = ConcurrentHashMap<String, Boolean>()

    /**
     * Returns the current health status of a voice, evaluating cooldown transitions if expired.
     */
    fun getStatus(voiceId: String): VoiceHealthStatus {
        val now = System.currentTimeMillis()
        val current = statuses.computeIfAbsent(voiceId) {
            VoiceHealthStatus(voiceId, VoiceHealthState.HEALTHY, 0, 0)
        }
        if (current.state == VoiceHealthState.CIRCUIT_OPEN && now >= current.circuitOpenUntilTimestamp) {
            val probed = current.copy(state = VoiceHealthState.HALF_OPEN_PROBE)
            statuses[voiceId] = probed
            eventLogger.logVoiceHealthChanged(
                voiceId = voiceId,
                oldState = VoiceHealthState.CIRCUIT_OPEN.name,
                newState = VoiceHealthState.HALF_OPEN_PROBE.name,
                reason = "Cooldown elapsed; ready for probe"
            )
            return probed
        }
        return current
    }

    /**
     * Determines whether a voice candidate should be attempted for a target.
     *
     * If the circuit is OPEN and other eligible candidates exist, the unhealthy voice is bypassed.
     * If this is the only available candidate, it is attempted as a last resort.
     */
    fun isCandidateEligible(voiceId: String, hasAlternativeCandidates: Boolean): Boolean {
        val status = getStatus(voiceId)
        return when (status.state) {
            VoiceHealthState.HEALTHY, VoiceHealthState.DEGRADED -> true
            VoiceHealthState.HALF_OPEN_PROBE -> {
                // Allow one probe attempt at a time
                probeInFlight.putIfAbsent(voiceId, true) == null || !hasAlternativeCandidates
            }
            VoiceHealthState.CIRCUIT_OPEN -> !hasAlternativeCandidates
        }
    }

    /**
     * Records a successful synthesis with this voice, restoring healthy state.
     */
    fun recordSuccess(voiceId: String) {
        probeInFlight.remove(voiceId)
        val prev = statuses[voiceId] ?: VoiceHealthStatus(voiceId, VoiceHealthState.HEALTHY, 0, 0)
        val now = System.currentTimeMillis()
        statuses[voiceId] = VoiceHealthStatus(
            voiceId = voiceId,
            state = VoiceHealthState.HEALTHY,
            consecutiveHardTimeouts = 0,
            consecutiveFastFailures = 0,
            lastFailureTimestamp = prev.lastFailureTimestamp,
            lastSuccessTimestamp = now,
            circuitOpenUntilTimestamp = 0L
        )
        if (prev.state != VoiceHealthState.HEALTHY) {
            eventLogger.logVoiceHealthChanged(
                voiceId = voiceId,
                oldState = prev.state.name,
                newState = VoiceHealthState.HEALTHY.name,
                reason = "Synthesis succeeded"
            )
        }
    }

    /**
     * Records a hard timeout / hang on this voice.
     * Hard timeouts carry heavy negative weight and immediately open circuit after [maxConsecutiveHardTimeoutsBeforeOpen].
     */
    fun recordHardTimeout(voiceId: String) {
        probeInFlight.remove(voiceId)
        val prev = statuses[voiceId] ?: VoiceHealthStatus(voiceId, VoiceHealthState.HEALTHY, 0, 0)
        val now = System.currentTimeMillis()
        val newTimeouts = prev.consecutiveHardTimeouts + 1
        val shouldOpen = newTimeouts >= maxConsecutiveHardTimeoutsBeforeOpen
        val newState = if (shouldOpen) VoiceHealthState.CIRCUIT_OPEN else VoiceHealthState.DEGRADED
        val openUntil = if (shouldOpen) now + circuitOpenCooldownMillis else 0L

        statuses[voiceId] = prev.copy(
            state = newState,
            consecutiveHardTimeouts = newTimeouts,
            lastFailureTimestamp = now,
            circuitOpenUntilTimestamp = openUntil
        )
        if (shouldOpen) {
            eventLogger.logVoiceCircuitOpen(voiceId, newTimeouts, circuitOpenCooldownMillis)
            eventLogger.logVoiceHealthChanged(
                voiceId = voiceId,
                oldState = prev.state.name,
                newState = VoiceHealthState.CIRCUIT_OPEN.name,
                reason = "Hard timeout threshold reached ($newTimeouts timeouts)"
            )
        } else {
            eventLogger.logVoiceHealthChanged(
                voiceId = voiceId,
                oldState = prev.state.name,
                newState = VoiceHealthState.DEGRADED.name,
                reason = "Hard timeout count: $newTimeouts"
            )
        }
    }

    /**
     * Records a fast non-timeout failure (e.g. HTTP 503, invalid voice).
     */
    fun recordFastFailure(voiceId: String) {
        probeInFlight.remove(voiceId)
        val prev = statuses[voiceId] ?: VoiceHealthStatus(voiceId, VoiceHealthState.HEALTHY, 0, 0)
        val now = System.currentTimeMillis()
        val newFailures = prev.consecutiveFastFailures + 1
        val shouldOpen = newFailures >= maxConsecutiveFastFailuresBeforeOpen
        val newState = if (shouldOpen) VoiceHealthState.CIRCUIT_OPEN else VoiceHealthState.DEGRADED
        val openUntil = if (shouldOpen) now + circuitOpenCooldownMillis else 0L

        statuses[voiceId] = prev.copy(
            state = newState,
            consecutiveFastFailures = newFailures,
            lastFailureTimestamp = now,
            circuitOpenUntilTimestamp = openUntil
        )
        if (shouldOpen) {
            eventLogger.logVoiceCircuitOpen(voiceId, newFailures, circuitOpenCooldownMillis)
            eventLogger.logVoiceHealthChanged(
                voiceId = voiceId,
                oldState = prev.state.name,
                newState = VoiceHealthState.CIRCUIT_OPEN.name,
                reason = "Fast failure threshold reached ($newFailures failures)"
            )
        }
    }

    /**
     * Reorders a candidate voice list so that healthy voices are prioritized before degraded/open candidates.
     */
    fun prioritizeCandidates(candidates: List<vn.loi.learning.desktop.tts.TtsVoice>): List<vn.loi.learning.desktop.tts.TtsVoice> {
        if (candidates.size <= 1) return candidates
        return candidates.sortedBy { voice ->
            when (getStatus(voice.id).state) {
                VoiceHealthState.HEALTHY -> 0
                VoiceHealthState.DEGRADED -> 1
                VoiceHealthState.HALF_OPEN_PROBE -> 2
                VoiceHealthState.CIRCUIT_OPEN -> 3
            }
        }
    }
}
