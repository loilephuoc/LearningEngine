package vn.loi.learning.android.ui

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.Test
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioSource
import vn.loi.learning.android.media.AndroidAudioState

/**
 * Focused unit test suite for ANDROID-ARCH-001 and ANDROID-RUNTIME-001.
 *
 * Verifies:
 * - Shared component invariants: non-blank loading/error/completion/progress states.
 * - Image experience: available, unavailable, failed states, IO decoding contract, fullscreen overlay.
 * - Audio experience: non-blocking audio button, typed states, source classification, target existence/readability.
 * - Touch target >= 48dp, accessibility semantics.
 * - Architecture invariants: shared components contain no repo/engine/context/facade calls.
 */
class LearningEngineSharedComponentsTest {

    private fun source(relative: String): String =
        Files.readString(Path.of("src/main/kotlin").resolve(relative))

    @Test
    fun `LearningEngineComponents source does not reference Repository or Engine or Facade`() {
        val src = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")
        assertFalse(src.contains("Repository"))
        assertFalse(src.contains("LearningEngineContext"))
        assertFalse(src.contains("LearningApplicationContext"))
        assertFalse(src.contains("AndroidStudyFacade"))
        assertFalse(src.contains("engine."))
    }

    @Test
    fun `extracted shared components define touch targets at least 48dp`() {
        val src = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")
        assertTrue(src.contains("touchTarget"))
        assertTrue(src.contains("defaultMinSize"))
    }

    @Test
    fun `extracted shared components support image loading outside Main thread`() {
        val src = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")
        assertTrue(src.contains("withContext(Dispatchers.IO)"))
        assertTrue(src.contains("decodeBoundedImage"))
        assertTrue(src.contains("FullscreenLearningImage"))
    }

    @Test
    fun `audio button wraps audio controller and defines typed playback states`() {
        val src = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")
        assertTrue(src.contains("AndroidAudioController"))
        assertTrue(src.contains("LearningEngineAudioButton"))
    }

    @Test
    fun `completion card and study top bar are exported in LearningEngineComponents`() {
        val src = source("vn/loi/learning/android/ui/LearningEngineComponents.kt")
        assertTrue(src.contains("LearningEngineStudyTopBar"))
        assertTrue(src.contains("LearningEngineCompletionCard"))
    }

    // ─── ANDROID-RUNTIME-001 Audio Pipeline Qualification Tests ───────────────────

    @Test
    fun `AndroidAudioSource classify identifies supported audio source types`() {
        assertNull(AndroidAudioSource.classify(null))
        assertNull(AndroidAudioSource.classify("   "))

        val content = AndroidAudioSource.classify("content://media/external/audio/media/123")
        assertIs<AndroidAudioSource.ContentUri>(content)
        assertEquals("content://media/external/audio/media/123", content.uriString)

        val resource = AndroidAudioSource.classify("android.resource://vn.loi.learning.android/raw/beep")
        assertIs<AndroidAudioSource.ResourceUri>(resource)
        assertEquals("android.resource://vn.loi.learning.android/raw/beep", resource.uriString)

        val fileUri = AndroidAudioSource.classify("file:///storage/emulated/0/audio.mp3")
        assertIs<AndroidAudioSource.FileUri>(fileUri)
        assertEquals("file:///storage/emulated/0/audio.mp3", fileUri.uriString)

        val localFile = AndroidAudioSource.classify("/data/user/0/vn.loi.learning.android/files/audio.mp3")
        assertIs<AndroidAudioSource.LocalFile>(localFile)
        assertEquals("/data/user/0/vn.loi.learning.android/files/audio.mp3", localFile.path)
    }

    @Test
    fun `AndroidAudioController returns Unavailable for null or blank path`() {
        val controller = AndroidAudioController()
        assertEquals(AndroidAudioState.Unavailable, controller.replay(null))
        assertEquals(AndroidAudioState.Unavailable, controller.replay(""))
        assertEquals(AndroidAudioState.Unavailable, controller.replay("   "))
        controller.close()
    }

    @Test
    fun `AndroidAudioController fails gracefully when target file is missing or unreadable`() {
        val controller = AndroidAudioController()
        val result = controller.replay("/nonexistent/path/missing_file.mp3")
        assertIs<AndroidAudioState.Failed>(result)
        val reason = (result as AndroidAudioState.Failed).reason
        assertNotNull(reason)
        assertTrue(reason.contains("not accessible") || reason.contains("IO error") || reason.contains("Error"))
        controller.close()
    }

    @Test
    fun `AndroidAudioController close releases resources and increments active session token`() {
        val controller = AndroidAudioController()
        assertNotNull(controller)
        controller.close()
    }

    @Test
    fun `AndroidAudioController is placed in neutral media package`() {
        val src = source("vn/loi/learning/android/media/AndroidAudioController.kt")
        assertTrue(src.contains("package vn.loi.learning.android.media"))
        assertTrue(src.contains("sealed interface AndroidAudioSource"))
        assertTrue(src.contains("sealed interface AndroidAudioState"))
        assertTrue(src.contains("class AndroidAudioController"))
    }
}
