package vn.loi.learning.desktop.ui.studio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.file.Files
import vn.loi.learning.application.port.ContentMediaStorage

sealed interface PlaybackStatus {
    object Idle : PlaybackStatus
    data class Loading(val audioRef: String) : PlaybackStatus
    data class Playing(val audioRef: String) : PlaybackStatus
    data class Error(val audioRef: String, val message: String) : PlaybackStatus
    data class Unavailable(val audioRef: String) : PlaybackStatus
}

sealed interface AudioButtonState {
    object Play : AudioButtonState
    object Playing : AudioButtonState
    object Loading : AudioButtonState
    object Unavailable : AudioButtonState
    data class Error(val message: String) : AudioButtonState
}

class PlaybackCoordinator(
    private val mediaStorage: ContentMediaStorage,
    private val audioPlayer: AudioPlayer = DesktopAudioPlayer()
) {
    private var currentAudioRef by mutableStateOf<String?>(null)
    private var explicitErrorRef by mutableStateOf<Pair<String, String>?>(null)

    val status: PlaybackStatus
        get() {
            val err = explicitErrorRef
            if (err != null) {
                return PlaybackStatus.Error(err.first, err.second)
            }
            val ref = currentAudioRef ?: return PlaybackStatus.Idle
            return when (val playerState = audioPlayer.state) {
                is AudioPlayerState.Idle -> PlaybackStatus.Idle
                is AudioPlayerState.Loading -> PlaybackStatus.Loading(ref)
                is AudioPlayerState.Playing -> PlaybackStatus.Playing(ref)
                is AudioPlayerState.Paused -> PlaybackStatus.Idle
                is AudioPlayerState.Error -> PlaybackStatus.Error(ref, playerState.message)
            }
        }

    fun play(audioRef: String?) {
        if (audioRef.isNullOrBlank()) return

        val current = status
        if (current is PlaybackStatus.Playing && current.audioRef == audioRef) {
            stop()
            return
        }

        stop()
        explicitErrorRef = null

        val resolvedPath = mediaStorage.resolve(audioRef)
        if (resolvedPath == null || !Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath)) {
            currentAudioRef = audioRef
            explicitErrorRef = Pair(audioRef, "Cannot play audio")
            return
        }

        currentAudioRef = audioRef
        audioPlayer.play(resolvedPath)
    }

    fun stop() {
        audioPlayer.stop()
        currentAudioRef = null
        explicitErrorRef = null
    }

    fun getButtonState(audioRef: String?): AudioButtonState {
        if (audioRef.isNullOrBlank()) return AudioButtonState.Unavailable

        if (currentAudioRef == audioRef || explicitErrorRef?.first == audioRef) {
            when (val current = status) {
                is PlaybackStatus.Error -> return AudioButtonState.Error(current.message)
                is PlaybackStatus.Playing -> return AudioButtonState.Playing
                is PlaybackStatus.Loading -> return AudioButtonState.Loading
                else -> {}
            }
        }

        val resolved = mediaStorage.resolve(audioRef)
        if (resolved == null || !Files.exists(resolved)) return AudioButtonState.Unavailable

        return AudioButtonState.Play
    }
}
