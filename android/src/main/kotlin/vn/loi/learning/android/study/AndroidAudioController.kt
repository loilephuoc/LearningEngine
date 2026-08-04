package vn.loi.learning.android.study

import android.media.MediaPlayer

sealed interface AndroidAudioState {
    data object Idle : AndroidAudioState
    data object Playing : AndroidAudioState
    data object Unavailable : AndroidAudioState
    data object Failed : AndroidAudioState
}

class AndroidAudioController(
    private val createPlayer: () -> MediaPlayer = ::MediaPlayer
) : AutoCloseable {
    private var player: MediaPlayer? = null

    fun replay(path: String?): AndroidAudioState {
        if (path == null) return AndroidAudioState.Unavailable
        close()
        return runCatching {
            createPlayer().also { mediaPlayer ->
                player = mediaPlayer
                mediaPlayer.setDataSource(path)
                mediaPlayer.setOnCompletionListener { close() }
                mediaPlayer.prepare()
                mediaPlayer.start()
            }
            AndroidAudioState.Playing
        }.getOrElse {
            close()
            AndroidAudioState.Failed
        }
    }

    override fun close() {
        player?.runCatching { stop() }
        player?.release()
        player = null
    }
}
