package vn.loi.learning.android.study.debug

import vn.loi.learning.android.study.AndroidReviewNavigation
import vn.loi.learning.android.study.AndroidStudyFacade
import vn.loi.learning.android.study.AndroidStudySessionHud
import vn.loi.learning.android.study.AndroidStudyState
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.partofspeech.PartOfSpeechExtractor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentCustomField
import vn.loi.learning.domain.content.model.ContentCustomFields
import vn.loi.learning.domain.content.model.ContentFieldId
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.recall.CaseSensitivity
import vn.loi.learning.domain.study.recall.PunctuationPolicy
import vn.loi.learning.domain.study.recall.RecallAnswerContract
import vn.loi.learning.domain.study.recall.RecallAnswerKind
import vn.loi.learning.domain.study.recall.RecallCapability
import vn.loi.learning.domain.study.recall.RecallChoice
import vn.loi.learning.domain.study.recall.RecallContentCapabilities
import vn.loi.learning.domain.study.recall.RecallDeterministicSeed
import vn.loi.learning.domain.study.recall.RecallDirection
import vn.loi.learning.domain.study.recall.RecallEvidenceEligibility
import vn.loi.learning.domain.study.recall.RecallLanguageTag
import vn.loi.learning.domain.study.recall.RecallNormalizationPolicyId
import vn.loi.learning.domain.study.recall.RecallOutcome
import vn.loi.learning.domain.study.recall.RecallPlan
import vn.loi.learning.domain.study.recall.RecallPlanId
import vn.loi.learning.domain.study.recall.RecallPlatformRequirements
import vn.loi.learning.domain.study.recall.RecallPrompt
import vn.loi.learning.domain.study.recall.RecallProvenance
import vn.loi.learning.domain.study.recall.RecallResourceId
import vn.loi.learning.domain.study.recall.RecallTextSpan
import vn.loi.learning.domain.study.recall.WhitespacePolicy
import vn.loi.learning.domain.study.session.model.SessionId

object AdaptiveStudyUiLabStateFactory {

    private val fallbackDistractors = listOf(
        "Khả năng thích ứng linh hoạt",
        "Sự kiên trì vượt qua khó khăn",
        "Trạng thái cân bằng bền vững",
        "Quá trình tích lũy kinh nghiệm",
        "Phương pháp tư duy trực quan"
    )

    fun createDemoPackageContents(): List<Content> {
        return listOf(
            Content(
                id = ContentId("demo-1"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "resilience",
                    translatedText = "khả năng phục hồi nhanh chóng",
                    exampleText = "Her mental resilience helped her overcome the severe hardship."
                ),
                media = ContentMedia(
                    primaryAudio = "resilience.mp3",
                    translatedAudio = "resilience_vi.mp3",
                    image = "resilience.png"
                ),
                metadata = ContentMetadata(lesson = "Unit 1: Mindset"),
                customFields = ContentCustomFields(
                    setOf(
                        ContentCustomField(ContentFieldId("ipa"), "rɪˈzɪl.jəns"),
                        ContentCustomField(ContentFieldId("partOfSpeech"), "noun")
                    )
                )
            ),
            Content(
                id = ContentId("demo-2"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "meticulous",
                    translatedText = "tỉ mỉ, cẩn trọng",
                    exampleText = "He is always meticulous about keeping his laboratory notes clean."
                ),
                media = ContentMedia(
                    primaryAudio = "meticulous.mp3",
                    translatedAudio = "meticulous_vi.mp3",
                    image = null
                ),
                metadata = ContentMetadata(lesson = "Unit 1: Mindset"),
                customFields = ContentCustomFields(
                    setOf(
                        ContentCustomField(ContentFieldId("ipa"), "məˈtɪk.jə.ləs"),
                        ContentCustomField(ContentFieldId("partOfSpeech"), "adjective")
                    )
                )
            ),
            Content(
                id = ContentId("demo-3"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "illuminate",
                    translatedText = "soi sáng, làm sáng tỏ",
                    exampleText = "A single beam of sunlight illuminated the ancient painting."
                ),
                media = ContentMedia(
                    primaryAudio = "illuminate.mp3",
                    translatedAudio = null,
                    image = "illuminate.jpg"
                ),
                metadata = ContentMetadata(lesson = "Unit 2: Science"),
                customFields = ContentCustomFields(
                    setOf(
                        ContentCustomField(ContentFieldId("ipa"), "ɪˈluː.mə.neɪt"),
                        ContentCustomField(ContentFieldId("partOfSpeech"), "verb")
                    )
                )
            ),
            Content(
                id = ContentId("demo-4"),
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "ambiguity",
                    translatedText = "sự mơ hồ, không rõ ràng",
                    exampleText = "There was some ambiguity in the contract wording that required clarification."
                ),
                media = ContentMedia(
                    primaryAudio = null,
                    translatedAudio = null,
                    image = null
                ),
                metadata = ContentMetadata(lesson = "Unit 2: Science"),
                customFields = ContentCustomFields(
                    setOf(
                        ContentCustomField(ContentFieldId("ipa"), "ˌæm.bɪˈɡjuː.ə.ti"),
                        ContentCustomField(ContentFieldId("partOfSpeech"), "noun")
                    )
                )
            )
        )
    }

    fun createPlan(
        mode: LabStudyMode,
        content: Content,
        choices: List<RecallChoice> = emptyList()
    ): RecallPlan {
        val prompt = when (mode) {
            LabStudyMode.TYPING -> RecallPrompt.Typing(
                sourceText = content.text.translatedText?.takeIf(String::isNotBlank) ?: content.text.primaryText
            )
            LabStudyMode.LISTENING -> RecallPrompt.Listening(
                audio = RecallResourceId(content.media.primaryAudio?.takeIf(String::isNotBlank) ?: "mock_audio.mp3")
            )
            LabStudyMode.MULTIPLE_CHOICE -> RecallPrompt.MultipleChoice(
                question = content.text.primaryText,
                choices = choices
            )
            LabStudyMode.IMAGE_RECALL -> RecallPrompt.ImageRecall(
                image = RecallResourceId(content.media.image?.takeIf(String::isNotBlank) ?: "mock_image.png")
            )
            LabStudyMode.EXAMPLE_COMPLETION -> {
                val primary = content.text.primaryText
                val example = content.text.exampleText?.takeIf(String::isNotBlank)
                    ?: "This is a standard sentence with $primary used as example."
                val idx = example.indexOf(primary, ignoreCase = true)
                val span = if (idx >= 0) {
                    RecallTextSpan(idx, idx + primary.length)
                } else {
                    RecallTextSpan(0, primary.length.coerceAtLeast(1))
                }
                RecallPrompt.ExampleCompletion(
                    example = example,
                    targetSpan = span
                )
            }
        }

        val primaryAudioRef = content.media.primaryAudio?.takeIf(String::isNotBlank)
        val imageRef = content.media.image?.takeIf(String::isNotBlank)

        val capabilities = mutableSetOf(
            RecallCapability.SOURCE_TEXT,
            RecallCapability.TARGET_TRANSLATION
        )
        if (primaryAudioRef != null) capabilities.add(RecallCapability.WORD_AUDIO)
        if (imageRef != null) capabilities.add(RecallCapability.IMAGE)

        return RecallPlan(
            planId = RecallPlanId("lab-plan-${content.id.value}-${mode.recallMode.wireId}"),
            learnerId = LearnerId("lab-learner"),
            contentId = content.id,
            learningItemId = null,
            sessionId = SessionId("lab-session"),
            mode = mode.recallMode,
            direction = RecallDirection.SOURCE_TO_TARGET,
            prompt = prompt,
            answerContract = RecallAnswerContract(
                canonicalAnswer = content.text.primaryText,
                normalizationPolicy = RecallNormalizationPolicyId("default-norm"),
                caseSensitivity = CaseSensitivity.INSENSITIVE,
                punctuationPolicy = PunctuationPolicy.IGNORE,
                whitespacePolicy = WhitespacePolicy.NORMALIZE,
                expectedLanguage = RecallLanguageTag("en"),
                kind = if (mode == LabStudyMode.MULTIPLE_CHOICE) RecallAnswerKind.CHOICE else RecallAnswerKind.TEXT
            ),
            availableAssistance = emptySet(),
            evidenceClass = RecallEvidenceEligibility.STRONG,
            deterministicSeed = RecallDeterministicSeed(1L),
            generatedAt = Moment(1_000L),
            provenance = RecallProvenance.PRACTICE,
            platformRequirements = RecallPlatformRequirements(
                requiresTextInput = mode in setOf(LabStudyMode.TYPING, LabStudyMode.LISTENING, LabStudyMode.IMAGE_RECALL, LabStudyMode.EXAMPLE_COMPLETION),
                requiresChoiceSelection = mode == LabStudyMode.MULTIPLE_CHOICE,
                requiresAudioPlayback = mode == LabStudyMode.LISTENING,
                requiresImageRendering = mode == LabStudyMode.IMAGE_RECALL
            ),
            contentCapabilities = RecallContentCapabilities(
                contentId = content.id,
                available = capabilities,
                image = imageRef?.let(::RecallResourceId),
                wordAudio = primaryAudioRef?.let(::RecallResourceId)
            )
        )
    }

    fun buildState(
        content: Content,
        allPackageContents: List<Content>,
        mode: LabStudyMode,
        mediaResolver: (String) -> String?,
        currentInput: String,
        selectedChoiceId: String?,
        isRevealed: Boolean,
        isCompleted: Boolean,
        currentIndex: Int,
        totalCount: Int,
        packageTitle: String
    ): AndroidStudyState {
        val choices = if (mode == LabStudyMode.MULTIPLE_CHOICE) buildChoices(content, allPackageContents) else emptyList()
        val plan = createPlan(mode, content, choices)
        val pos = PartOfSpeechExtractor.primary(content)?.value
        val ipa = content.customFields[ContentFieldId("ipa")]?.value
            ?: content.customFields[ContentFieldId("pronunciation")]?.value
        val meaning = content.text.translatedText
        val example = content.text.exampleText
        val translation = content.customFields[ContentFieldId("translation")]?.value

        val primaryAudio = content.media.primaryAudio?.let(mediaResolver)
        val meaningAudio = content.media.translatedAudio?.let(mediaResolver)
        val exampleEnglishAudio = content.media.exampleAudio?.let(mediaResolver)
        val exampleVietnameseAudio = content.media.exampleTranslatedAudio?.let(mediaResolver)
        val resolvedImage = content.media.image?.let(mediaResolver)

        val outcome = if (isCompleted) {
            if (isInputCorrect(mode, currentInput, selectedChoiceId, content)) {
                RecallOutcome.CORRECT
            } else {
                RecallOutcome.INCORRECT
            }
        } else null

        val hud = AndroidStudySessionHud(
            newCompleted = 3,
            newTarget = 10,
            newConfiguredTarget = 10,
            reviewCompleted = 7,
            reviewTarget = 20,
            reviewConfiguredTarget = 20,
            totalLearned = totalCount,
            dueCount = 5,
            againCount = 1,
            hardCount = 2,
            goodCount = 5,
            easyCount = 2,
            skimStatus = null,
            focusedPractice = false
        )

        val navigation = AndroidReviewNavigation(
            canPrevious = currentIndex > 0,
            canNext = totalCount > 0,
            historyPreview = false
        )

        return when (mode) {
            LabStudyMode.TYPING -> {
                val evaluation = evaluateTyping(currentInput, content.text.primaryText, isCompleted)
                AndroidStudyState.Typing(
                    plan = plan,
                    prompt = meaning ?: content.text.primaryText,
                    answer = currentInput,
                    evaluation = evaluation,
                    revealed = isRevealed,
                    completed = isCompleted,
                    outcome = outcome,
                    pronunciation = ipa,
                    partOfSpeech = pos,
                    meaning = meaning,
                    example = example,
                    translation = translation,
                    resolvedPromptAudio = primaryAudio,
                    resolvedExpectedAnswerAudio = primaryAudio,
                    resolvedMeaningAudio = meaningAudio,
                    resolvedExampleEnglishAudio = exampleEnglishAudio,
                    resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                    resolvedImage = resolvedImage,
                    currentPosition = currentIndex + 1,
                    totalItems = totalCount,
                    contextTitle = packageTitle,
                    hud = hud,
                    navigation = navigation
                )
            }
            LabStudyMode.LISTENING -> {
                AndroidStudyState.Listening(
                    plan = plan,
                    audioPath = primaryAudio,
                    answer = currentInput,
                    completed = isCompleted,
                    outcome = outcome,
                    pronunciation = ipa,
                    partOfSpeech = pos,
                    meaning = meaning,
                    example = example,
                    translation = translation,
                    resolvedPromptAudio = primaryAudio,
                    resolvedExpectedAnswerAudio = primaryAudio,
                    resolvedMeaningAudio = meaningAudio,
                    resolvedExampleEnglishAudio = exampleEnglishAudio,
                    resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                    resolvedImage = resolvedImage,
                    currentPosition = currentIndex + 1,
                    totalItems = totalCount,
                    contextTitle = packageTitle,
                    hud = hud,
                    navigation = navigation
                )
            }
            LabStudyMode.MULTIPLE_CHOICE -> {
                AndroidStudyState.MultipleChoice(
                    plan = plan,
                    question = content.text.primaryText,
                    choices = choices,
                    selectedChoiceId = selectedChoiceId,
                    completed = isCompleted,
                    outcome = outcome,
                    pronunciation = ipa,
                    partOfSpeech = pos,
                    meaning = meaning,
                    example = example,
                    translation = translation,
                    resolvedPromptAudio = primaryAudio,
                    resolvedExpectedAnswerAudio = primaryAudio,
                    resolvedMeaningAudio = meaningAudio,
                    resolvedExampleEnglishAudio = exampleEnglishAudio,
                    resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                    resolvedImage = resolvedImage,
                    currentPosition = currentIndex + 1,
                    totalItems = totalCount,
                    contextTitle = packageTitle,
                    hud = hud,
                    navigation = navigation
                )
            }
            LabStudyMode.IMAGE_RECALL -> {
                AndroidStudyState.ImageRecall(
                    plan = plan,
                    imagePath = resolvedImage,
                    answer = currentInput,
                    completed = isCompleted,
                    outcome = outcome,
                    pronunciation = ipa,
                    partOfSpeech = pos,
                    meaning = meaning,
                    example = example,
                    translation = translation,
                    resolvedPromptAudio = primaryAudio,
                    resolvedExpectedAnswerAudio = primaryAudio,
                    resolvedMeaningAudio = meaningAudio,
                    resolvedExampleEnglishAudio = exampleEnglishAudio,
                    resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                    resolvedImage = resolvedImage,
                    currentPosition = currentIndex + 1,
                    totalItems = totalCount,
                    contextTitle = packageTitle,
                    hud = hud,
                    navigation = navigation
                )
            }
            LabStudyMode.EXAMPLE_COMPLETION -> {
                val primary = content.text.primaryText
                val rawExample = content.text.exampleText?.takeIf(String::isNotBlank)
                    ?: "This example illustrates the word $primary in a natural sentence."
                val idx = rawExample.indexOf(primary, ignoreCase = true)
                val (prefix, blank, suffix) = if (idx >= 0) {
                    Triple(
                        rawExample.substring(0, idx),
                        rawExample.substring(idx, idx + primary.length),
                        rawExample.substring(idx + primary.length)
                    )
                } else {
                    Triple("Sentence with [", primary, "] demonstrated here.")
                }

                AndroidStudyState.ExampleCompletion(
                    plan = plan,
                    prefix = prefix,
                    blank = blank,
                    suffix = suffix,
                    answer = currentInput,
                    revealed = isRevealed,
                    completed = isCompleted,
                    outcome = outcome,
                    pronunciation = ipa,
                    partOfSpeech = pos,
                    meaning = meaning,
                    example = rawExample,
                    translation = translation,
                    resolvedPromptAudio = primaryAudio,
                    resolvedExpectedAnswerAudio = primaryAudio,
                    resolvedMeaningAudio = meaningAudio,
                    resolvedExampleEnglishAudio = exampleEnglishAudio,
                    resolvedExampleVietnameseAudio = exampleVietnameseAudio,
                    resolvedImage = resolvedImage,
                    currentPosition = currentIndex + 1,
                    totalItems = totalCount,
                    contextTitle = packageTitle,
                    hud = hud,
                    navigation = navigation
                )
            }
        }
    }

    fun buildChoices(target: Content, allContents: List<Content>): List<RecallChoice> {
        val targetText = target.text.translatedText?.takeIf(String::isNotBlank) ?: target.text.primaryText
        val correctChoice = RecallChoice(
            id = "choice-target-${target.id.value}",
            text = targetText,
            correct = true
        )

        val otherTexts = allContents
            .filter { it.id != target.id }
            .mapNotNull { it.text.translatedText?.takeIf(String::isNotBlank) ?: it.text.primaryText.takeIf(String::isNotBlank) }
            .distinct()
            .filter { !it.equals(targetText, ignoreCase = true) }

        val distractors = mutableListOf<String>()
        distractors.addAll(otherTexts.take(3))

        var fallbackIndex = 0
        while (distractors.size < 3 && fallbackIndex < fallbackDistractors.size) {
            val candidate = fallbackDistractors[fallbackIndex++]
            if (!candidate.equals(targetText, ignoreCase = true) && candidate !in distractors) {
                distractors.add(candidate)
            }
        }

        val allList = mutableListOf(correctChoice)
        distractors.forEachIndexed { i, d ->
            allList.add(RecallChoice(id = "choice-distractor-$i", text = d, correct = false))
        }

        return allList.sortedBy { it.id.hashCode() }
    }

    private fun evaluateTyping(input: String, expected: String, completed: Boolean): TypingAnswerEvaluationStatus {
        if (!completed) return TypingAnswerEvaluationStatus.EMPTY
        val trimmedInput = input.trim()
        val trimmedExpected = expected.trim()
        return if (trimmedInput.equals(trimmedExpected, ignoreCase = true)) {
            TypingAnswerEvaluationStatus.CORRECT
        } else {
            TypingAnswerEvaluationStatus.INCORRECT
        }
    }

    private fun isInputCorrect(
        mode: LabStudyMode,
        input: String,
        selectedChoiceId: String?,
        content: Content
    ): Boolean {
        return when (mode) {
            LabStudyMode.TYPING,
            LabStudyMode.LISTENING,
            LabStudyMode.IMAGE_RECALL,
            LabStudyMode.EXAMPLE_COMPLETION -> {
                input.trim().equals(content.text.primaryText.trim(), ignoreCase = true)
            }
            LabStudyMode.MULTIPLE_CHOICE -> {
                selectedChoiceId == "choice-target-${content.id.value}"
            }
        }
    }
}
