package vn.loi.learning.android.study

internal data class StudyAudioOwnerToken(val itemKey: String, val generation: Long)

/** Arbitrates Study audio independently from the visual lifetime of AnimatedContent children. */
internal class StudyAudioOwnership {
    private var generation = 0L
    private var current: StudyAudioOwnerToken? = null
    private var feedbackActive = false
    private val autoplayClaims = mutableSetOf<Pair<StudyAudioOwnerToken, AudioRole>>()
    private val foregroundStops = mutableMapOf<Any, () -> Unit>()

    fun update(authoritativeItemKey: String?, feedbackActive: Boolean): StudyAudioOwnerToken? {
        if (authoritativeItemKey == null) {
            current = null
        } else if (current?.itemKey != authoritativeItemKey) {
            generation += 1
            current = StudyAudioOwnerToken(authoritativeItemKey, generation)
        }
        this.feedbackActive = feedbackActive
        return current
    }

    fun tokenFor(itemKey: String): StudyAudioOwnerToken =
        current?.takeIf { it.itemKey == itemKey } ?: StudyAudioOwnerToken(itemKey, -1L)

    fun claimAutoplay(token: StudyAudioOwnerToken, role: AudioRole): Boolean =
        !feedbackActive && token == current && autoplayClaims.add(token to role)

    fun permitsManualPlayback(token: StudyAudioOwnerToken): Boolean = !feedbackActive && token == current

    fun isCurrent(token: StudyAudioOwnerToken): Boolean = token == current

    fun registerForegroundStop(owner: Any, stop: () -> Unit) {
        foregroundStops[owner] = stop
    }

    fun unregisterForegroundStop(owner: Any) {
        foregroundStops.remove(owner)
    }

    fun stopForForegroundLoss() {
        foregroundStops.values.toList().forEach { it() }
    }
}
