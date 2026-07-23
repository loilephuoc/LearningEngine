package vn.loi.learning.desktop.ui.library

/**
 * Sanitizes technical failures into safe, user-friendly Desktop Library error messages.
 * Prevents leaking filesystem paths, class names, stack traces, internal IDs, or secrets.
 */
internal object LibraryFailureMessage {

    fun forFailure(failure: Throwable): String {
        val usefulMessage = failure.firstUsefulMessage() ?: "An infrastructure failure occurred."
        val sanitized = sanitize(usefulMessage)
        return "Unable to load library: $sanitized"
    }

    internal fun sanitize(message: String): String {
        var clean = message
        // Remove file paths (Windows paths like C:\... or Unix paths starting with / or file://)
        clean = clean.replace(Regex("""[A-Za-z]:\\[^\s:]+"""), "[path]")
        clean = clean.replace(Regex("""/(?:[^\s:]+/)+[^\s:]+"""), "[path]")
        clean = clean.replace(Regex("""file:///[^\s:]+"""), "[path]")
        // Remove Java/Kotlin class names like java.lang.Exception or vn.loi.learning...
        clean = clean.replace(Regex("""\b(?:[a-zA-Z_][a-zA-Z0-9_]*\.)+[a-zA-Z_][a-zA-Z0-9_]*:?"""), "")
        clean = clean.replace(Regex("""\b[a-zA-Z_][a-zA-Z0-9_]*(?:Exception|Error):?"""), "")


        // Remove secrets/tokens pattern (e.g. secret=..., token=..., password=...)
        clean = clean.replace(Regex("""(?i)(secret|token|password|key)\s*=\s*\S+"""), "$1=[redacted]")
        clean = clean.trim()
        if (clean.isBlank()) return "An infrastructure failure occurred."
        return clean
    }

    private fun Throwable.firstUsefulMessage(): String? {
        var current: Throwable? = this
        while (current != null) {
            val msg = current.message?.trim()
            if (!msg.isNullOrEmpty() && !msg.startsWith("java.") && !msg.startsWith("kotlin.")) {
                return msg
            }
            current = current.cause.takeUnless { it === current }
        }
        return this.message?.trim()
    }
}
