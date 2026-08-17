package vn.loi.learning.android.media

import android.content.Context
import android.media.AudioManager
import android.os.Build

/**
 * Snapshot of current system media volume state.
 */
data class SystemVolumeStatus(
    val currentVolume: Int,
    val minVolume: Int,
    val maxVolume: Int,
    val isVolumeFixed: Boolean
)

/**
 * Truthful result of a system media volume adjustment attempt.
 */
sealed interface SystemVolumeAdjustmentResult {
    data class Success(val before: Int, val after: Int, val max: Int, val min: Int, val direction: Int) : SystemVolumeAdjustmentResult
    data class Boundary(val volume: Int, val isMax: Boolean, val message: String) : SystemVolumeAdjustmentResult
    data class FixedVolume(val message: String = "System media volume is fixed") : SystemVolumeAdjustmentResult
    data class NoChange(val before: Int, val max: Int, val min: Int, val message: String = "Volume did not change") : SystemVolumeAdjustmentResult
    data class Error(val message: String) : SystemVolumeAdjustmentResult
}

/**
 * Controller interface for querying and modifying real Android system media stream volume.
 */
interface SystemMediaVolumeController {
    fun adjustVolume(direction: Int, isForeground: Boolean): SystemVolumeAdjustmentResult
    fun currentStatus(): SystemVolumeStatus
}

/**
 * Android AudioManager-backed implementation of SystemMediaVolumeController.
 */
class AndroidSystemMediaVolumeController(
    private val context: Context,
    private val audioManagerProvider: () -> AudioManager? = {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
) : SystemMediaVolumeController {

    override fun currentStatus(): SystemVolumeStatus {
        val am = audioManagerProvider() ?: return SystemVolumeStatus(0, 0, 15, false)
        val isFixed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            runCatching { am.isVolumeFixed }.getOrDefault(false)
        } else false
        val current = runCatching { am.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        val max = runCatching { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(15)
        val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { am.getStreamMinVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        } else 0
        return SystemVolumeStatus(current, min, max, isFixed)
    }

    override fun adjustVolume(direction: Int, isForeground: Boolean): SystemVolumeAdjustmentResult {
        val am = audioManagerProvider() ?: return SystemVolumeAdjustmentResult.Error("AudioManager unavailable")
        val isFixed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            runCatching { am.isVolumeFixed }.getOrDefault(false)
        } else false

        if (isFixed) {
            return SystemVolumeAdjustmentResult.FixedVolume("System media volume is fixed")
        }

        val before = runCatching { am.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrElse {
            return SystemVolumeAdjustmentResult.Error("Failed to get stream volume: ${it.message}")
        }
        val max = runCatching { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(15)
        val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { am.getStreamMinVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        } else 0

        if (direction == AudioManager.ADJUST_RAISE && before >= max) {
            return SystemVolumeAdjustmentResult.Boundary(before, isMax = true, message = "Already at maximum media volume ($max)")
        }
        if (direction == AudioManager.ADJUST_LOWER && before <= min) {
            return SystemVolumeAdjustmentResult.Boundary(before, isMax = false, message = "Already at minimum media volume ($min)")
        }

        val flags = if (isForeground) AudioManager.FLAG_SHOW_UI else 0

        try {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, flags)
        } catch (e: Exception) {
            return SystemVolumeAdjustmentResult.Error("adjustStreamVolume error: ${e.message}")
        }

        val after = runCatching { am.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(before)

        return if (direction == AudioManager.ADJUST_RAISE) {
            if (after > before) {
                SystemVolumeAdjustmentResult.Success(before, after, max, min, direction)
            } else if (after == max) {
                SystemVolumeAdjustmentResult.Boundary(after, isMax = true, message = "Already at maximum media volume ($max)")
            } else {
                SystemVolumeAdjustmentResult.NoChange(before, max, min, "Volume raise did not change volume (before=$before, after=$after)")
            }
        } else {
            if (after < before) {
                SystemVolumeAdjustmentResult.Success(before, after, max, min, direction)
            } else if (after == min) {
                SystemVolumeAdjustmentResult.Boundary(after, isMax = false, message = "Already at minimum media volume ($min)")
            } else {
                SystemVolumeAdjustmentResult.NoChange(before, max, min, "Volume lower did not change volume (before=$before, after=$after)")
            }
        }
    }
}
