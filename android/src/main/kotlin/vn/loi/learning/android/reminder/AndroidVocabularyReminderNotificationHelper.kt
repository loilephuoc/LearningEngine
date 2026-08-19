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
        const val CHANNEL_ID = "vocabulary_reminder_v2"
        const val CHANNEL_NAME = "Vocabulary Reminder"
        const val CHANNEL_DESCRIPTION = "Periodic vocabulary flashcards and review reminders"
        const val REMINDER_NOTIFICATION_ID = 202608
        const val REMINDER_PENDING_INTENT_REQUEST_CODE = 4040

        const val THUMBNAIL_MAX_DIMENSION = 384

        const val ACTION_REMINDER_REVIEW = "vn.loi.learning.android.ACTION_REMINDER_REVIEW"
        const val EXTRA_PACKAGE_ID = "extra_package_id"
        const val EXTRA_CONTENT_ID = "extra_content_id"
        const val EXTRA_REMINDER_MODE = "extra_reminder_mode"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
