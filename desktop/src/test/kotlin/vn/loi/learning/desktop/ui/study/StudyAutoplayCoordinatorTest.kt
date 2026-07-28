package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode

class StudyAutoplayCoordinatorTest {
    private val availability = StudyPresentationAvailability(
        primaryEnglishAvailable = true,
        vietnameseMeaningAvailable = true,
        englishExamplesAvailable = true,
        vietnameseExamplesAvailable = true,
        primaryEnglishAudio = Path.of("primary.mp3"),
        vietnameseMeaningAudio = Path.of("meaning.mp3"),
        englishExampleAudio = Path.of("example-en.mp3"),
        vietnameseExampleAudio = Path.of("example-vi.mp3")
    )
    private val effective = EffectiveStudyPresentation(
        showPrimaryEnglish = false,
        showPrimaryEnglishAudio = true,
        showVietnameseMeaning = false,
        showEnglishExamples = false,
        showVietnameseExamples = false,
        autoplayPrimaryEnglish = true,
        autoplayVietnameseMeaning = false,
        autoplayEnglishExample = false,
        autoplayVietnameseExample = false
    )
    private val fullAnswerAudio = FullAnswerAudio(
        primaryEnglish = Path.of("primary.mp3"),
        vietnameseMeaning = Path.of("meaning.mp3"),
        englishExamples = listOf(Path.of("example-en.mp3")),
        vietnameseExamples = listOf(Path.of("example-vi.mp3"))
    )

    @Test
    fun `live reveal autoplays full answer primary English exactly once`() {
        val coordinator = StudyAutoplayCoordinator()
        val question = transition("item-1", StudyAutoplayPhase.QUESTION_BOUND)
        val reveal = transition("item-1", StudyAutoplayPhase.ANSWER_REVEALED)

        assertEquals(
            Path.of("primary.mp3"),
            coordinator.nextAutoplay(question, availability, effective, fullAnswerAudio)
        )
        assertEquals(
            Path.of("primary.mp3"),
            coordinator.nextAutoplay(
                reveal,
                availability,
                effective.copy(
                    autoplayPrimaryEnglish = false,
                    autoplayVietnameseMeaning = true
                ),
                fullAnswerAudio
            )
        )
        assertNull(coordinator.nextAutoplay(reveal, availability, effective, fullAnswerAudio))
    }

    @Test
    fun `answer reveal ignores Vietnamese question autoplay and has no blind fallback`() {
        val coordinator = StudyAutoplayCoordinator()
        coordinator.nextAutoplay(
            transition("item-1", StudyAutoplayPhase.QUESTION_BOUND),
            availability,
            effective.copy(
                controlMode = StudyPresentationControlMode.MANUAL,
                autoplayPrimaryEnglish = false,
                autoplayVietnameseMeaning = true
            ),
            fullAnswerAudio
        )

        assertNull(
            coordinator.nextAutoplay(
                transition("item-1", StudyAutoplayPhase.ANSWER_REVEALED),
                availability,
                effective,
                fullAnswerAudio.copy(primaryEnglish = null)
            )
        )
    }

    @Test
    fun `recovered revealed item and stale prior item do not autoplay`() {
        val recovered = StudyAutoplayCoordinator()
        assertNull(
            recovered.nextAutoplay(
                transition("item-1", StudyAutoplayPhase.ANSWER_REVEALED),
                availability,
                effective,
                fullAnswerAudio
            )
        )

        val advanced = StudyAutoplayCoordinator()
        advanced.nextAutoplay(
            transition("item-1", StudyAutoplayPhase.QUESTION_BOUND),
            availability,
            effective,
            fullAnswerAudio
        )
        assertNull(
            advanced.nextAutoplay(
                transition("item-2", StudyAutoplayPhase.ANSWER_REVEALED),
                availability,
                effective,
                fullAnswerAudio
            )
        )
    }

    @Test
    fun `same transition models recomposition resize and apply without replay`() {
        val coordinator = StudyAutoplayCoordinator()
        val question = transition("item-1", StudyAutoplayPhase.QUESTION_BOUND)

        coordinator.nextAutoplay(question, availability, effective, fullAnswerAudio)
        assertNull(
            coordinator.nextAutoplay(
                question,
                availability,
                effective.copy(autoplayPrimaryEnglish = false),
                fullAnswerAudio
            )
        )
    }

    @Test
    fun `manual question may autoplay visible Vietnamese while adaptive listening uses primary`() {
        val manual = StudyAutoplayCoordinator()
        assertEquals(
            Path.of("meaning.mp3"),
            manual.nextAutoplay(
                transition("manual", StudyAutoplayPhase.QUESTION_BOUND),
                availability,
                effective.copy(
                    controlMode = StudyPresentationControlMode.MANUAL,
                    autoplayPrimaryEnglish = false,
                    autoplayVietnameseMeaning = true
                ),
                fullAnswerAudio
            )
        )

        val adaptive = StudyAutoplayCoordinator()
        assertEquals(
            Path.of("primary.mp3"),
            adaptive.nextAutoplay(
                transition("adaptive", StudyAutoplayPhase.QUESTION_BOUND),
                availability,
                effective,
                fullAnswerAudio
            )
        )
    }

    @Test
    fun `question availability excludes semantically hidden answer media`() {
        val scene = PromptScene(
            LearningSceneContext(answerRevealed = false),
            SceneCapabilities(true, false, true, false),
            blocks = listOf(
                PresentedLearningBlock.Audio(
                    Path.of("primary.mp3"),
                    "English",
                    "English",
                    PresentedAudioRole.PRIMARY_WORD
                ),
                PresentedLearningBlock.Audio(
                    Path.of("meaning.mp3"),
                    "Vietnamese",
                    "Vietnamese",
                    PresentedAudioRole.MEANING_TRANSLATION
                )
            )
        )

        val questionAvailability =
            questionTransitionAvailability(availability, scene, effective)

        assertEquals(Path.of("primary.mp3"), questionAvailability.primaryEnglishAudio)
        assertNull(questionAvailability.vietnameseMeaningAudio)
    }

    @Test
    fun `hidden loop paths remain a Question-only visibility projection`() {
        assertEquals(
            setOf(
                Path.of("meaning.mp3"),
                Path.of("example-en.mp3"),
                Path.of("example-vi.mp3")
            ),
            hiddenLoopPaths(availability, effective)
        )
        assertTrue(
            hiddenLoopPaths(
                availability,
                effective.copy(
                    showVietnameseMeaning = true,
                    showEnglishExamples = true,
                    showVietnameseExamples = true
                )
            ).isEmpty()
        )
    }

    private fun transition(itemId: String, phase: StudyAutoplayPhase) =
        StudyAutoplayTransition(itemId, phase)
}
