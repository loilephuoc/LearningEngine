package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode
import vn.loi.learning.desktop.runtime.StudyPresentationPreferences

class StudyPresentationPolicyTest {
    private val allAvailable = StudyPresentationAvailability(
        primaryEnglishAvailable = true,
        vietnameseMeaningAvailable = true,
        englishExamplesAvailable = true,
        vietnameseExamplesAvailable = true,
        primaryEnglishAudio = Path.of("primary.mp3"),
        vietnameseMeaningAudio = Path.of("meaning.mp3"),
        englishExampleAudio = Path.of("example-en.mp3"),
        vietnameseExampleAudio = Path.of("example-vi.mp3")
    )
    private val allRecommended = StudyPresentationRecommendation(
        showPrimaryEnglish = true,
        allowPrimaryEnglishAudio = true,
        showVietnameseMeaning = true,
        showEnglishExamples = true,
        showVietnameseExamples = true,
        autoplayPrimaryEnglish = true,
        autoplayVietnameseMeaning = true,
        autoplayEnglishExample = true,
        autoplayVietnameseExample = true
    )

    @Test
    fun `adaptive follows recommendation and ignores stored switches`() {
        val effective = StudyPresentationPolicy.resolve(
            StudyPresentationPreferences(
                showEnglish = false,
                showVietnamese = false,
                autoplayEnglish = false,
                autoplayVietnamese = false
            ),
            allAvailable,
            allRecommended
        )

        assertTrue(effective.showPrimaryEnglish)
        assertTrue(effective.showPrimaryEnglishAudio)
        assertTrue(effective.showVietnameseMeaning)
        assertTrue(effective.showEnglishExamples)
        assertTrue(effective.showVietnameseExamples)
        assertTrue(effective.autoplayPrimaryEnglish)
        assertTrue(effective.autoplayVietnameseMeaning)
    }

    @Test
    fun `adaptive Listening can expose primary audio without English identity`() {
        val effective = StudyPresentationPolicy.resolve(
            StudyPresentationPreferences(),
            allAvailable,
            allRecommended.copy(
                showPrimaryEnglish = false,
                allowPrimaryEnglishAudio = true,
                showVietnameseMeaning = false,
                showEnglishExamples = false,
                showVietnameseExamples = false
            )
        )

        assertFalse(effective.showPrimaryEnglish)
        assertTrue(effective.showPrimaryEnglishAudio)
        assertTrue(effective.autoplayPrimaryEnglish)
        assertFalse(effective.showVietnameseMeaning)
        assertFalse(effective.showEnglishExamples)
        assertFalse(effective.showVietnameseExamples)
    }

    @Test
    fun `preference guided intersects recommendation with user autoplay and visibility`() {
        val effective = StudyPresentationPolicy.resolve(
            StudyPresentationPreferences(
                controlMode = StudyPresentationControlMode.PREFERENCE_GUIDED,
                showEnglish = true,
                showVietnamese = false,
                autoplayEnglish = false,
                autoplayVietnamese = true
            ),
            allAvailable,
            allRecommended
        )

        assertTrue(effective.showPrimaryEnglish)
        assertFalse(effective.showVietnameseMeaning)
        assertTrue(effective.showEnglishExamples)
        assertFalse(effective.showVietnameseExamples)
        assertFalse(effective.autoplayPrimaryEnglish)
        assertFalse(effective.autoplayVietnameseMeaning)
    }

    @Test
    fun `preference guided cannot enable support rejected by the current experience`() {
        val effective = StudyPresentationPolicy.resolve(
            StudyPresentationPreferences(
                controlMode = StudyPresentationControlMode.PREFERENCE_GUIDED,
                showEnglish = true,
                showVietnamese = true,
                autoplayEnglish = true,
                autoplayVietnamese = true
            ),
            allAvailable,
            allRecommended.copy(
                showPrimaryEnglish = false,
                allowPrimaryEnglishAudio = false,
                showVietnameseMeaning = false,
                showEnglishExamples = false,
                showVietnameseExamples = false
            )
        )

        assertFalse(effective.showPrimaryEnglish)
        assertFalse(effective.showPrimaryEnglishAudio)
        assertFalse(effective.showVietnameseMeaning)
        assertFalse(effective.showEnglishExamples)
        assertFalse(effective.showVietnameseExamples)
    }

    @Test
    fun `manual uses user choices without engine autoplay permission`() {
        val effective = StudyPresentationPolicy.resolve(
            StudyPresentationPreferences(
                controlMode = StudyPresentationControlMode.MANUAL,
                showEnglish = false,
                showVietnamese = true,
                autoplayEnglish = true,
                autoplayVietnamese = true
            ),
            allAvailable,
            allRecommended.copy(
                autoplayPrimaryEnglish = false,
                autoplayVietnameseMeaning = false
            )
        )

        assertFalse(effective.showPrimaryEnglish)
        assertTrue(effective.showVietnameseMeaning)
        assertFalse(effective.showEnglishExamples)
        assertTrue(effective.showVietnameseExamples)
        assertFalse(effective.autoplayPrimaryEnglish)
        assertTrue(effective.autoplayVietnameseMeaning)
    }

    @Test
    fun `unavailable support stays hidden and policy has no workspace phase input`() {
        val effective = StudyPresentationPolicy.resolve(
            StudyPresentationPreferences(controlMode = StudyPresentationControlMode.MANUAL),
            allAvailable.copy(
                vietnameseMeaningAvailable = false,
                englishExamplesAvailable = false
            ),
            allRecommended
        )

        assertTrue(effective.showPrimaryEnglish)
        assertFalse(effective.showVietnameseMeaning)
        assertFalse(effective.showEnglishExamples)
        assertTrue(effective.autoplayPrimaryEnglish)
        assertFalse(effective.autoplayVietnameseMeaning)
    }
}
