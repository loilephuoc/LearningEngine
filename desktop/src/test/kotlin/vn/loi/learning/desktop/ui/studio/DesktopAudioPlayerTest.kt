package vn.loi.learning.desktop.ui.studio

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopAudioPlayerTest {

    @Test
    fun `initial state of DesktopAudioPlayer is Idle`() {
        val player = DesktopAudioPlayer()
        assertEquals(AudioPlayerState.Idle, player.state)
    }

    @Test
    fun `playing missing path sets state to Error`() {
        val player = DesktopAudioPlayer()
        val missingPath = Path.of("missing_file_12345.mp3")

        player.play(missingPath)

        val state = player.state
        assertTrue(state is AudioPlayerState.Error)
        assertEquals("Missing file", state.message)
    }

    @Test
    fun `stop resets DesktopAudioPlayer state to Idle`() {
        val player = DesktopAudioPlayer()
        val missingPath = Path.of("missing_file_12345.mp3")

        player.play(missingPath)
        player.stop()

        assertEquals(AudioPlayerState.Idle, player.state)
    }
}
