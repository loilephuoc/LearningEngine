package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentFieldId

data class FocusedVocabularyAnswerModel(
    val englishWord: String,
    val ipa: String? = null,
    val partOfSpeech: String? = null,
    val imagePath: Path? = null,
    val primaryAudioPath: Path? = null,
    val vietnameseMeaning: String,
    val meaningAudioPath: Path? = null,
    val englishDefinition: String? = null,
    val examples: List<FocusedExampleItem> = emptyList()
)

data class FocusedExampleItem(
    val englishText: String,
    val vietnameseTranslation: String? = null,
    val audioPath: Path? = null,
    val englishAudioPath: Path? = audioPath,
    val vietnameseAudioPath: Path? = null
)

object FocusedVocabularyAnswerResolver {

    fun resolve(
        uiState: StudyUiState,
        learningScene: LearningScene? = null
    ): FocusedVocabularyAnswerModel {
        val domainContent: Content? = uiState.domainContent

        val englishWord = domainContent?.text?.primaryText
            ?: uiState.learningContent?.question?.textBlocks?.firstOrNull()?.value
            ?: uiState.contentText

        val ipaRaw = domainContent?.text?.pronunciation?.takeIf { it.isNotBlank() }
            ?: uiState.learningContent?.answer?.textBlocks?.firstOrNull { it.value.startsWith("/") || it.value.contains("IPA") }?.value
        val normalizedPronunciation = normalizePronunciation(ipaRaw)

        val posField = domainContent?.customFields?.get(ContentFieldId("partOfSpeech"))?.value
            ?: domainContent?.customFields?.get(ContentFieldId("pos"))?.value
            ?: domainContent?.metadata?.tags?.firstOrNull { it.startsWith("pos:", ignoreCase = true) }?.substringAfter("pos:")
        val partOfSpeech = (posField ?: normalizedPronunciation.partOfSpeech)
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("WORD", ignoreCase = true) }
            ?.uppercase()

        val vietnameseMeaning = domainContent?.text?.translatedText
            ?: uiState.learningContent?.answer?.textBlocks?.lastOrNull()?.value
            ?: uiState.translationText

        val englishDefinition = domainContent?.customFields?.get(ContentFieldId("definition"))?.value
            ?.trim()?.takeIf { it.isNotBlank() }

        // Resolve Image & Audio paths from scene blocks if present
        val allBlocks = (learningScene?.blocks.orEmpty() + learningScene?.supportingScenes.orEmpty().flatMap { it.blocks })
        val imagePath = allBlocks.filterIsInstance<PresentedLearningBlock.Image>().firstOrNull()?.path

        val audioBlocks = allBlocks.filterIsInstance<PresentedLearningBlock.Audio>()
        val primaryAudioPath = audioBlocks.firstOrNull {
            it.roleLabel.contains("Question", ignoreCase = true) ||
                    it.roleLabel.contains("Answer", ignoreCase = true) ||
                    it.roleLabel.contains("Primary", ignoreCase = true) ||
                    (!it.roleLabel.contains("Example", ignoreCase = true) && !it.roleLabel.contains("Meaning", ignoreCase = true) && !it.roleLabel.contains("Translation", ignoreCase = true))
        }?.path ?: audioBlocks.firstOrNull()?.path

        val meaningAudioPath = audioBlocks.firstOrNull {
            it.roleLabel.contains("Meaning", ignoreCase = true) ||
                    it.roleLabel.contains("Translation", ignoreCase = true)
        }?.path

        // Resolve Examples
        val examples = mutableListOf<FocusedExampleItem>()
        if (domainContent?.text?.exampleText != null && domainContent.text.exampleText!!.isNotBlank()) {
            val enExAudio = audioBlocks.firstOrNull {
                it.roleLabel.contains("Example", ignoreCase = true) &&
                        !it.roleLabel.contains("Translation", ignoreCase = true) &&
                        !it.roleLabel.contains("Vietnamese", ignoreCase = true)
            }?.path
            val viExAudio = audioBlocks.firstOrNull {
                it.roleLabel.contains("Example", ignoreCase = true) &&
                        (it.roleLabel.contains("Translation", ignoreCase = true) || it.roleLabel.contains("Vietnamese", ignoreCase = true))
            }?.path

            examples.add(
                FocusedExampleItem(
                    englishText = domainContent.text.exampleText!!,
                    vietnameseTranslation = domainContent.text.exampleTranslation?.takeIf { it.isNotBlank() },
                    audioPath = enExAudio,
                    englishAudioPath = enExAudio,
                    vietnameseAudioPath = viExAudio
                )
            )
        } else {
            // Extract from learningScene supporting ExampleScene
            learningScene?.supportingScenes.orEmpty()
                .filter { it.type == SceneType.EXAMPLE && it.blocks.isNotEmpty() }
                .forEach { exampleScene ->
                val textBlocks = exampleScene.blocks.filterIsInstance<PresentedLearningBlock.Text>()
                val exAudioBlocks = exampleScene.blocks.filterIsInstance<PresentedLearningBlock.Audio>()
                if (textBlocks.isNotEmpty()) {
                    val enExAudio = exAudioBlocks.firstOrNull {
                        !it.roleLabel.contains("Translation", ignoreCase = true) && !it.roleLabel.contains("Vietnamese", ignoreCase = true)
                    }?.path ?: exAudioBlocks.firstOrNull()?.path
                    val viExAudio = exAudioBlocks.firstOrNull {
                        it.roleLabel.contains("Translation", ignoreCase = true) || it.roleLabel.contains("Vietnamese", ignoreCase = true)
                    }?.path

                    examples.add(
                        FocusedExampleItem(
                            englishText = textBlocks.first().document.blocks.firstOrNull()?.text ?: "",
                            vietnameseTranslation = textBlocks.getOrNull(1)?.document?.blocks?.firstOrNull()?.text,
                            audioPath = enExAudio,
                            englishAudioPath = enExAudio,
                            vietnameseAudioPath = viExAudio
                        )
                    )
                }
            }
        }

        return FocusedVocabularyAnswerModel(
            englishWord = englishWord,
            ipa = normalizedPronunciation.ipa,
            partOfSpeech = partOfSpeech,
            imagePath = imagePath,
            primaryAudioPath = primaryAudioPath,
            vietnameseMeaning = vietnameseMeaning,
            meaningAudioPath = meaningAudioPath,
            englishDefinition = englishDefinition,
            examples = examples
        )
    }
}

internal data class NormalizedPronunciation(
    val ipa: String?,
    val partOfSpeech: String?
)

internal fun normalizePronunciation(raw: String?): NormalizedPronunciation {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return NormalizedPronunciation(null, null)

    val partOfSpeech = Regex("""\(\s*([A-Za-z][A-Za-z -]*)\s*\)""")
        .find(value)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?.uppercase()
    val withoutPartOfSpeech = value.replace(Regex("""/?\(\s*[A-Za-z][A-Za-z -]*\s*\)/?"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    val phonemes = withoutPartOfSpeech.trim('/').trim()
    val ipa = phonemes.takeIf(String::isNotBlank)?.let { "/$it/" }
    return NormalizedPronunciation(ipa, partOfSpeech)
}
