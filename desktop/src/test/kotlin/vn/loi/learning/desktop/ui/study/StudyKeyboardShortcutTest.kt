package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentBlock
import vn.loi.learning.application.learningcontent.LearningContentSection
import vn.loi.learning.application.learningcontent.LocalLearningAssetReference
import vn.loi.learning.desktop.shortcut.DesktopKeyChord
import vn.loi.learning.desktop.shortcut.DesktopShortcutKey
import vn.loi.learning.desktop.shortcut.ShortcutChangeResult
import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand
import vn.loi.learning.domain.content.model.ContentTextFormat

class StudyKeyboardShortcutTest {
    private val defaults = ShortcutRegistry.defaults()

    @Test
    fun `default reveal chord follows workspace state without changing study behavior`() {
        val space = StudyKeyboardInput(DesktopKeyChord(DesktopShortcutKey.SPACE))
        assertEquals(StudyKeyboardAction.START_STUDY, resolveStudyKeyboardAction(StudyUiState(), space, defaults))
        assertEquals(
            StudyKeyboardAction.RETRY_LOAD,
            resolveStudyKeyboardAction(
                StudyUiState(loadError = "retry"),
                space,
                defaults
            )
        )
        assertEquals(
            StudyKeyboardAction.REVEAL_ANSWER,
            resolveStudyKeyboardAction(
                StudyUiState(hasActiveSession = true, canRevealAnswer = true),
                space,
                defaults
            )
        )
        assertEquals(
            StudyKeyboardAction.REVIEW_GOOD,
            resolveStudyKeyboardAction(
                StudyUiState(hasActiveSession = true, canReview = true),
                space,
                defaults
            )
        )
        assertNull(
            resolveStudyKeyboardAction(
                StudyUiState(hasActiveSession = true, canReview = true),
                space.copy(repeated = true),
                defaults
            )
        )
        assertNull(
            resolveStudyKeyboardAction(
                StudyUiState(hasActiveSession = true, canReview = true),
                space.copy(textInputFocused = true),
                defaults
            )
        )
    }

    @Test
    fun `configured reveal chord routes while old default stops routing`() {
        val changed = defaults.requestChange(
            StudyShortcutCommand.REVEAL_ANSWER,
            DesktopKeyChord(DesktopShortcutKey.ENTER)
        ) as ShortcutChangeResult.Changed
        val state = StudyUiState(hasActiveSession = true, canRevealAnswer = true)

        assertEquals(
            StudyKeyboardAction.REVEAL_ANSWER,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardInput(DesktopKeyChord(DesktopShortcutKey.ENTER)),
                changed.registry
            )
        )
        assertNull(
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardInput(DesktopKeyChord(DesktopShortcutKey.SPACE)),
                changed.registry
            )
        )
    }

    @Test
    fun `rating commands route only after answer is revealed`() {
        val revealed = StudyUiState(hasActiveSession = true, canReview = true)
        val hidden = StudyUiState(hasActiveSession = true, canRevealAnswer = true)
        val expectations = listOf(
            DesktopShortcutKey.ONE to StudyKeyboardAction.REVIEW_AGAIN,
            DesktopShortcutKey.TWO to StudyKeyboardAction.REVIEW_HARD,
            DesktopShortcutKey.THREE to StudyKeyboardAction.REVIEW_GOOD,
            DesktopShortcutKey.FOUR to StudyKeyboardAction.REVIEW_EASY
        )

        expectations.forEach { (key, action) ->
            val input = StudyKeyboardInput(DesktopKeyChord(key))
            assertEquals(action, resolveStudyKeyboardAction(revealed, input, defaults))
            assertNull(resolveStudyKeyboardAction(hidden, input, defaults))
        }
    }

    @Test
    fun `undo pause and replay retain availability rules`() {
        val ctrlZ = StudyKeyboardInput(
            DesktopKeyChord(DesktopShortcutKey.Z, controlPressed = true)
        )
        val escape = StudyKeyboardInput(DesktopKeyChord(DesktopShortcutKey.ESCAPE))
        val replay = StudyKeyboardInput(DesktopKeyChord(DesktopShortcutKey.R))
        val state = StudyUiState(
            hasActiveSession = true,
            canRevealAnswer = true,
            canUndo = true,
            learningContent = contentWithAudio()
        )

        assertEquals(StudyKeyboardAction.UNDO_LATEST, resolveStudyKeyboardAction(state, ctrlZ, defaults))
        assertEquals(StudyKeyboardAction.PAUSE_WORKSPACE, resolveStudyKeyboardAction(state, escape, defaults))
        assertEquals(StudyKeyboardAction.REPLAY_PRIMARY_AUDIO, resolveStudyKeyboardAction(state, replay, defaults))
        assertNull(resolveStudyKeyboardAction(state.copy(canUndo = false), ctrlZ, defaults))
        assertNull(resolveStudyKeyboardAction(state.copy(learningContent = null), replay, defaults))
        assertNull(resolveStudyKeyboardAction(StudyUiState(), escape, defaults))
    }

    @Test
    fun `busy repeated and text input shortcuts are suppressed except active pause`() {
        val state = StudyUiState(hasActiveSession = true, canReview = true)
        val good = StudyKeyboardInput(DesktopKeyChord(DesktopShortcutKey.THREE))
        assertNull(resolveStudyKeyboardAction(state.copy(actionInProgress = true), good, defaults))
        assertNull(resolveStudyKeyboardAction(state, good.copy(repeated = true), defaults))
        assertNull(resolveStudyKeyboardAction(state, good.copy(textInputFocused = true), defaults))
        assertEquals(
            StudyKeyboardAction.PAUSE_WORKSPACE,
            resolveStudyKeyboardAction(
                state,
                StudyKeyboardInput(
                    DesktopKeyChord(DesktopShortcutKey.ESCAPE),
                    textInputFocused = true
                ),
                defaults
            )
        )
    }

    @Test
    fun `new audio chords route as distinct actions and respect input guards`() {
        val state = StudyUiState(hasActiveSession = true, canRevealAnswer = true)
        val expectations = listOf(
            DesktopKeyChord(DesktopShortcutKey.L) to StudyKeyboardAction.TOGGLE_VOCABULARY_AUDIO_LOOP,
            DesktopKeyChord(DesktopShortcutKey.L, shiftPressed = true) to
                StudyKeyboardAction.TOGGLE_EXAMPLE_AUDIO_LOOP,
            DesktopKeyChord(DesktopShortcutKey.V) to StudyKeyboardAction.PLAY_VIETNAMESE_MEANING_AUDIO,
            DesktopKeyChord(DesktopShortcutKey.V, shiftPressed = true) to
                StudyKeyboardAction.PLAY_VIETNAMESE_EXAMPLE_AUDIO
        )
        expectations.forEach { (chord, action) ->
            assertEquals(action, resolveStudyKeyboardAction(state, StudyKeyboardInput(chord), defaults))
            assertNull(
                resolveStudyKeyboardAction(
                    state,
                    StudyKeyboardInput(chord, textInputFocused = true),
                    defaults
                )
            )
        }
    }

    @Test
    fun `changed and reset audio bindings update dispatcher from the same registry snapshot`() {
        val state = StudyUiState(hasActiveSession = true, canRevealAnswer = true)
        val cases =
            listOf(
                Triple(
                    StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP,
                    DesktopKeyChord(DesktopShortcutKey.M, altPressed = true),
                    StudyKeyboardAction.TOGGLE_VOCABULARY_AUDIO_LOOP
                ),
                Triple(
                    StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP,
                    DesktopKeyChord(DesktopShortcutKey.N, altPressed = true),
                    StudyKeyboardAction.TOGGLE_EXAMPLE_AUDIO_LOOP
                ),
                Triple(
                    StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO,
                    DesktopKeyChord(DesktopShortcutKey.B, altPressed = true),
                    StudyKeyboardAction.PLAY_VIETNAMESE_MEANING_AUDIO
                ),
                Triple(
                    StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO,
                    DesktopKeyChord(DesktopShortcutKey.C, altPressed = true),
                    StudyKeyboardAction.PLAY_VIETNAMESE_EXAMPLE_AUDIO
                )
            )

        cases.forEach { (command, changedChord, expectedAction) ->
            val originalChord = defaults.chordFor(command)
            val changed =
                (defaults.requestChange(command, changedChord) as ShortcutChangeResult.Changed)
                    .registry
            assertEquals(
                expectedAction,
                resolveStudyKeyboardAction(state, StudyKeyboardInput(changedChord), changed)
            )
            assertNull(resolveStudyKeyboardAction(state, StudyKeyboardInput(originalChord), changed))

            val reset =
                (changed.requestChange(command, originalChord) as ShortcutChangeResult.Changed)
                    .registry
            assertEquals(
                expectedAction,
                resolveStudyKeyboardAction(state, StudyKeyboardInput(originalChord), reset)
            )
            assertNull(resolveStudyKeyboardAction(state, StudyKeyboardInput(changedChord), reset))
        }
    }

    private fun contentWithAudio(): LearningContent =
        LearningContent(
            question = LearningContentSection(
                listOf(
                    LearningContentBlock.Text(
                        "Question",
                        ContentTextFormat.PLAIN_TEXT,
                        vn.loi.learning.application.learningcontent.LearningTextRole.PRIMARY_ENGLISH
                    ),
                    LearningContentBlock.Audio(
                        requireNotNull(LocalLearningAssetReference.from("audio/prompt.mp3"))
                    )
                )
            ),
            answer = LearningContentSection(listOf(LearningContentBlock.UnavailableAnswer))
        )
}
