package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode
import vn.loi.learning.desktop.runtime.StudyPresentationPreferences

class FullAnswerPresentationTest {
    private val completeModel = FocusedVocabularyAnswerModel(
        englishWord = "disaster",
        ipa = "/dɪˈzɑːstə/",
        partOfSpeech = "NOUN",
        imagePath = Path.of("disaster.jpg"),
        vietnameseMeaning = "thảm họa",
        englishDefinition = "A sudden event causing great damage.",
        examples = listOf(
            FocusedExampleItem(
                englishText = "The earthquake was a terrible disaster.",
                vietnameseTranslation = "Trận động đất là một thảm họa lớn."
            )
        )
    )

    @Test
    fun `manual Vietnamese question still resolves every available answer field`() {
        val questionPreferences = StudyPresentationPreferences(
            controlMode = StudyPresentationControlMode.MANUAL,
            showEnglish = false,
            showVietnamese = true
        )

        val disclosure = FullAnswerPresentation.resolve(completeModel)

        assertEquals(StudyPresentationControlMode.MANUAL, questionPreferences.controlMode)
        assertComplete(disclosure)
    }

    @Test
    fun `manual English question still resolves every available answer field`() {
        val questionPreferences = StudyPresentationPreferences(
            controlMode = StudyPresentationControlMode.MANUAL,
            showEnglish = true,
            showVietnamese = false
        )

        val disclosure = FullAnswerPresentation.resolve(completeModel)

        assertEquals(StudyPresentationControlMode.MANUAL, questionPreferences.controlMode)
        assertComplete(disclosure)
    }

    @Test
    fun `adaptive and preference guided questions share the full answer contract`() {
        val modes = listOf(
            StudyPresentationControlMode.ADAPTIVE,
            StudyPresentationControlMode.PREFERENCE_GUIDED
        )

        modes.forEach { mode ->
            val preferences = StudyPresentationPreferences(controlMode = mode)
            assertEquals(mode, preferences.controlMode)
            assertComplete(FullAnswerPresentation.resolve(completeModel))
        }
    }

    @Test
    fun `full answer audio keeps every semantic path independent of Question switches`() {
        val model = completeModel.copy(
            primaryAudioPath = Path.of("primary.mp3"),
            meaningAudioPath = Path.of("meaning.mp3"),
            examples = listOf(
                completeModel.examples.single().copy(
                    englishAudioPath = Path.of("example-en.mp3"),
                    vietnameseAudioPath = Path.of("example-vi.mp3")
                ),
                completeModel.examples.single().copy(
                    key = "duplicate-path",
                    englishAudioPath = Path.of("example-en.mp3")
                )
            )
        )

        val audio = FullAnswerAudioPresentation.resolve(model)

        assertEquals(Path.of("primary.mp3"), audio.primaryEnglish)
        assertEquals(Path.of("meaning.mp3"), audio.vietnameseMeaning)
        assertEquals(listOf(Path.of("example-en.mp3")), audio.englishExamples)
        assertEquals(listOf(Path.of("example-vi.mp3")), audio.vietnameseExamples)
    }

    @Test
    fun `full answer audio missing primary remains safely silent without cross-role fallback`() {
        val audio = FullAnswerAudioPresentation.resolve(
            completeModel.copy(
                primaryAudioPath = null,
                meaningAudioPath = Path.of("meaning.mp3")
            )
        )

        assertEquals(null, audio.primaryEnglish)
        assertEquals(Path.of("meaning.mp3"), audio.vietnameseMeaning)
    }

    private fun assertComplete(disclosure: FullAnswerDisclosure) {
        assertEquals("disaster", disclosure.englishWord)
        assertEquals("/dɪˈzɑːstə/", disclosure.ipa)
        assertEquals("NOUN", disclosure.partOfSpeech)
        assertTrue(disclosure.imageAvailable)
        assertEquals("thảm họa", disclosure.vietnameseMeaning)
        assertEquals("A sudden event causing great damage.", disclosure.englishDefinition)
        assertEquals(1, disclosure.examples.size)
        assertEquals(
            "The earthquake was a terrible disaster.",
            disclosure.examples.single().englishText
        )
        assertEquals(
            "Trận động đất là một thảm họa lớn.",
            disclosure.examples.single().vietnameseTranslation
        )
    }
}
