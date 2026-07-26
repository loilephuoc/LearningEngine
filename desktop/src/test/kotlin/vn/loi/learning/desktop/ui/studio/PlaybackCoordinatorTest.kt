package vn.loi.learning.desktop.ui.studio

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage

class PlaybackCoordinatorTest {

    private class FakeMediaStorage(
        private val validPaths: Map<String, Path> = emptyMap()
    ) : ContentMediaStorage {
        override fun store(packageName: String, fileName: String, content: ByteArray): ContentMediaAsset {
            throw UnsupportedOperationException()
        }

        override fun resolve(relativePath: String): Path? {
            return validPaths[relativePath]
        }

        override fun exists(relativePath: String): Boolean {
            return validPaths.containsKey(relativePath)
        }
    }

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun `audio button state is Unavailable for blank or missing audioRef`() {
        val storage = FakeMediaStorage()
        val coordinator = PlaybackCoordinator(storage, testScope)

        assertEquals(AudioButtonState.Unavailable, coordinator.getButtonState(null))
        assertEquals(AudioButtonState.Unavailable, coordinator.getButtonState(""))
        assertEquals(AudioButtonState.Unavailable, coordinator.getButtonState("non_existent.mp3"))
    }

    @Test
    fun `playing invalid audio ref sets status to Error Cannot play audio`() {
        val storage = FakeMediaStorage()
        val coordinator = PlaybackCoordinator(storage, testScope)

        coordinator.play("invalid.mp3")

        val status = coordinator.status
        assertTrue(status is PlaybackStatus.Error, "Status should be Error for invalid path")
        assertEquals("Cannot play audio", status.message)
        assertEquals(AudioButtonState.Error("Cannot play audio"), coordinator.getButtonState("invalid.mp3"))
    }

    @Test
    fun `stop resets status to Idle`() {
        val storage = FakeMediaStorage()
        val coordinator = PlaybackCoordinator(storage, testScope)

        coordinator.play("invalid.mp3")
        coordinator.stop()

        assertEquals(PlaybackStatus.Idle, coordinator.status)
    }
}
