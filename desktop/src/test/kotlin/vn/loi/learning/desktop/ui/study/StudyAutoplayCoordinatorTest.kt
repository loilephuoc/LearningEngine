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
        englishExamplesAvailable = false,
        vietnameseExamplesAvailable = false,
        primaryEnglishAudio = Path.of("primary.mp3"),
        vietnameseMeaningAudio = Path.of("meaning.mp3")
    )
    private val effective = EffectiveStudyPresentation(
        showPrimaryEnglish = true,
        showVietnameseMeaning = true,
        showEnglishExamples = false,
        showVietnameseExamples = false,
        autoplayPrimaryEnglish = true,
        autoplayVietnameseMeaning = true,
        autoplayEnglishExample = false,
        autoplayVietnameseExample = false
    )

    @Test
    fun `one transition autoplays once and apply does not replay current item`() {
        val coordinator = StudyAutoplayCoordinator()
        val transition = StudyAutoplayTransition("item-1", answerRevealed = true)

        assertEquals(
            Path.of("primary.mp3"),
            coordinator.nextAutoplay(transition, availability, effective)
        )
        assertNull(
            coordinator.nextAutoplay(
                transition,
                availability,
                effective.copy(autoplayPrimaryEnglish = false)
            )
        )
        assertEquals(
            Path.of("primary.mp3"),
            coordinator.nextAutoplay(transition.copy(itemId = "item-2"), availability, effective)
        )
    }

    @Test
    fun `hidden loop paths include only support that became invisible`() {
        assertEquals(
            setOf(Path.of("meaning.mp3")),
            hiddenLoopPaths(
                availability,
                effective.copy(showVietnameseMeaning = false)
            )
        )
        assertTrue(hiddenLoopPaths(availability, effective).isEmpty())
    }

    @Test
    fun `manual question transition autoplays visible Vietnamese without waiting for reveal`() {
        val coordinator = StudyAutoplayCoordinator()

        assertEquals(
            Path.of("meaning.mp3"),
            coordinator.nextAutoplay(
                StudyAutoplayTransition("item-1", answerRevealed = false),
                availability,
                effective.copy(
                    controlMode = StudyPresentationControlMode.MANUAL,
                    showPrimaryEnglish = false,
                    autoplayPrimaryEnglish = false
                )
            )
        )
    }

    @Test
    fun `adaptive question keeps baseline silent and reveal autoplays English`() {
        val coordinator = StudyAutoplayCoordinator()

        assertNull(
            coordinator.nextAutoplay(
                StudyAutoplayTransition("item-1", answerRevealed = false),
                availability,
                effective
            )
        )
        assertEquals(
            Path.of("primary.mp3"),
            coordinator.nextAutoplay(
                StudyAutoplayTransition("item-1", answerRevealed = true),
                availability,
                effective
            )
        )
    }

    @Test
    fun `question transition cannot autoplay audio absent from current scene`() {
        val scene = PromptScene(
            LearningSceneContext(answerRevealed = false),
            SceneCapabilities(
                hasAudio = true,
                hasImage = false,
                hasMeaning = true,
                hasExamples = false
            ),
            blocks = listOf(
                PresentedLearningBlock.Audio(
                    Path.of("primary.mp3"),
                    "English",
                    "English",
                    PresentedAudioRole.PRIMARY_WORD
                )
            )
        )

        val questionAvailability = questionTransitionAvailability(availability, scene)

        assertEquals(Path.of("primary.mp3"), questionAvailability.primaryEnglishAudio)
        assertNull(questionAvailability.vietnameseMeaningAudio)
    }
}
