package vn.loi.learning.android.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import vn.loi.learning.android.platform.AndroidStartupTrace

sealed interface AndroidAudioState {
    data object Idle : AndroidAudioState
    data object Preparing : AndroidAudioState
    data object Playing : AndroidAudioState
    data object Unavailable : AndroidAudioState
    data class Failed(val reason: String? = null) : AndroidAudioState
}

sealed interface AndroidAudioSource {
    data class LocalFile(val path: String) : AndroidAudioSource
    data class FileUri(val uriString: String) : AndroidAudioSource
    data class ContentUri(val uriString: String) : AndroidAudioSource
    data class ResourceUri(val uriString: String) : AndroidAudioSource

    companion object {
        fun classify(rawPath: String?): AndroidAudioSource? {
            if (rawPath.isNullOrBlank()) return null
            val trimmed = rawPath.trim()
            return when {
                trimmed.startsWith("content://") -> ContentUri(trimmed)
                trimmed.startsWith("android.resource://") -> ResourceUri(trimmed)
                trimmed.startsWith("file://") -> FileUri(trimmed)
                else -> LocalFile(trimmed)
            }
        }
    }
}

class AndroidAudioController(
    context: Context? = null,
    private val createPlayer: () -> MediaPlayer = ::MediaPlayer
) : AutoCloseable {
    private val appContext: Context? = context?.applicationContext
    private var player: MediaPlayer? = null
    private var activeSessionId: Long = 0L

    private fun log(error: Boolean, message: String) {
        AndroidStartupTrace.write(error, "AndroidAudioController: $message")
    }

    fun replay(
        path: String?,
        isLooping: Boolean = false,
        onState: (AndroidAudioState) -> Unit = {}
    ): AndroidAudioState {
        if (path.isNullOrBlank()) {
            log(false, "audio_resolve_result null_or_blank")
            return AndroidAudioState.Unavailable
        }

        val source = AndroidAudioSource.classify(path) ?: run {
            log(true, "Failed to classify audio source: $path")
            return AndroidAudioState.Unavailable
        }

        close()
        val currentSessionId = ++activeSessionId

        return try {
            val mediaPlayer = createPlayer()
            player = mediaPlayer
            mediaPlayer.isLooping = isLooping

            log(false, "audio_source_type ${source::class.simpleName}")

            when (source) {
                is AndroidAudioSource.LocalFile -> {
                    val file = File(source.path)
                    val exists = file.exists()
                    val readable = file.canRead()
                    log(false, "Local file target audio_target_exists=$exists audio_target_readable=$readable")
                    if (!exists || !readable) {
                        close()
                        return AndroidAudioState.Failed("File target not accessible")
                    }
                    FileInputStream(file).use { fis ->
                        mediaPlayer.setDataSource(fis.fd)
                    }
                }
                is AndroidAudioSource.FileUri -> {
                    val uri = Uri.parse(source.uriString)
                    val filePath = uri.path
                    if (filePath != null && File(filePath).exists() && File(filePath).canRead()) {
                        FileInputStream(File(filePath)).use { fis ->
                            mediaPlayer.setDataSource(fis.fd)
                        }
                    } else if (appContext != null) {
                        mediaPlayer.setDataSource(appContext, uri)
                    } else {
                        mediaPlayer.setDataSource(source.uriString)
                    }
                }
                is AndroidAudioSource.ContentUri -> {
                    val uri = Uri.parse(source.uriString)
                    if (appContext != null) {
                        mediaPlayer.setDataSource(appContext, uri)
                    } else {
                        close()
                        return AndroidAudioState.Failed("Context required for content URI")
                    }
                }
                is AndroidAudioSource.ResourceUri -> {
                    val uri = Uri.parse(source.uriString)
                    if (appContext != null) {
                        mediaPlayer.setDataSource(appContext, uri)
                    } else {
                        close()
                        return AndroidAudioState.Failed("Context required for resource URI")
                    }
                }
            }

            mediaPlayer.setOnCompletionListener {
                if (currentSessionId == activeSessionId) {
                    log(false, "audio_completed")
                    close()
                    onState(AndroidAudioState.Idle)
                }
            }

            mediaPlayer.setOnErrorListener { _, what, extra ->
                if (currentSessionId == activeSessionId) {
                    log(true, "audio_failed MediaPlayer error: what=$what, extra=$extra")
                    close()
                    onState(AndroidAudioState.Failed("MediaPlayer error $what, $extra"))
                }
                true
            }

            mediaPlayer.setOnPreparedListener { mp ->
                if (currentSessionId == activeSessionId) {
                    log(false, "audio_prepared starting playback")
                    mp.start()
                    log(false, "audio_playing")
                    onState(AndroidAudioState.Playing)
                }
            }

            log(false, "audio_prepare_start")
            mediaPlayer.prepareAsync()
            AndroidAudioState.Preparing
        } catch (e: IOException) {
            log(true, "IOException setting up player: ${e.message}")
            close()
            AndroidAudioState.Failed("IO error: ${e.message}")
        } catch (e: SecurityException) {
            log(true, "SecurityException setting up player: ${e.message}")
            close()
            AndroidAudioState.Failed("Security error: ${e.message}")
        } catch (e: IllegalArgumentException) {
            log(true, "IllegalArgumentException setting up player: ${e.message}")
            close()
            AndroidAudioState.Failed("Invalid argument: ${e.message}")
        } catch (e: IllegalStateException) {
            log(true, "IllegalStateException setting up player: ${e.message}")
            close()
            AndroidAudioState.Failed("Invalid state: ${e.message}")
        } catch (e: Exception) {
            log(true, "Unexpected exception setting up player: ${e.message}")
            close()
            AndroidAudioState.Failed("Error: ${e.message}")
        }
    }

    fun stop() {
        close()
    }

    override fun close() {
        activeSessionId++
        player?.runCatching {
            if (isPlaying) {
                stop()
            }
        }
        player?.runCatching { release() }
        player = null
        log(false, "player_released")
    }
}
