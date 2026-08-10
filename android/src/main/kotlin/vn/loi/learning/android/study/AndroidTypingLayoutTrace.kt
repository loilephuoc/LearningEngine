package vn.loi.learning.android.study

import android.os.SystemClock
import android.util.Log
import vn.loi.learning.android.BuildConfig
import vn.loi.learning.android.study.design.StudyContentDensity
import vn.loi.learning.android.study.design.StudyMediaRole

internal object AndroidTypingLayoutTrace {
    private const val TAG = "TypingLayoutTrace"
    private const val TRACE_WINDOW_MILLIS = 500L

    private data class PlanTrace(
        val startedAtMillis: Long,
        var lastSignature: String? = null
    )

    private val plans = mutableMapOf<String, PlanTrace>()

    @Synchronized
    fun changed(
        planId: String,
        imeVisible: Boolean,
        inputFocused: Boolean?,
        density: StudyContentDensity,
        mediaRole: StudyMediaRole,
        availableMediaHeightDp: Int
    ) {
        if (!BuildConfig.DEBUG) return
        val now = monotonicMillis()
        val trace = plans.getOrPut(planId) { PlanTrace(now) }
        val elapsed = (now - trace.startedAtMillis).coerceAtLeast(0L)
        if (elapsed > TRACE_WINDOW_MILLIS) return
        val signature = "$imeVisible|$inputFocused|$density|$mediaRole|$availableMediaHeightDp"
        if (trace.lastSignature == signature) return
        trace.lastSignature = signature
        runCatching {
            Log.d(
                TAG,
                "TYPING_LAYOUT plan=$planId event=presentationChanged elapsedMs=$elapsed " +
                    "imeVisible=$imeVisible inputFocused=${inputFocused?.toString() ?: "UNKNOWN"} " +
                    "density=${density.name} mediaRole=${mediaRole.name} " +
                    "availableMediaHeightDp=$availableMediaHeightDp"
            )
        }
    }

    private fun monotonicMillis(): Long =
        runCatching { SystemClock.elapsedRealtime() }.getOrElse { System.nanoTime() / 1_000_000L }
}
