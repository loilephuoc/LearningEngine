package vn.loi.learning.android.recording

/**
 * Thread-safe bridge connecting Activity permission launcher with QuickVoiceRecorderController.
 */
object QuickVoicePermissionBridge {

    @Volatile
    private var permissionLauncher: (() -> Unit)? = null

    fun register(launcher: () -> Unit) {
        permissionLauncher = launcher
    }

    fun unregister() {
        permissionLauncher = null
    }

    fun requestPermission(): Boolean {
        val launcher = permissionLauncher
        if (launcher != null) {
            launcher.invoke()
            return true
        }
        return false
    }

    val isForegroundLauncherAttached: Boolean
        get() = permissionLauncher != null
}
