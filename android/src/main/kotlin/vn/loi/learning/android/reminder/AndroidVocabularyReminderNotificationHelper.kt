package vn.loi.learning.android.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.loi.learning.android.BuildConfig
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R

class AndroidVocabularyReminderNotificationHelper(
    private val context: Context,
    private val resolveMedia: (String) -> String? = { null }
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val helperScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var autoCancelJob: Job? = null

    init {
        createNotificationChannel()
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Clean up legacy channel v1 if present to ensure clean high-importance state
            runCatching {
                notificationManager.deleteNotificationChannel("vocabulary_reminder")
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(false)
            }
            notificationManager.createNotificationChannel(channel)

            // Clean up legacy pause channel and legacy notification if present
            runCatching {
                notificationManager.deleteNotificationChannel(PAUSE_STATUS_CHANNEL_ID)
                notificationManager.cancel(UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID)
            }
        }
    }

    fun postReminderNotification(
        candidate: AndroidVocabularyCandidate,
        mode: AndroidVocabularyReminderSelectionMode,
        displayDurationMillis: Long
    ): Boolean {
        if (!hasNotificationPermission()) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_REMINDER_REVIEW
            putExtra(EXTRA_PACKAGE_ID, candidate.packageId.value)
            putExtra(EXTRA_CONTENT_ID, candidate.contentId.value)
            putExtra(EXTRA_REMINDER_MODE, mode.name)
            putExtra(EXTRA_NOTIFICATION_ID, REMINDER_NOTIFICATION_ID)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REMINDER_PENDING_INTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val meaningLine = candidate.translation?.takeIf { it.isNotBlank() }
            ?: candidate.answer?.takeIf { it.isNotBlank() }
            ?: ""

        // Resolve and decode vocabulary image
        val resolvedPath = candidate.imageReference?.let(resolveMedia)
        val thumbnailBitmap = resolvedPath?.let { path ->
            decodeVocabularyThumbnail(candidate.contentId.value, candidate.imageReference, path)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(candidate.primaryText)
            .setContentText(meaningLine)
            .setLargeIcon(thumbnailBitmap)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setTimeoutAfter(displayDurationMillis)

        if (thumbnailBitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(thumbnailBitmap)
                    .bigLargeIcon(null as Bitmap?)
                    .setBigContentTitle(candidate.primaryText)
                    .setSummaryText(meaningLine)
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(meaningLine)
                    .setBigContentTitle(candidate.primaryText)
            )
        }

        val notification = builder.build()
        notificationManager.notify(REMINDER_NOTIFICATION_ID, notification)

        // Schedule auto-cancellation after configured display duration
        autoCancelJob?.cancel()
        autoCancelJob = helperScope.launch {
            delay(displayDurationMillis)
            cancelNotification()
        }

        return true
    }

    fun cancelNotification() {
        autoCancelJob?.cancel()
        autoCancelJob = null
        notificationManager.cancel(REMINDER_NOTIFICATION_ID)
    }

    fun postPauseStatusNotification(durationMinutes: Long, pausedUntilEpochMillis: Long): Boolean {
        if (!hasNotificationPermission()) return false

        val timeStr = Instant.ofEpochMilli(pausedUntilEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))

        val durationLabel = when (durationMinutes) {
            60L -> "1 hour"
            else -> "$durationMinutes minutes"
        }
        val contentText = "Paused for $durationLabel (until $timeStr)"

        // Body Tap -> triggers Resume now directly (Fallback for HyperOS collapsed view)
        val bodyResumeIntent = Intent(context, AndroidVocabularyReminderResumeReceiver::class.java).apply {
            action = ACTION_RESUME_UNLOCKED_NOW
            putExtra(EXTRA_RESUME_SOURCE, "NOTIFICATION_BODY")
        }
        val bodyPendingIntent = PendingIntent.getBroadcast(
            context,
            PAUSE_CONTENT_INTENT_REQUEST_CODE,
            bodyResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Button Tap -> triggers Resume now via Action
        val actionResumeIntent = Intent(context, AndroidVocabularyReminderResumeReceiver::class.java).apply {
            action = ACTION_RESUME_UNLOCKED_NOW
            putExtra(EXTRA_RESUME_SOURCE, "NOTIFICATION_ACTION")
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            RESUME_ACTION_REQUEST_CODE,
            actionResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // RemoteViews Custom Resume Button Tap
        val customResumeIntent = Intent(context, AndroidVocabularyReminderResumeReceiver::class.java).apply {
            action = ACTION_RESUME_UNLOCKED_NOW
            putExtra(EXTRA_RESUME_SOURCE, "NOTIFICATION_CUSTOM_RESUME")
        }
        val customResumePendingIntent = PendingIntent.getBroadcast(
            context,
            RESUME_CUSTOM_REQUEST_CODE,
            customResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteViews = android.widget.RemoteViews(context.packageName, R.layout.notification_unlocked_pause_status).apply {
            setTextViewText(R.id.pause_notif_title, "Vocabulary reminders paused")
            setTextViewText(R.id.pause_notif_text, contentText)
            setOnClickPendingIntent(R.id.pause_resume_now, customResumePendingIntent)
        }

        val builder = NotificationCompat.Builder(context, PAUSE_STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle("Vocabulary reminders paused")
            .setContentText(contentText)
            .setContentIntent(bodyPendingIntent)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .addAction(
                R.drawable.ic_notification_reminder,
                "Resume now",
                actionPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)

        Log.i(TAG_PAUSE_NOTIF, "[UnlockedPauseNotification] action=SHOW duration=${durationMinutes}m pausedUntil=$pausedUntilEpochMillis hasResumeAction=true hasResumeContentIntent=true hasCustomResume=true notificationId=$UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID")
        notificationManager.notify(UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID, builder.build())
        return true
    }

    fun cancelPauseStatusNotification(reason: String) {
        Log.i(TAG_PAUSE_NOTIF, "[UnlockedPauseNotification] action=CANCEL reason=$reason")
        notificationManager.cancel(UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID)
    }

    fun decodeVocabularyThumbnail(contentId: String, rawRef: String, resolvedPath: String?): Bitmap? {
        return decodeBitmapSafely(contentId, rawRef, resolvedPath, maxDimension = THUMBNAIL_MAX_DIMENSION, tag = "Thumbnail")
    }

    private fun decodeBitmapSafely(
        contentId: String,
        rawRef: String,
        resolvedPath: String?,
        maxDimension: Int,
        tag: String
    ): Bitmap? {
        val file = resolvedPath?.let(::File)
        val fileExists = file?.exists() == true && file.isFile

        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "ImageResolution [$tag]: contentId=$contentId, imageRef=$rawRef, resolvedPath=$resolvedPath, fileExists=$fileExists, maxDim=$maxDimension"
            )
        }

        if (file == null || !fileExists) return null

        return runCatching {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            var inSampleSize = 1
            if (boundsOptions.outHeight > maxDimension || boundsOptions.outWidth > maxDimension) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            if (BuildConfig.DEBUG && bitmap != null) {
                Log.d(
                    TAG,
                    "ImageDecoded [$tag]: contentId=$contentId, width=${bitmap.width}, height=${bitmap.height}, sampleSize=$inSampleSize"
                )
            }
            bitmap
        }.getOrNull()
    }

    companion object {
        private const val TAG = "VocabularyReminder"
        private const val TAG_PAUSE_NOTIF = "UnlockedPauseNotification"
        const val CHANNEL_ID = "vocabulary_reminder_v2"
        const val CHANNEL_NAME = "Vocabulary Reminder"
        const val CHANNEL_DESCRIPTION = "Periodic vocabulary flashcards and review reminders"
        const val REMINDER_NOTIFICATION_ID = 202608
        const val REMINDER_PENDING_INTENT_REQUEST_CODE = 4040

        const val PAUSE_STATUS_CHANNEL_ID = "unlocked_reminder_pause_status"
        const val PAUSE_STATUS_CHANNEL_NAME = "Vocabulary reminder pause status"
        const val UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID = 20311
        const val PAUSE_CONTENT_INTENT_REQUEST_CODE = 4041
        const val RESUME_ACTION_REQUEST_CODE = 4042
        const val RESUME_CUSTOM_REQUEST_CODE = 4043

        const val THUMBNAIL_MAX_DIMENSION = 384

        const val ACTION_REMINDER_REVIEW = "vn.loi.learning.android.ACTION_REMINDER_REVIEW"
        const val ACTION_RESUME_UNLOCKED_NOW = "vn.loi.learning.android.ACTION_RESUME_UNLOCKED_NOW"
        const val EXTRA_RESUME_SOURCE = "extra_resume_source"
        const val EXTRA_PACKAGE_ID = "extra_package_id"
        const val EXTRA_CONTENT_ID = "extra_content_id"
        const val EXTRA_REMINDER_MODE = "extra_reminder_mode"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
