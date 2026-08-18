package vn.loi.learning.android.recording

import java.io.File

/**
 * Metadata record for a persistent voice recording.
 */
data class VoiceRecordingItem(
    val id: String,
    val filename: String,
    val filePath: String,
    val createdAt: Long,
    val durationMs: Long,
    val sizeBytes: Long
) {
    val file: File
        get() = File(filePath)

    val exists: Boolean
        get() = file.exists() && file.length() > 0
}

/**
 * Unique identifier for a single discrete voice recording session.
 */
data class RecordingSessionId(val value: String) {
    companion object {
        fun generate(timestamp: Long = System.currentTimeMillis()): RecordingSessionId =
            RecordingSessionId("rec_session_${timestamp}_${java.util.UUID.randomUUID().toString().take(8)}")
    }
}

/**
 * State machine for Quick Voice Recorder.
 */
sealed interface QuickVoiceRecorderState {
    data class Idle(
        val latestRecording: VoiceRecordingItem? = null
    ) : QuickVoiceRecorderState

    data class Starting(
        val sessionId: RecordingSessionId,
        val targetFile: File,
        val requestedAtMs: Long
    ) : QuickVoiceRecorderState

    data class Recording(
        val sessionId: RecordingSessionId,
        val startTimeMs: Long,
        val file: File
    ) : QuickVoiceRecorderState

    data class Stopping(
        val sessionId: RecordingSessionId,
        val file: File,
        val startTimeMs: Long
    ) : QuickVoiceRecorderState

    data class Recorded(
        val item: VoiceRecordingItem
    ) : QuickVoiceRecorderState

    data class Replaying(
        val item: VoiceRecordingItem
    ) : QuickVoiceRecorderState

    data class Error(
        val message: String,
        val latestRecording: VoiceRecordingItem? = null
    ) : QuickVoiceRecorderState
}

/**
 * Operational result returned by QuickVoiceRecorderController commands.
 */
sealed interface QuickVoiceOperationResult {
    data class Starting(val sessionId: RecordingSessionId, val file: File) : QuickVoiceOperationResult
    data class Started(val sessionId: RecordingSessionId, val file: File) : QuickVoiceOperationResult
    data class Stopping(val sessionId: RecordingSessionId) : QuickVoiceOperationResult
    data class Stopped(val item: VoiceRecordingItem) : QuickVoiceOperationResult
    data class Replaying(val item: VoiceRecordingItem) : QuickVoiceOperationResult
    data class PermissionRequired(val message: String = "Microphone permission (RECORD_AUDIO) is required") : QuickVoiceOperationResult
    data class BackgroundStartRestricted(val reason: String = "Background microphone start restricted by Android OS") : QuickVoiceOperationResult
    data class Unavailable(val reason: String) : QuickVoiceOperationResult
    data class Failed(val error: String) : QuickVoiceOperationResult
}

/**
 * Fine-grained diagnostic trace event types for Quick Voice Recorder.
 */
enum class QuickVoiceTraceEvent {
    INPUT_RECEIVED,
    GESTURE_CREATED,
    MAPPING_RESOLVED,
    DISPATCH_ACCEPTED,
    RECORD_REQUESTED,
    PERMISSION_CHECKED,
    FGS_START_REQUESTED,
    FGS_STARTED,
    RECORDER_CREATED,
    RECORDER_PREPARED,
    RECORDING_STARTED,
    STOP_REQUESTED,
    RECORDER_STOPPED,
    FILE_FINALIZED,
    REPLAY_STARTED,
    REPLAY_FINISHED,
    FILE_DELETED,
    FGS_STOPPED,
    BACKGROUND_MIC_START_RESTRICTED,
    ERROR
}
