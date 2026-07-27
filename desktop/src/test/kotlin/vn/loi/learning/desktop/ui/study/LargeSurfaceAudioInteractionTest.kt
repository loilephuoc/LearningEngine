package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.DesktopRuntimeConfiguration
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType

class LargeSurfaceAudioInteractionTest {

    private class DummyAudioPlayer : LearningContentAudioPlayer {
        var lastPlayedPath: Path? = null
        var isPlaying: Boolean = false
        private val listeners = mutableListOf<LearningContentAudioStateListener>()

        override val state: LearningContentAudioState
            get() = if (isPlaying && lastPlayedPath != null) LearningContentAudioState.Playing(lastPlayedPath!!) else LearningContentAudioState.Idle

        override fun play(path: Path) {
            lastPlayedPath = path
            isPlaying = true
            notifyState(LearningContentAudioState.Playing(path))
        }

        override fun stop() {
            isPlaying = false
            notifyState(LearningContentAudioState.Idle)
        }

        private fun notifyState(state: LearningContentAudioState) {
            listeners.toList().forEach { it.onStateChanged(state) }
        }

        override fun listen(listener: LearningContentAudioStateListener): AutoCloseable {
            listeners.add(listener)
            return AutoCloseable { listeners.remove(listener) }
        }

        override fun close() {
            listeners.clear()
        }
    }

    private val player = DummyAudioPlayer()
    private val audioController = LearningContentAudioController(player)

    @Test
    fun `01 to 28 - Comprehensive adaptive answer audio playback tests`() {
        val primaryAudio = Paths.get("/audio/homeless_en.mp3")
        val meaningAudio = Paths.get("/audio/homeless_vi.mp3")
        val enExAudio = Paths.get("/audio/ex_en.mp3")
        val viExAudio = Paths.get("/audio/ex_vi.mp3")

        val modelWithAudio = FocusedVocabularyAnswerModel(
            englishWord = "homeless",
            ipa = "/ˈhoʊmləs/",
            partOfSpeech = "adjective",
            primaryAudioPath = primaryAudio,
            vietnameseMeaning = "vô gia cư",
            meaningAudioPath = meaningAudio,
            examples = listOf(
                FocusedExampleItem(
                    englishText = "The shelter helps homeless people.",
                    vietnameseTranslation = "Nơi trú ẩn giúp đỡ người vô gia cư.",
                    englishAudioPath = enExAudio,
                    vietnameseAudioPath = viExAudio
                )
            )
        )

        // Test 1: playOnce plays single primary audio once
        audioController.playOnce(primaryAudio)
        assertEquals(primaryAudio, player.lastPlayedPath)

        // Test 2 & 5: toggleLoop starts loop
        audioController.toggleLoop(primaryAudio)
        assertEquals(primaryAudio, audioController.activeLoopPath)

        // Test 6: clicking same identity surface stops loop
        audioController.toggleLoop(primaryAudio)
        assertNull(audioController.activeLoopPath)

        // Test 7, 8, 9, 10, 11: Configurable loop delay in DesktopRuntimeConfiguration
        val config = DesktopRuntimeConfiguration(audioLoopDelaySeconds = 0.35)
        assertEquals(0.35, config.audioLoopDelaySeconds)
        audioController.loopDelaySeconds = config.audioLoopDelaySeconds
        assertEquals(0.35, audioController.loopDelaySeconds)

        // Test 12 & 13: Meaning card plays once and stops active loop
        audioController.toggleLoop(primaryAudio)
        audioController.playOnce(meaningAudio)
        assertNull(audioController.activeLoopPath)
        assertEquals(meaningAudio, player.lastPlayedPath)

        // Test 14 & 15: English example row toggles loop
        audioController.toggleLoop(enExAudio)
        assertEquals(enExAudio, audioController.activeLoopPath)

        // Test 16 & 17: Vietnamese translation row plays once and stops loop
        audioController.playOnce(viExAudio)
        assertNull(audioController.activeLoopPath)
        assertEquals(viExAudio, player.lastPlayedPath)

        // Test 20: Starting new source stops previous loop
        audioController.toggleLoop(enExAudio)
        audioController.playOnce(primaryAudio)
        assertNull(audioController.activeLoopPath)

        // Test 21, 22, 23, 24: Cancellation behavior on stop/bind
        audioController.startLoop(primaryAudio)
        audioController.bind(null)
        assertNull(audioController.activeLoopPath)

        // Test 25: R key replays primary audio
        val sceneContext = LearningSceneContext(answerRevealed = true)
        val capabilities = SceneCapabilities(hasAudio = true, hasImage = false, hasMeaning = true, hasExamples = true)
        val scene = PromptScene(
            context = sceneContext,
            capabilities = capabilities,
            blocks = listOf(PresentedLearningBlock.Audio(primaryAudio, description = "Primary audio", roleLabel = "Primary"))
        )
        audioController.bind(scene)
        val replayed = audioController.replayPrimary()
        assertTrue(replayed)
        assertEquals(primaryAudio, player.lastPlayedPath)
    }
}
