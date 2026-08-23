package vn.loi.learning.android.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import vn.loi.learning.android.MainActivity
import vn.loi.learning.android.R

class DailyLearningNotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
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

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dueChannel = NotificationChannel(
                CHANNEL_DUE_REVIEW,
                "Nhắc ôn tập đến hạn",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo hàng ngày nhắc bạn ôn tập các từ vựng đã đến hạn ghi nhớ"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(dueChannel)

            val inactivityChannel = NotificationChannel(
                CHANNEL_INACTIVITY,
                "Nhắc quay lại học",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo nhắc bạn quay lại học từ mới khi đã lâu không mở ứng dụng"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(inactivityChannel)
        }
    }

    fun postDueReviewNotification(packageName: String, dueCount: Int, packageId: String): Boolean {
        if (!hasNotificationPermission()) return false

        // Tap content intent -> opens Review route in MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_DUE_REVIEW
            putExtra(EXTRA_PACKAGE_ID, packageId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_DUE_CONTENT,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss type action intent -> disables this reminder type in preferences
        val disableIntent = Intent(context, DailyLearningReminderReceiver::class.java).apply {
            action = ACTION_DISABLE_DUE_REMINDER
        }
        val disablePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DUE_DISABLE,
            disableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Nhắc nhở ôn tập"
        val text = "Bạn có $dueCount từ đến hạn cần ôn tập trong gói \"$packageName\"."

        val notification = NotificationCompat.Builder(context, CHANNEL_DUE_REVIEW)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Ẩn loại nhắc này", disablePendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_DUE_REVIEW, notification)
        return true
    }

    fun postInactivityReminderNotification(packageName: String, daysInactive: Int, packageId: String): Boolean {
        if (!hasNotificationPermission()) return false

        // Tap content intent -> opens Study route in MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_INACTIVITY_LEARN
            putExtra(EXTRA_PACKAGE_ID, packageId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_INACTIVITY_CONTENT,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss type action intent -> disables this reminder type in preferences
        val disableIntent = Intent(context, DailyLearningReminderReceiver::class.java).apply {
            action = ACTION_DISABLE_INACTIVITY_REMINDER
        }
        val disablePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_INACTIVITY_DISABLE,
            disableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Tiếp tục học từ mới"
        val text = "Đã $daysInactive ngày bạn chưa học. Hãy tiếp tục học các từ mới trong gói \"$packageName\" nhé!"

        val notification = NotificationCompat.Builder(context, CHANNEL_INACTIVITY)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Ẩn loại nhắc này", disablePendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_INACTIVITY, notification)
        return true
    }

    companion object {
        const val CHANNEL_DUE_REVIEW = "learning_due_review_reminder"
        const val CHANNEL_INACTIVITY = "learning_inactivity_reminder"

        const val NOTIFICATION_ID_DUE_REVIEW = 8001
        const val NOTIFICATION_ID_INACTIVITY = 8002

        const val ACTION_OPEN_DUE_REVIEW = "vn.loi.learning.android.action.OPEN_DUE_REVIEW"
        const val ACTION_OPEN_INACTIVITY_LEARN = "vn.loi.learning.android.action.OPEN_INACTIVITY_LEARN"
        const val ACTION_DISABLE_DUE_REMINDER = "vn.loi.learning.android.action.DISABLE_DUE_REMINDER"
        const val ACTION_DISABLE_INACTIVITY_REMINDER = "vn.loi.learning.android.action.DISABLE_INACTIVITY_REMINDER"

        const val EXTRA_PACKAGE_ID = "package_id"

        const val REQUEST_CODE_DUE_CONTENT = 8101
        const val REQUEST_CODE_DUE_DISABLE = 8102
        const val REQUEST_CODE_INACTIVITY_CONTENT = 8103
        const val REQUEST_CODE_INACTIVITY_DISABLE = 8104
    }
}
