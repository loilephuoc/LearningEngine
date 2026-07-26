package vn.loi.learning.desktop.ui.studio

import java.nio.file.Path

sealed interface AudioPlayerState {
    object Idle : AudioPlayerState
    data class Loading(val path: Path) : AudioPlayerState
    data class Playing(val path: Path) : AudioPlayerState
    data class Paused(val path: Path) : AudioPlayerState
    data class Error(val path: Path, val message: String) : AudioPlayerState
}

interface AudioPlayer {
    val state: AudioPlayerState

    fun load(path: Path)
    fun play(path: Path)
    fun pause()
    fun stop()
    fun release()

    fun positionMs(): Long
    fun durationMs(): Long
}
