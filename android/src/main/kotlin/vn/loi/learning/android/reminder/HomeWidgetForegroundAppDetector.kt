package vn.loi.learning.android.reminder

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log

enum class HomeForegroundState {
    HOME,
    OTHER_APP,
    UNKNOWN
}

data class HomeForegroundDetectionResult(
    val state: HomeForegroundState,
    val hasPermission: Boolean,
    val foregroundPackage: String? = null,
    val launcherPackage: String? = null,
    val reason: String? = null
)

interface HomeWidgetForegroundDetector {
    fun hasUsageAccess(): Boolean
    fun detectForeground(): HomeForegroundDetectionResult
}

class HomeWidgetForegroundAppDetector(
    private val context: Context,
    private val defaultLauncherResolver: () -> String? = { null }
) : HomeWidgetForegroundDetector {

    override fun hasUsageAccess(): Boolean {
        return checkUsageAccess(context)
    }

    override fun detectForeground(): HomeForegroundDetectionResult {
        if (!hasUsageAccess()) {
            Log.i(
                TAG,
                "[HOME_WIDGET_FOREGROUND] permission=false state=UNKNOWN reason=USAGE_ACCESS_NOT_GRANTED"
            )
            return HomeForegroundDetectionResult(
                state = HomeForegroundState.UNKNOWN,
                hasPermission = false,
                reason = "USAGE_ACCESS_NOT_GRANTED"
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager == null) {
            Log.i(
                TAG,
                "[HOME_WIDGET_FOREGROUND] permission=true state=UNKNOWN reason=USAGE_STATS_SERVICE_UNAVAILABLE"
            )
            return HomeForegroundDetectionResult(
                state = HomeForegroundState.UNKNOWN,
                hasPermission = true,
                reason = "USAGE_STATS_SERVICE_UNAVAILABLE"
            )
        }

        val defaultLauncher = defaultLauncherResolver()
        val now = System.currentTimeMillis()
        val startTime = now - LOOKBACK_WINDOW_MS
        val usageEvents = try {
            usageStatsManager.queryEvents(startTime, now)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to query usage events: ${e.message}")
            null
        }

        var latestPkg: String? = null
        var latestTimestamp = 0L

        if (usageEvents != null) {
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val eventType = event.eventType
                val isForegroundEvent = eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                        eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                if (isForegroundEvent && event.timeStamp >= latestTimestamp) {
                    val pkg = event.packageName
                    if (!AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage(pkg)) {
                        latestPkg = pkg
                        latestTimestamp = event.timeStamp
                    }
                }
            }
        }

        // Fallback to queryUsageStats if no foreground events were found in the short lookback window
        if (latestPkg == null) {
            val statsList = try {
                usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    now - DAILY_FALLBACK_WINDOW_MS,
                    now
                )
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to query usage stats fallback: ${e.message}")
                null
            }

            val topStat = statsList
                ?.filter { !AndroidHomeVocabularyWidgetCoordinator.isTransientSystemPackage(it.packageName) }
                ?.maxByOrNull { it.lastTimeUsed }

            if (topStat != null && topStat.lastTimeUsed > 0L) {
                latestPkg = topStat.packageName
                latestTimestamp = topStat.lastTimeUsed
            }
        }

        if (latestPkg.isNullOrBlank()) {
            Log.i(
                TAG,
                "[HOME_WIDGET_FOREGROUND] permission=true state=UNKNOWN reason=NO_RECENT_USAGE_DATA"
            )
            return HomeForegroundDetectionResult(
                state = HomeForegroundState.UNKNOWN,
                hasPermission = true,
                reason = "NO_RECENT_USAGE_DATA"
            )
        }

        val isLauncher = AndroidHomeVocabularyWidgetCoordinator.isLauncherPackage(latestPkg, defaultLauncher)
        val isOurApp = latestPkg == context.packageName

        val state = when {
            isLauncher -> HomeForegroundState.HOME
            isOurApp -> HomeForegroundState.OTHER_APP
            else -> HomeForegroundState.OTHER_APP
        }

        Log.i(
            TAG,
            "[HOME_WIDGET_FOREGROUND] permission=true foregroundPkg=$latestPkg launcherPkg=$defaultLauncher state=$state"
        )

        return HomeForegroundDetectionResult(
            state = state,
            hasPermission = true,
            foregroundPackage = latestPkg,
            launcherPackage = defaultLauncher
        )
    }

    companion object {
        const val TAG = "HomeWidgetForeground"
        private const val LOOKBACK_WINDOW_MS = 15 * 60 * 1000L // 15 minutes
        private const val DAILY_FALLBACK_WINDOW_MS = 24 * 60 * 60 * 1000L // 24 hours

        fun checkUsageAccess(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun openUsageAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallbackIntent)
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to open usage access settings: ${e.message}")
                }
            }
        }
    }
}
