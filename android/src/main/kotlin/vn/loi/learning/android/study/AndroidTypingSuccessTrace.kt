package vn.loi.learning.android.study

import android.os.SystemClock
import android.util.Log
import vn.loi.learning.android.BuildConfig

internal object AndroidTypingSuccessTrace {
    private const val TAG = "TypingSuccessTrace"
    private data class Transaction(
        val planId: String,
        val startedAtMillis: Long,
        var audioCompleted: Boolean = false,
        var dwellCompleted: Boolean = false
    )

    @Volatile private var transaction: Transaction? = null

    fun exactMatch(planId: String, audioPath: String?) {
        if (!BuildConfig.DEBUG) return
        val startedAt = monotonicMillis()
        transaction = Transaction(planId, startedAt)
        write("exactMatch", planId, startedAt, false, false, null, "audio=${audioPath.orEmpty()}")
    }

    fun event(
        name: String,
        planId: String,
        audioCompleted: Boolean,
        dwellCompleted: Boolean,
        activeAudioRole: AudioRole? = null,
        detail: String = ""
    ) {
        if (!BuildConfig.DEBUG) return
        val active = transaction ?: return
        if (audioCompleted) active.audioCompleted = true
        if (dwellCompleted) active.dwellCompleted = true
        write(name, planId, active.startedAtMillis, active.audioCompleted, active.dwellCompleted, activeAudioRole, detail)
        if (name == "nextVisible") transaction = null
    }

    fun activePlanId(): String? = if (BuildConfig.DEBUG) transaction?.planId else null

    fun commitEvent(name: String, planId: String, detail: String = "") =
        phaseEvent("TYPING_COMMIT", name, planId, detail)

    fun nextEvent(name: String, planId: String, detail: String = "") =
        phaseEvent("TYPING_NEXT", name, planId, detail)

    private fun phaseEvent(prefix: String, name: String, planId: String, detail: String) {
        if (!BuildConfig.DEBUG) return
        val active = transaction ?: return
        val elapsed = (monotonicMillis() - active.startedAtMillis).coerceAtLeast(0L)
        runCatching {
            Log.d(
                TAG,
                "$prefix $name plan=$planId elapsedMs=$elapsed" +
                    detail.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
            )
        }
    }

    private fun write(
        name: String,
        planId: String,
        startedAtMillis: Long,
        audioCompleted: Boolean,
        dwellCompleted: Boolean,
        activeAudioRole: AudioRole?,
        detail: String
    ) {
        val elapsed = (monotonicMillis() - startedAtMillis).coerceAtLeast(0L)
        runCatching {
            Log.d(
                TAG,
                "TYPING_SUCCESS $name plan=$planId elapsedMs=$elapsed " +
                    "pendingAudio=${!audioCompleted} pendingDwell=${!dwellCompleted} " +
                    "activeAudioRole=${activeAudioRole?.name ?: "NONE"}${detail.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()}"
            )
        }
    }

    private fun monotonicMillis(): Long =
        runCatching { SystemClock.elapsedRealtime() }.getOrElse { System.nanoTime() / 1_000_000L }
}
