package vn.loi.learning.android.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import vn.loi.learning.android.MainActivity
import java.io.File

/**
 * Foreground Service that owns the MediaRecorder microphone capture lifecycle and file finalization.
 */
class QuickVoiceRecordingService : Service() {

    private val TAG = "QuickVoiceRecService"

    private var activeSessionId: String? = null
    private var activeMediaRecorder: MediaRecorder? = null
    private var activeOutputFile: File? = null
    private var activeStartTimeMs: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RECORDING -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                stopAndFinalize(sessionId)
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_START_RECORDING -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: "session_${System.currentTimeMillis()}"
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                val startTimeMs = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
                if (filePath != null) {
                    startRecordingInternal(sessionId, File(filePath), startTimeMs)
                }
                return START_STICKY
            }
            else -> {
                return START_NOT_STICKY
            }
        }
    }

    private fun startRecordingInternal(sessionId: String, outputFile: File, startTimeMs: Long) {
        // If an existing recorder was somehow running, release it first
        if (activeMediaRecorder != null) {
            releaseRecorder()
        }

        activeSessionId = sessionId
        activeOutputFile = outputFile
        activeStartTimeMs = startTimeMs

        // 1. Enter foreground immediately
        startForegroundNotification()

        // 2. Prepare and start MediaRecorder inside the service boundary
        try {
            outputFile.parentFile?.mkdirs()

            val recorder = createMediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            activeMediaRecorder = recorder
            QuickVoiceRecorderController.onServiceRecordingStarted(RecordingSessionId(sessionId), outputFile, startTimeMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaRecorder in service: ${e.message}", e)
            releaseRecorder()
            QuickVoiceRecorderController.onServiceRecordingFailed(
                RecordingSessionId(sessionId),
                e.message ?: "Failed to initialize MediaRecorder"
            )
            stopForegroundService()
        }
    }

    private fun stopAndFinalize(requestedSessionId: String?) {
        val sessionId = activeSessionId ?: requestedSessionId ?: "unknown"
        val outputFile = activeOutputFile
        val startTime = activeStartTimeMs
        val durationMs = if (startTime > 0) System.currentTimeMillis() - startTime else 0L

        var isSuccess = false
        var errorMessage: String? = null

        try {
            activeMediaRecorder?.apply {
                stop()
                release()
            }
            // File validation: exists and has non-trivial byte size (> 0)
            if (outputFile != null && outputFile.exists() && outputFile.length() > 0) {
                isSuccess = true
            } else {
                errorMessage = "Recording too short or file empty"
                outputFile?.delete()
            }
        } catch (e: RuntimeException) {
            // MediaRecorder.stop() throws RuntimeException if no valid audio data was captured (< 1s)
            Log.w(TAG, "MediaRecorder stop failed (possibly too short): ${e.message}")
            outputFile?.delete()
            errorMessage = "Recording too short or discarded"
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error stopping MediaRecorder: ${e.message}", e)
            outputFile?.delete()
            errorMessage = e.message
        } finally {
            activeMediaRecorder = null
            activeOutputFile = null
            activeStartTimeMs = 0L
            activeSessionId = null

            if (outputFile != null) {
                QuickVoiceRecorderController.onServiceRecordingStopped(
                    sessionId = RecordingSessionId(sessionId),
                    outputFile = outputFile,
                    durationMs = durationMs,
                    isSuccess = isSuccess,
                    errorMessage = errorMessage
                )
            }
        }
    }

    private fun startForegroundNotification() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quick Voice Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification while recording quick voice audio"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, QuickVoiceRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
            activeSessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Learning Engine")
            .setContentText("Đang ghi âm...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()
    }

    private fun releaseRecorder() {
        try {
            activeMediaRecorder?.release()
        } catch (_: Exception) {}
        activeMediaRecorder = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseRecorder()
        stopForegroundService()
    }

    companion object {
        const val CHANNEL_ID = "quick_voice_recording"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START_RECORDING = "vn.loi.learning.android.recording.ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "vn.loi.learning.android.recording.ACTION_STOP_RECORDING"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_START_TIME = "extra_start_time"

        internal var createMediaRecorder: (Context) -> MediaRecorder = { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
        }

        fun start(context: Context, sessionId: RecordingSessionId, outputFile: File, startTimeMs: Long) {
            val intent = Intent(context, QuickVoiceRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_SESSION_ID, sessionId.value)
                putExtra(EXTRA_FILE_PATH, outputFile.absolutePath)
                putExtra(EXTRA_START_TIME, startTimeMs)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context, sessionId: RecordingSessionId? = null) {
            val intent = Intent(context, QuickVoiceRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
                sessionId?.let { putExtra(EXTRA_SESSION_ID, it.value) }
            }
            context.startService(intent)
        }
    }
}
