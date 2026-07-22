package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LearningContentAudioTest {
    @Test
    fun `unsupported local audio fails safely without retaining playback`() {
        val unsupported = Files.createTempFile("learning-audio", ".mp3")
        Files.writeString(unsupported, "not audio")
        val player = JavaSoundLearningContentAudioPlayer()

        assertFalse(player.toggle(unsupported))
        assertNull(player.playing)
        player.close()
    }
}
