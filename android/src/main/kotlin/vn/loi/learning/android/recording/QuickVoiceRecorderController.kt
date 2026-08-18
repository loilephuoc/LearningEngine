package vn.loi.learning.android.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vn.loi.learning.android.controller.ControllerDiagnosticsHolder
import vn.loi.learning.android.controller.StudyAudioReason
import vn.loi.learning.android.controller.StudyControllerBridge
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioPlaybackEvent
import vn.loi.learning.android.media.AndroidAudioState
import java.io.File

/**
 * Single application-wide authority for Quick Voice Recording state machine,
 * permission routing, playback, and repository coordination.
 *
 * MediaRecorder hardware lifecycle and file finalization are exclusively owned by
 * QuickVoiceRecordingService.
 */
object QuickVoiceRecorderController {

    private const val TAG = "QuickVoiceRecorder"
    private val operationMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val mutableState = MutableStateFlow<QuickVoiceRecorderState>(QuickVoiceRecorderState.Idle())
    val state: StateFlow<QuickVoiceRecorderState> = mutableState.asStateFlow()

    private var repository: QuickVoiceRecordingRepository? = null
    private var activeAudioPlayer: AndroidAudioController? = null

    // Track completed session IDs to guarantee exactly-once persistence
    private val finalizedSessionIds = mutableSetOf<String>()

    // Injectable service starter for unit test environments
    internal var startServiceDirectly: (Context, RecordingSessionId, File, Long) -> Unit = { ctx, sessionId, file, startTime ->
        QuickVoiceRecordingService.start(ctx, sessionId, file, startTime)
    }

    internal var stopServiceDirectly: (Context, RecordingSessionId?) -> Unit = { ctx, sessionId ->
        QuickVoiceRecordingService.stop(ctx, sessionId)
    }

    internal var audioControllerProvider: () -> AndroidAudioController = {
        AndroidAudioController()
    }

    fun initialize(context: Context) {
        val repo = QuickVoiceRecordingRepository.getInstance(context)
        repository = repo
        scope.launch(Dispatchers.IO) {
            repo.initialize()
            val latest = repo.getLatest()
            if (mutableState.value is QuickVoiceRecorderState.Idle) {
                mutableState.value = QuickVoiceRecorderState.Idle(latest)
            }
            updateDiagnostics()
        }
    }

    fun isPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionResult(isGranted: Boolean) {
        recordTrace(
            QuickVoiceTraceEvent.PERMISSION_CHECKED,
            "Runtime permission result: ${if (isGranted) "GRANTED. Press Record again to start." else "DENIED"}"
        )
        updateDiagnostics()
    }

    suspend fun toggleRecording(context: Context): QuickVoiceOperationResult {
        return operationMutex.withLock {
            when (mutableState.value) {
                is QuickVoiceRecorderState.Recording,
                is QuickVoiceRecorderState.Starting -> stopRecordingInternal(context)
                is QuickVoiceRecorderState.Stopping -> QuickVoiceOperationResult.Unavailable("Recording stop is already in progress")
                else -> startRecordingInternal(context)
            }
        }
    }

    suspend fun startRecording(context: Context): QuickVoiceOperationResult {
        return operationMutex.withLock {
            startRecordingInternal(context)
        }
    }

    suspend fun stopRecording(context: Context? = null): QuickVoiceOperationResult {
        return operationMutex.withLock {
            stopRecordingInternal(context)
        }
    }

    private fun startRecordingInternal(context: Context): QuickVoiceOperationResult {
        recordTrace(QuickVoiceTraceEvent.RECORD_REQUESTED, "Start voice recording requested")

        // Reject starting if already active or stopping
        when (mutableState.value) {
            is QuickVoiceRecorderState.Recording,
            is QuickVoiceRecorderState.Starting,
            is QuickVoiceRecorderState.Stopping -> {
                return QuickVoiceOperationResult.Unavailable("Recording is already active or stopping")
            }
            else -> {}
        }

        // 1. Permission Gate
        if (!isPermissionGranted(context)) {
            val isForeground = ControllerDiagnosticsHolder.state.value.isForeground
            return if (isForeground && QuickVoicePermissionBridge.isForegroundLauncherAttached) {
                QuickVoicePermissionBridge.requestPermission()
                recordTrace(QuickVoiceTraceEvent.PERMISSION_CHECKED, "Permission required - requested from foreground Activity")
                QuickVoiceOperationResult.PermissionRequired("Microphone permission (RECORD_AUDIO) is required. Permission dialog shown. Please grant and press Record again.")
            } else {
                recordTrace(QuickVoiceTraceEvent.PERMISSION_CHECKED, "Permission required - background, cannot show dialog")
                QuickVoiceOperationResult.PermissionRequired("Microphone permission (RECORD_AUDIO) is required. Please grant permission in Settings and press Record again.")
            }
        }

        // 2. Stop any existing Study/AutoPlay audio and Replay to avoid recording interference
        StudyControllerBridge.stopAudio(StudyAudioReason.MANUAL_PLAY)
        stopPlaybackInternal()

        // 3. Prepare unique session ID and target file in app-private storage (filesDir)
        val startTimeMs = System.currentTimeMillis()
        val sessionId = RecordingSessionId.generate(startTimeMs)
        val dir = File(context.filesDir, "recordings/quick_voice").apply { mkdirs() }
        val outputFile = File(dir, "voice_${startTimeMs}.m4a")

        mutableState.value = QuickVoiceRecorderState.Starting(sessionId, outputFile, startTimeMs)
        recordTrace(QuickVoiceTraceEvent.RECORDER_CREATED, "Session ${sessionId.value}: Prepared file ${outputFile.name}")

        // 4. Request Foreground Service with Microphone type
        return try {
            recordTrace(QuickVoiceTraceEvent.FGS_START_REQUESTED, "Starting QuickVoiceRecordingService for session ${sessionId.value}")
            startServiceDirectly(context, sessionId, outputFile, startTimeMs)
            updateDiagnostics()

            QuickVoiceOperationResult.Starting(sessionId, outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start microphone service: ${e.message}", e)
            val isBgRestricted = e.javaClass.simpleName.contains("ForegroundServiceStartNotAllowed") ||
                                 e is SecurityException
            val errorResult = if (isBgRestricted) {
                recordTrace(
                    QuickVoiceTraceEvent.BACKGROUND_MIC_START_RESTRICTED,
                    "OS restricted background microphone start: ${e.javaClass.simpleName}: ${e.message}"
                )
                QuickVoiceOperationResult.BackgroundStartRestricted("OS restricted background microphone start: ${e.message}")
            } else {
                recordTrace(QuickVoiceTraceEvent.ERROR, "Failed to start service: ${e.message}")
                QuickVoiceOperationResult.Failed("Failed to start recording service: ${e.message}")
            }

            scope.launch(Dispatchers.IO) {
                val latest = repository?.getLatest()
                mutableState.value = QuickVoiceRecorderState.Error(e.message ?: "Failed to start service", latest)
                updateDiagnostics()
            }

            errorResult
        }
    }

    private fun stopRecordingInternal(context: Context?): QuickVoiceOperationResult {
        val current = mutableState.value
        val (sessionId, outputFile, startTimeMs) = when (current) {
            is QuickVoiceRecorderState.Recording -> Triple(current.sessionId, current.file, current.startTimeMs)
            is QuickVoiceRecorderState.Starting -> Triple(current.sessionId, current.targetFile, current.requestedAtMs)
            is QuickVoiceRecorderState.Stopping -> return QuickVoiceOperationResult.Unavailable("Recording stop is already in progress")
            else -> return QuickVoiceOperationResult.Unavailable("Not currently recording")
        }

        mutableState.value = QuickVoiceRecorderState.Stopping(sessionId, outputFile, startTimeMs)
        recordTrace(QuickVoiceTraceEvent.STOP_REQUESTED, "Stopping session ${sessionId.value}: ${outputFile.name}")
        updateDiagnostics()

        if (context != null) {
            try {
                stopServiceDirectly(context, sessionId)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping service directly: ${e.message}")
            }
        }

        return QuickVoiceOperationResult.Stopping(sessionId)
    }

    fun onServiceRecordingStarted(sessionId: RecordingSessionId, outputFile: File, startTimeMs: Long) {
        val current = mutableState.value
        if (current is QuickVoiceRecorderState.Starting && current.sessionId == sessionId) {
            val st = QuickVoiceRecorderState.Recording(sessionId, startTimeMs, outputFile)
            mutableState.value = st
            recordTrace(QuickVoiceTraceEvent.FGS_STARTED, "Service confirmed recording started for session ${sessionId.value}")
            updateDiagnostics()
        } else if (current is QuickVoiceRecorderState.Stopping && current.sessionId == sessionId) {
            // User already requested stop while starting; remain in Stopping state
            recordTrace(QuickVoiceTraceEvent.FGS_STARTED, "Service started after stop requested for session ${sessionId.value}; remaining Stopping")
        } else {
            // Stale callback from past session
            recordTrace(QuickVoiceTraceEvent.FGS_STARTED, "Ignored stale service start callback for session ${sessionId.value}")
        }
    }

    fun onServiceRecordingStopped(
        sessionId: RecordingSessionId,
        outputFile: File,
        durationMs: Long,
        isSuccess: Boolean,
        errorMessage: String?
    ) {
        recordTrace(QuickVoiceTraceEvent.RECORDER_STOPPED, "Service stopped for session ${sessionId.value} (success=$isSuccess)")

        // Exactly-once idempotency guard against duplicate callbacks
        synchronized(finalizedSessionIds) {
            if (finalizedSessionIds.contains(sessionId.value)) {
                recordTrace(QuickVoiceTraceEvent.RECORDER_STOPPED, "Ignored duplicate completion callback for session ${sessionId.value}")
                return
            }
            finalizedSessionIds.add(sessionId.value)
        }

        val current = mutableState.value
        val isSessionMatching = when (current) {
            is QuickVoiceRecorderState.Stopping -> current.sessionId == sessionId
            is QuickVoiceRecorderState.Recording -> current.sessionId == sessionId
            is QuickVoiceRecorderState.Starting -> current.sessionId == sessionId
            else -> false
        }

        if (!isSessionMatching) {
            recordTrace(QuickVoiceTraceEvent.RECORDER_STOPPED, "Ignored completion for mismatched or inactive session ${sessionId.value}")
            return
        }

        if (isSuccess && outputFile.exists() && outputFile.length() > 0) {
            val item = VoiceRecordingItem(
                id = "rec_${outputFile.nameWithoutExtension}",
                filename = outputFile.name,
                filePath = outputFile.absolutePath,
                createdAt = if (current is QuickVoiceRecorderState.Recording) current.startTimeMs else System.currentTimeMillis() - durationMs,
                durationMs = durationMs,
                sizeBytes = outputFile.length()
            )
            scope.launch(Dispatchers.IO) {
                repository?.save(item)
                mutableState.value = QuickVoiceRecorderState.Recorded(item)
                recordTrace(QuickVoiceTraceEvent.FILE_FINALIZED, "Persisted ${outputFile.name}")
                updateDiagnostics()
            }
        } else {
            scope.launch(Dispatchers.IO) {
                val latest = repository?.getLatest()
                mutableState.value = QuickVoiceRecorderState.Idle(latest)
                if (errorMessage != null) {
                    recordTrace(QuickVoiceTraceEvent.ERROR, errorMessage)
                }
                updateDiagnostics()
            }
        }
    }

    fun onServiceRecordingFailed(sessionId: RecordingSessionId, error: String) {
        recordTrace(QuickVoiceTraceEvent.ERROR, "Service error for session ${sessionId.value}: $error")
        val current = mutableState.value
        val isCurrent = when (current) {
            is QuickVoiceRecorderState.Starting -> current.sessionId == sessionId
            is QuickVoiceRecorderState.Recording -> current.sessionId == sessionId
            is QuickVoiceRecorderState.Stopping -> current.sessionId == sessionId
            else -> false
        }
        if (isCurrent) {
            scope.launch(Dispatchers.IO) {
                val latest = repository?.getLatest()
                mutableState.value = QuickVoiceRecorderState.Error(error, latest)
                updateDiagnostics()
            }
        }
    }

    suspend fun replayLastRecording(): QuickVoiceOperationResult {
        return operationMutex.withLock {
            // Reject replay if recording or starting or stopping is active
            when (val st = mutableState.value) {
                is QuickVoiceRecorderState.Recording,
                is QuickVoiceRecorderState.Starting,
                is QuickVoiceRecorderState.Stopping -> {
                    val activeSession = when (st) {
                        is QuickVoiceRecorderState.Recording -> st.sessionId.value
                        is QuickVoiceRecorderState.Starting -> st.sessionId.value
                        is QuickVoiceRecorderState.Stopping -> st.sessionId.value
                    }
                    recordTrace(
                        QuickVoiceTraceEvent.ERROR,
                        "REPLAY_LAST_RECORDING rejected: voice recorder state=${st::class.simpleName} session=$activeSession"
                    )
                    return@withLock QuickVoiceOperationResult.Unavailable(
                        "Cannot replay audio while recording is active (state=${st::class.simpleName}, session=$activeSession)"
                    )
                }
                is QuickVoiceRecorderState.Idle,
                is QuickVoiceRecorderState.Recorded,
                is QuickVoiceRecorderState.Replaying,
                is QuickVoiceRecorderState.Error -> {}
            }

            val item = repository?.getLatest()
                ?: when (val st = mutableState.value) {
                    is QuickVoiceRecorderState.Recorded -> st.item
                    is QuickVoiceRecorderState.Replaying -> st.item
                    is QuickVoiceRecorderState.Idle -> st.latestRecording
                    else -> null
                }

            if (item == null || !item.exists) {
                recordTrace(QuickVoiceTraceEvent.ERROR, "Replay rejected: No voice recording available")
                return@withLock QuickVoiceOperationResult.Unavailable("No voice recording available")
            }

            playRecordingInternal(item)
        }
    }

    suspend fun playRecording(item: VoiceRecordingItem): QuickVoiceOperationResult {
        return operationMutex.withLock {
            when (mutableState.value) {
                is QuickVoiceRecorderState.Recording,
                is QuickVoiceRecorderState.Starting,
                is QuickVoiceRecorderState.Stopping -> {
                    return@withLock QuickVoiceOperationResult.Unavailable("Cannot play audio while recording is active")
                }
                else -> {}
            }

            if (!item.exists) {
                return@withLock QuickVoiceOperationResult.Unavailable("Recording file not found on disk")
            }
            playRecordingInternal(item)
        }
    }

    private fun playRecordingInternal(item: VoiceRecordingItem): QuickVoiceOperationResult {
        // Stop conflicting playback
        StudyControllerBridge.stopAudio(StudyAudioReason.MANUAL_PLAY)
        stopPlaybackInternal()

        mutableState.value = QuickVoiceRecorderState.Replaying(item)
        recordTrace(QuickVoiceTraceEvent.REPLAY_STARTED, "Replaying recording: ${item.filename}")
        updateDiagnostics()

        val player = activeAudioPlayer ?: audioControllerProvider().also { activeAudioPlayer = it }
        player.replay(
            path = item.filePath,
            isLooping = false,
            onPlaybackEvent = { },
            onState = { state ->
                if (state is AndroidAudioState.Idle || state is AndroidAudioState.Failed) {
                    mutableState.update { current ->
                        if (current is QuickVoiceRecorderState.Replaying && current.item.id == item.id) {
                            QuickVoiceRecorderState.Recorded(item)
                        } else current
                    }
                    recordTrace(QuickVoiceTraceEvent.REPLAY_FINISHED, "Replay finished for ${item.filename}")
                    updateDiagnostics()
                }
            }
        )

        return QuickVoiceOperationResult.Replaying(item)
    }

    fun stopPlayback() {
        stopPlaybackInternal()
        scope.launch(Dispatchers.IO) {
            val latest = repository?.getLatest()
            mutableState.update { current ->
                if (current is QuickVoiceRecorderState.Replaying) {
                    QuickVoiceRecorderState.Recorded(current.item)
                } else current
            }
            updateDiagnostics()
        }
    }

    private fun stopPlaybackInternal() {
        activeAudioPlayer?.stop()
    }

    suspend fun deleteRecording(id: String): Boolean {
        return operationMutex.withLock {
            val current = mutableState.value
            if (current is QuickVoiceRecorderState.Replaying && current.item.id == id) {
                stopPlaybackInternal()
            }
            val success = repository?.delete(id) ?: false
            if (success) {
                recordTrace(QuickVoiceTraceEvent.FILE_DELETED, "Deleted recording $id")
                val latest = repository?.getLatest()
                mutableState.value = QuickVoiceRecorderState.Idle(latest)
                updateDiagnostics()
            }
            success
        }
    }

    private fun recordTrace(event: QuickVoiceTraceEvent, detail: String) {
        Log.i(TAG, "[$event] $detail")
        ControllerDiagnosticsHolder.recordQuickVoiceTrace(event, detail)
    }

    private fun updateDiagnostics() {
        val repo = repository
        val count = repo?.recordings?.value?.size ?: 0
        val latest = repo?.latestRecording?.value
        ControllerDiagnosticsHolder.updateQuickVoiceRecorderDiagnostic(
            state = mutableState.value,
            recordingsCount = count,
            latestRecording = latest
        )
    }

    fun resetForTesting(
        repo: QuickVoiceRecordingRepository? = null,
        initialState: QuickVoiceRecorderState = QuickVoiceRecorderState.Idle()
    ) {
        stopPlaybackInternal()
        activeAudioPlayer?.close()
        activeAudioPlayer = null
        repository = repo
        synchronized(finalizedSessionIds) {
            finalizedSessionIds.clear()
        }
        mutableState.value = initialState
    }
}
