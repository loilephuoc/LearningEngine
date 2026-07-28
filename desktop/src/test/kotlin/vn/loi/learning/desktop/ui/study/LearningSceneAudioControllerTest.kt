package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LearningSceneAudioControllerTest {
    @Test
    fun `primary replay follows generated scene and scene change cancels playback`() {
        val player = RecordingPlayer()
        val controller = LearningContentAudioController(player)
        val firstAudio = Path.of("first.mp3")
        val secondAudio = Path.of("second.mp3")

        controller.bind(listeningScene(firstAudio))
        assertTrue(controller.replayPrimary())
        assertEquals(firstAudio, player.played.single())

        controller.bind(listeningScene(secondAudio))
        assertEquals(2, player.stopCount)
        assertTrue(controller.replayPrimary())
        assertEquals(listOf(firstAudio, secondAudio), player.played)
    }

    @Test
    fun `supporting audio never becomes keyboard primary`() {
        val player = RecordingPlayer()
        val context = LearningSceneContext(answerRevealed = true)
        val capabilities = SceneCapabilities(true, false, true, false)
        val scene = PromptScene(
            context,
            capabilities,
            blocks = listOf(textBlock("question")),
            supportingScenes = listOf(
                MeaningScene(
                    context,
                    capabilities,
                    listOf(PresentedLearningBlock.Audio(Path.of("answer.mp3"), "audio", "Answer audio"))
                )
            )
        )
        val controller = LearningContentAudioController(player)

        controller.bind(scene)

        assertFalse(controller.replayPrimary())
        assertTrue(player.played.isEmpty())
    }

    @Test
    fun `primary replay selects semantic role instead of block order`() {
        val player = RecordingPlayer()
        val meaning = Path.of("meaning.mp3")
        val primary = Path.of("word.mp3")
        val scene = PromptScene(
            LearningSceneContext(false),
            SceneCapabilities(true, true, true, false),
            listOf(
                PresentedLearningBlock.Audio(
                    meaning, "audio", "renamed", PresentedAudioRole.MEANING_TRANSLATION
                ),
                PresentedLearningBlock.Audio(
                    primary, "audio", "renamed", PresentedAudioRole.PRIMARY_WORD
                )
            )
        )
        val controller = LearningContentAudioController(player)

        controller.bind(scene)

        assertTrue(controller.replayPrimary())
        assertEquals(listOf(primary), player.played)
    }

    @Test
    fun `typed meaning audio never falls back as primary replay`() {
        val player = RecordingPlayer()
        val controller = LearningContentAudioController(player)
        controller.bind(
            PromptScene(
                LearningSceneContext(false),
                SceneCapabilities(true, false, true, false),
                listOf(
                    PresentedLearningBlock.Audio(
                        Path.of("meaning.mp3"),
                        "audio",
                        "renamed",
                        PresentedAudioRole.MEANING_TRANSLATION
                    )
                )
            )
        )

        assertFalse(controller.replayPrimary())
        assertTrue(player.played.isEmpty())
    }

    @Test
    fun `front play once and revealed loop share controller without changing front into loop`() {
        val player = RecordingPlayer()
        val controller = LearningContentAudioController(player)
        val primary = Path.of("word.mp3")

        controller.playOnce(primary)
        controller.playOnce(primary)
        assertEquals(null, controller.activeLoopPath)
        assertEquals(listOf(primary, primary), player.played)

        controller.toggleLoop(primary)
        assertEquals(primary, controller.activeLoopPath)
        controller.toggleLoop(primary)
        assertEquals(null, controller.activeLoopPath)
    }

    @Test
    fun `study lifecycle item change and completion clear active loop`() {
        val player = RecordingPlayer()
        val controller = LearningContentAudioController(player)
        val first = Path.of("first.mp3")
        val second = Path.of("second.mp3")

        synchronizeStudyAudio(controller, listeningScene(first), sessionCompleted = false)
        controller.startLoop(first)
        assertEquals(first, controller.activeLoopPath)

        synchronizeStudyAudio(controller, listeningScene(second), sessionCompleted = false)
        assertEquals(null, controller.activeLoopPath)
        controller.startLoop(second)

        synchronizeStudyAudio(controller, listeningScene(second), sessionCompleted = true)
        assertEquals(null, controller.activeLoopPath)
        assertTrue(player.stopCount >= 3)
    }

    private fun listeningScene(path: Path): LearningScene {
        val context = LearningSceneContext(answerRevealed = false)
        return ListeningScene(
            context = context,
            capabilities = SceneCapabilities(true, false, false, false),
            blocks = listOf(
                textBlock("question"),
                PresentedLearningBlock.Audio(path, "audio", "Pronunciation")
            )
        )
    }

    private fun textBlock(value: String) =
        PresentedLearningBlock.Text(
            SafeMarkdownDocument.plain(value),
            PresentedTextRole.PRIMARY_ENGLISH
        )

    private class RecordingPlayer : LearningContentAudioPlayer {
        override val state: LearningContentAudioState
            get() = LearningContentAudioState.Idle
        val played = mutableListOf<Path>()
        var stopCount = 0

        override fun play(path: Path) {
            played.add(path)
        }

        override fun stop() {
            stopCount++
        }

        override fun listen(listener: LearningContentAudioStateListener) =
            AutoCloseable { }

        override fun close() = Unit
    }
}
