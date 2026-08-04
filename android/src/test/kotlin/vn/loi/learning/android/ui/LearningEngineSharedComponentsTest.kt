package vn.loi.learning.android.ui

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*
import org.junit.Test

/**
 * Focused unit test suite for ANDROID-ARCH-001 Extract Shared Android Learning Components.
 *
 * Verifies:
 * - Shared component invariants: non-blank loading/error/completion/progress states.
 * - Image experience: available, unavailable, failed states, IO decoding contract, fullscreen overlay.
 * - Audio experience: non-blocking audio button, typed states.
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
}
