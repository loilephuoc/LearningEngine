package vn.loi.learning.desktop.ui.studio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    var status by mutableStateOf<PlaybackStatus>(PlaybackStatus.Idle)
        private set

    private var activeClip: Clip? = null
    private var playbackJob: Job? = null

    fun play(audioRef: String?) {
        if (audioRef.isNullOrBlank()) {
            return
        }

        // If already playing this exact audio ref, toggle to stop
        val current = status
        if (current is PlaybackStatus.Playing && current.audioRef == audioRef) {
            stop()
            return
        }

        // Requirement: Only ONE audio may play at a time. Starting another audio automatically stops the previous playback.
        stopInternal()

        val resolvedPath = mediaStorage.resolve(audioRef)
        if (resolvedPath == null || !Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath)) {
            status = PlaybackStatus.Error(audioRef, "Cannot play audio")
            return
        }

        status = PlaybackStatus.Loading(audioRef)

        playbackJob = scope.launch {
            try {
                val clip = loadAndPlay(resolvedPath)
                if (clip != null) {
                    activeClip = clip
                    status = PlaybackStatus.Playing(audioRef)
                } else {
                    status = PlaybackStatus.Error(audioRef, "Cannot play audio")
                }
            } catch (e: Exception) {
                status = PlaybackStatus.Error(audioRef, "Cannot play audio")
            }
        }
    }

    fun stop() {
        stopInternal()
        status = PlaybackStatus.Idle
    }

    private fun stopInternal() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            activeClip?.let { clip ->
                if (clip.isRunning) {
                    clip.stop()
                }
                clip.close()
            }
        } catch (_: Exception) {
        } finally {
            activeClip = null
        }
    }

    private fun loadAndPlay(path: Path): Clip? {
        val file = path.toFile()
        return try {
            val inputStream = BufferedInputStream(file.inputStream())
            val audioInputStream = AudioSystem.getAudioInputStream(inputStream)
            val format = audioInputStream.format
            val info = DataLine.Info(Clip::class.java, format)
            val clip = AudioSystem.getLine(info) as Clip
            clip.open(audioInputStream)
            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    clip.close()
                    if (status is PlaybackStatus.Playing) {
                        status = PlaybackStatus.Idle
                    }
                }
            }
            clip.start()
            clip
        } catch (e: Exception) {
            // Handle unsupported format or sound line exception
            null
        }
    }

    fun getButtonState(audioRef: String?): AudioButtonState {
        if (audioRef.isNullOrBlank()) return AudioButtonState.Unavailable

        when (val current = status) {
            is PlaybackStatus.Error -> if (current.audioRef == audioRef) return AudioButtonState.Error(current.message)
            is PlaybackStatus.Playing -> if (current.audioRef == audioRef) return AudioButtonState.Playing
            is PlaybackStatus.Loading -> if (current.audioRef == audioRef) return AudioButtonState.Loading
            else -> {}
        }

        val resolved = mediaStorage.resolve(audioRef)
        if (resolved == null || !Files.exists(resolved)) return AudioButtonState.Unavailable

        return AudioButtonState.Play
    }
}
