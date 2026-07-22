package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

interface LearningContentAudioPlayer : AutoCloseable {
    val playing: Path?
    fun toggle(path: Path): Boolean
    fun stop()
}

class JavaSoundLearningContentAudioPlayer : LearningContentAudioPlayer {
    private var clip: Clip? = null
    override var playing: Path? = null
        private set

    override fun toggle(path: Path): Boolean {
        if (playing == path) {
            stop()
            return true
        }
        stop()
        return try {
            AudioSystem.getAudioInputStream(path.toFile()).use { stream ->
                val next = AudioSystem.getClip()
                next.open(stream)
                next.start()
                clip = next
                playing = path
            }
            true
        } catch (_: Exception) {
            stop()
            false
        }
    }

    override fun stop() {
        clip?.stop()
        clip?.close()
        clip = null
        playing = null
    }

    override fun close() = stop()
}
