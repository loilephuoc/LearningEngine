package vn.loi.learning.desktop.ui.study

import java.nio.file.Path

/**
 * Answer disclosure is derived only from available content. Learner presentation preferences
 * apply to the question surface and must never remove fields from a revealed answer.
 */
object FullAnswerPresentation {
    fun resolve(model: FocusedVocabularyAnswerModel): FullAnswerDisclosure =
        FullAnswerDisclosure(
            englishWord = model.englishWord,
            ipa = model.ipa,
            partOfSpeech = model.partOfSpeech,
            imageAvailable = model.imagePath != null,
            vietnameseMeaning = model.vietnameseMeaning,
            englishDefinition = model.englishDefinition,
            examples = model.examples
        )
}

object FullAnswerAudioPresentation {
    fun resolve(model: FocusedVocabularyAnswerModel): FullAnswerAudio =
        FullAnswerAudio(
            primaryEnglish = model.primaryAudioPath,
            vietnameseMeaning = model.meaningAudioPath,
            englishExamples = model.examples.mapNotNull { it.englishAudioPath ?: it.audioPath }.distinct(),
            vietnameseExamples = model.examples.mapNotNull { it.vietnameseAudioPath }.distinct()
        )
}

data class FullAnswerAudio(
    val primaryEnglish: Path?,
    val vietnameseMeaning: Path?,
    val englishExamples: List<Path>,
    val vietnameseExamples: List<Path>
)

data class FullAnswerDisclosure(
    val englishWord: String,
    val ipa: String?,
    val partOfSpeech: String?,
    val imageAvailable: Boolean,
    val vietnameseMeaning: String,
    val englishDefinition: String?,
    val examples: List<FocusedExampleItem>
)
