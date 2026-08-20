package vn.loi.learning.android.media

import android.content.Context
import vn.loi.learning.android.platform.coordinatedApply
import android.content.SharedPreferences
import java.util.Collections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persisted storage interface for application-wide audio mute setting.
 */
interface AudioMutePreferenceStore {
    fun loadMuted(): Boolean
    fun saveMuted(muted: Boolean)
}

/**
 * SharedPreferences-backed audio mute preference store with fallback/migration
 * from legacy AutoPlay mute preferences if present.
 */
class SharedPreferencesAudioMutePreferenceStore(context: Context) : AudioMutePreferenceStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val legacyAutoPlayPrefs: SharedPreferences = context.getSharedPreferences(LEGACY_AUTOPLAY_FILE, Context.MODE_PRIVATE)

    override fun loadMuted(): Boolean {
        if (prefs.contains(KEY_MUTED)) {
            return prefs.getBoolean(KEY_MUTED, false)
        }
        if (legacyAutoPlayPrefs.contains(LEGACY_KEY_MUTED)) {
            val legacy = legacyAutoPlayPrefs.getBoolean(LEGACY_KEY_MUTED, false)
            saveMuted(legacy)
            return legacy
        }
        return false
    }

    override fun saveMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_MUTED, muted).coordinatedApply()
    }

    companion object {
        const val FILE_NAME = "learning_engine_audio_prefs"
        const val KEY_MUTED = "app.audio.muted"
        const val LEGACY_AUTOPLAY_FILE = "autoplay_preferences"
        const val LEGACY_KEY_MUTED = "autoplay.muted"
    }
}

/**
 * Contract implemented by any audio player in LearningEngine that respects global muting.
 */
interface MuteableAudioPlayer {
    fun applyMute(muted: Boolean)
}

/**
 * Single application-level authority managing LearningEngine global app mute/unmute state.
 *
 * All players (Study foreground/background, AutoPlay ExoPlayer, Typing, Library) observe
 * this single authority. Toggling mute immediately mutes or unmutes all active players
 * without stopping or duplicating loops, and ensures new playbacks start silently when muted.
 */
object LearningEngineAudioPolicy {

    private var store: AudioMutePreferenceStore? = null
    private val mutableIsMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = mutableIsMuted.asStateFlow()

    private val activePlayers = Collections.synchronizedSet(mutableSetOf<MuteableAudioPlayer>())

    fun init(store: AudioMutePreferenceStore) {
        this.store = store
        mutableIsMuted.value = store.loadMuted()
    }

    fun init(context: Context) {
        init(SharedPreferencesAudioMutePreferenceStore(context.applicationContext))
    }

    fun setMuted(muted: Boolean) {
        if (mutableIsMuted.value == muted) return
        mutableIsMuted.value = muted
        store?.saveMuted(muted)
        notifyPlayers(muted)
    }

    fun toggleMuted(): Boolean {
        val next = !mutableIsMuted.value
        setMuted(next)
        return next
    }

    fun registerPlayer(player: MuteableAudioPlayer) {
        activePlayers.add(player)
        player.applyMute(mutableIsMuted.value)
    }

    fun unregisterPlayer(player: MuteableAudioPlayer) {
        activePlayers.remove(player)
    }

    private fun notifyPlayers(muted: Boolean) {
        val players = synchronized(activePlayers) { activePlayers.toList() }
        players.forEach { it.applyMute(muted) }
    }

    fun resetForTesting(initialMuted: Boolean = false) {
        store = null
        mutableIsMuted.value = initialMuted
        synchronized(activePlayers) { activePlayers.clear() }
    }
}
