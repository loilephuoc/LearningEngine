package vn.loi.learning.desktop.ui.study

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

data class FullAnswerDisclosure(
    val englishWord: String,
    val ipa: String?,
    val partOfSpeech: String?,
    val imageAvailable: Boolean,
    val vietnameseMeaning: String,
    val englishDefinition: String?,
    val examples: List<FocusedExampleItem>
)
