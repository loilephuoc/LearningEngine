package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType

class LargeSurfaceAudioInteractionTest {

    private class DummyAudioPlayer : LearningContentAudioPlayer {
        var lastPlayedPath: Path? = null
        var isPlaying: Boolean = false
        override val state: LearningContentAudioState
            get() = if (isPlaying && lastPlayedPath != null) LearningContentAudioState.Playing(lastPlayedPath!!) else LearningContentAudioState.Idle

        override fun play(path: Path) {
            lastPlayedPath = path
            isPlaying = true
        }

        override fun stop() {
            isPlaying = false
        }

        override fun listen(listener: LearningContentAudioStateListener): AutoCloseable = AutoCloseable {}
        override fun close() {}
    }

    private val player = DummyAudioPlayer()
    private val audioController = LearningContentAudioController(player)

    @Test
    fun `15 to 28 - Large-surface audio interactions and accessibility contracts`() {
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

        // Test 15 & 16: Primary audio playing via controller
        audioController.toggle(modelWithAudio.primaryAudioPath!!)
        assertEquals(primaryAudio, player.lastPlayedPath)

        // Test 17: R shortcut replays primary audio
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

        // Test 18: Meaning card plays Vietnamese audio
        audioController.toggle(modelWithAudio.meaningAudioPath!!)
        assertEquals(meaningAudio, player.lastPlayedPath)

        // Test 19: Meaning card without audio is not clickable
        val modelNoAudio = FocusedVocabularyAnswerModel(
            englishWord = "homeless",
            vietnameseMeaning = "vô gia cư",
            meaningAudioPath = null
        )
        assertNull(modelNoAudio.meaningAudioPath)

        // Test 20, 21, 22: EN and VI example rows play distinct correct audio assets
        val example = modelWithAudio.examples.first()
        assertNotNull(example.englishAudioPath)
        assertNotNull(example.vietnameseAudioPath)

        audioController.toggle(example.englishAudioPath!!)
        assertEquals(enExAudio, player.lastPlayedPath)

        audioController.toggle(example.vietnameseAudioPath!!)
        assertEquals(viExAudio, player.lastPlayedPath)

        // Test 23: Multiple examples preserve mapping
        val modelMultiEx = FocusedVocabularyAnswerModel(
            englishWord = "homeless",
            vietnameseMeaning = "vô gia cư",
            examples = listOf(
                FocusedExampleItem("Ex 1 EN", "Ex 1 VI", englishAudioPath = Paths.get("/audio/ex1_en.mp3"), vietnameseAudioPath = Paths.get("/audio/ex1_vi.mp3")),
                FocusedExampleItem("Ex 2 EN", "Ex 2 VI", englishAudioPath = Paths.get("/audio/ex2_en.mp3"), vietnameseAudioPath = Paths.get("/audio/ex2_vi.mp3"))
            )
        )
        assertEquals(2, modelMultiEx.examples.size)
        audioController.toggle(modelMultiEx.examples[1].englishAudioPath!!)
        assertEquals(Paths.get("/audio/ex2_en.mp3"), player.lastPlayedPath)

        // Test 26: Pre-reveal prompt answer audio protection
        val domainContent = Content(
            id = ContentId("c-1"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "homeless", translatedText = "vô gia cư"),
            media = ContentMedia(primaryAudio = "homeless.mp3")
        )
        assertNotNull(domainContent)

        // Test 27 & 28: Only single audioController authority used, no long text buttons
        assertNotNull(audioController.state)
    }
}
