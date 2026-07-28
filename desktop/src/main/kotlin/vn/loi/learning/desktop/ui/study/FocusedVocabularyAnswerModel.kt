package vn.loi.learning.desktop.ui.study

import java.nio.file.Path
import vn.loi.learning.application.partofspeech.PartOfSpeechExtractor
import vn.loi.learning.application.partofspeech.PartOfSpeechNormalizer
import vn.loi.learning.application.partofspeech.normalizePronunciation
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
    val key: String = "example-0",
    val englishText: String,
    val vietnameseTranslation: String? = null,
    val audioPath: Path? = null,
    val englishAudioPath: Path? = audioPath,
    val vietnameseAudioPath: Path? = null
)

object FocusedVocabularyAnswerResolver {

    fun resolve(
        uiState: StudyUiState,
        learningScene: LearningScene? = null,
        completePresentation: LearningContentPresentation? = null
    ): FocusedVocabularyAnswerModel {
        val domainContent: Content? = uiState.domainContent

        val englishWord = domainContent?.text?.primaryText
            ?: uiState.learningContent?.question?.textBlocks?.firstOrNull()?.value
            ?: uiState.contentText

        val ipaRaw = domainContent?.text?.pronunciation?.takeIf { it.isNotBlank() }
            ?: uiState.learningContent?.answer?.textBlocks?.firstOrNull { it.value.startsWith("/") || it.value.contains("IPA") }?.value
        val normalizedPronunciation = normalizePronunciation(ipaRaw)

        val partOfSpeech = (
            domainContent?.let(PartOfSpeechExtractor::primary)
                ?: PartOfSpeechNormalizer.canonicalize(normalizedPronunciation.partOfSpeech)
            )?.value

        val vietnameseMeaning = domainContent?.text?.translatedText
            ?: uiState.learningContent?.answer?.textBlocks?.lastOrNull()?.value
            ?: uiState.translationText

        val englishDefinition = domainContent?.customFields?.get(ContentFieldId("definition"))?.value
            ?.trim()?.takeIf { it.isNotBlank() }

        // Resolve Image & Audio paths from scene blocks if present
        val completeBlocks = completePresentation?.sections.orEmpty().flatMap { it.blocks }
        val sceneBlocks =
            learningScene?.blocks.orEmpty() +
                learningScene?.supportingScenes.orEmpty().flatMap { it.blocks }
        val allBlocks = (completeBlocks + sceneBlocks).distinct()
        val imagePath = allBlocks.filterIsInstance<PresentedLearningBlock.Image>().firstOrNull()?.path

        val audioBlocks = allBlocks.filterIsInstance<PresentedLearningBlock.Audio>()
        val primaryAudioPath = audioBlocks
            .firstOrNull { it.role == PresentedAudioRole.PRIMARY_WORD }
            ?.path
        val meaningAudioPath = audioBlocks
            .firstOrNull { it.role == PresentedAudioRole.MEANING_TRANSLATION }
            ?.path

        // Resolve Examples
        val examples = mutableListOf<FocusedExampleItem>()
        if (domainContent?.text?.exampleText != null && domainContent.text.exampleText!!.isNotBlank()) {
            val enExAudio = audioBlocks.firstOrNull { it.role == PresentedAudioRole.EXAMPLE_PRIMARY }?.path
            val viExAudio = audioBlocks.firstOrNull { it.role == PresentedAudioRole.EXAMPLE_TRANSLATION }?.path
            val normalizedExample = normalizeExamplePair(
                englishText = domainContent.text.exampleText!!,
                explicitTranslation = domainContent.text.exampleTranslation,
                hasAuthoritativeTranslationAudio = viExAudio != null
            )

            examples.add(
                FocusedExampleItem(
                    key = "domain-example-0",
                    englishText = normalizedExample.first,
                    vietnameseTranslation = normalizedExample.second,
                    audioPath = enExAudio,
                    englishAudioPath = enExAudio,
                    vietnameseAudioPath = viExAudio
                )
            )
        } else {
            // Extract from learningScene supporting ExampleScene
            val exampleScenes = completePresentation?.sections.orEmpty()
                .filter { it.kind == LearningSectionKind.EXAMPLE }
                .map { section ->
                    ExampleScene(
                        context = LearningSceneContext(answerRevealed = true),
                        capabilities = SceneCapabilities(
                            hasAudio = section.blocks.any { it is PresentedLearningBlock.Audio },
                            hasImage = false,
                            hasMeaning = false,
                            hasExamples = true
                        ),
                        blocks = section.blocks
                    )
                }
                .ifEmpty {
                    learningScene?.supportingScenes.orEmpty()
                        .filter { it.type == SceneType.EXAMPLE }
                }
            exampleScenes
                .filter { it.type == SceneType.EXAMPLE && it.blocks.isNotEmpty() }
                .forEach { exampleScene ->
                val textBlocks = exampleScene.blocks.filterIsInstance<PresentedLearningBlock.Text>()
                val exAudioBlocks = exampleScene.blocks.filterIsInstance<PresentedLearningBlock.Audio>()
                if (textBlocks.isNotEmpty()) {
                    val enExAudio = exAudioBlocks.firstOrNull { it.role == PresentedAudioRole.EXAMPLE_PRIMARY }?.path
                    val viExAudio = exAudioBlocks.firstOrNull { it.role == PresentedAudioRole.EXAMPLE_TRANSLATION }?.path
                    val firstText = textBlocks
                        .firstOrNull { it.role == PresentedTextRole.ENGLISH_EXAMPLE }
                        ?.document
                        ?.blocks
                        ?.firstOrNull()
                        ?.text
                        .orEmpty()
                    val explicitTranslation = textBlocks
                        .firstOrNull { it.role == PresentedTextRole.VIETNAMESE_EXAMPLE }
                        ?.document
                        ?.blocks
                        ?.firstOrNull()
                        ?.text
                    val normalizedExample = normalizeExamplePair(
                        firstText,
                        explicitTranslation,
                        viExAudio != null
                    )

                    examples.add(
                        FocusedExampleItem(
                            key = "scene-example-${examples.size}",
                            englishText = normalizedExample.first,
                            vietnameseTranslation = normalizedExample.second,
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

internal fun normalizeExamplePair(
    englishText: String,
    explicitTranslation: String?,
    hasAuthoritativeTranslationAudio: Boolean
): Pair<String, String?> {
    explicitTranslation?.trim()?.takeIf(String::isNotBlank)?.let {
        return englishText.trim() to it
    }
    val lines = englishText.lines().map(String::trim).filter(String::isNotBlank)
    return if (hasAuthoritativeTranslationAudio && lines.size == 2) {
        lines[0] to lines[1]
    } else {
        englishText.trim() to null
    }
}
