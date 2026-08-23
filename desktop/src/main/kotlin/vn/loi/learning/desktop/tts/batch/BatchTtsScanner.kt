package vn.loi.learning.desktop.tts.batch

import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyConfig
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode

/**
 * Scans content items to identify missing audio targets and build executable batch jobs.
 */
object BatchTtsScanner {

    /**
     * Default language mapping for each target field.
     */
    fun defaultLanguageFor(field: TtsField): TtsLanguage =
        when (field) {
            TtsField.QUESTION -> TtsLanguage.ENGLISH
            TtsField.ANSWER -> TtsLanguage.ENGLISH
            TtsField.EXAMPLE -> TtsLanguage.ENGLISH
            TtsField.TRANSLATION -> TtsLanguage.VIETNAMESE
        }

    /**
     * Performs comprehensive scan on items for given selected fields.
     */
    fun scanBatchScope(
        items: List<PackageContentBrowserItem>,
        selectedFields: Set<TtsField> = TtsField.entries.toSet(),
        overwriteExisting: Boolean = false
    ): BatchTtsScopeScan {
        val validTargets = mutableListOf<BatchTtsTarget>()
        val missingCountByField = mutableMapOf<TtsField, Int>()
        for (f in TtsField.entries) {
            missingCountByField[f] = 0
        }

        var existingSkipped = 0
        var emptyTextSkipped = 0

        val samplesByField = mutableMapOf<TtsField, BatchTtsSample>()
        var repEnglish: String? = null
        var repVietnamese: String? = null

        for ((index, item) in items.withIndex()) {
            val itemIndex = index + 1
            val itemLabel = item.questionText.trim().ifBlank { item.contentId.value }

            for (field in TtsField.entries) {
                val (text, audioRef) = when (field) {
                    TtsField.QUESTION -> item.questionText to item.questionAudioRef
                    TtsField.ANSWER -> item.answerText to item.answerAudioRef
                    TtsField.EXAMPLE -> item.exampleText to item.exampleAudioRef
                    TtsField.TRANSLATION -> item.exampleTranslation to item.translationAudioRef
                }

                val hasAudio = !audioRef.isNullOrBlank()
                val isMissing = !hasAudio
                val trimmedText = text.orEmpty().trim()
                val language = defaultLanguageFor(field)

                if (trimmedText.isNotBlank() && !samplesByField.containsKey(field)) {
                    samplesByField[field] = BatchTtsSample(
                        field = field,
                        text = trimmedText,
                        contentId = item.contentId.value,
                        itemIndex = itemIndex,
                        itemLabel = itemLabel
                    )
                }

                if (isMissing && trimmedText.isNotBlank()) {
                    missingCountByField[field] = (missingCountByField[field] ?: 0) + 1

                    if (language == TtsLanguage.ENGLISH && repEnglish == null) {
                        repEnglish = trimmedText
                    } else if (language == TtsLanguage.VIETNAMESE && repVietnamese == null) {
                        repVietnamese = trimmedText
                    }
                }

                // If user selected this field for generation:
                if (field in selectedFields) {
                    if (hasAudio && !overwriteExisting) {
                        existingSkipped++
                    } else if (trimmedText.isBlank()) {
                        emptyTextSkipped++
                    } else {
                        validTargets.add(
                            BatchTtsTarget(
                                contentId = item.contentId.value,
                                field = field,
                                text = trimmedText,
                                language = language,
                                isMissing = !hasAudio || overwriteExisting,
                                hasAudio = hasAudio,
                                previousAudioRef = audioRef
                            )
                        )
                    }
                }
            }
        }

        // If no missing text found for representative, fallback to first available sample in scope
        if (repEnglish == null) {
            repEnglish = samplesByField[TtsField.QUESTION]?.text
                ?: samplesByField[TtsField.ANSWER]?.text
                ?: samplesByField[TtsField.EXAMPLE]?.text
        }
        if (repVietnamese == null) {
            repVietnamese = samplesByField[TtsField.TRANSLATION]?.text
        }

        val englishCount = validTargets.count { it.language == TtsLanguage.ENGLISH }
        val vietnameseCount = validTargets.count { it.language == TtsLanguage.VIETNAMESE }

        return BatchTtsScopeScan(
            totalSelectedItems = items.size,
            selectedFields = selectedFields,
            validTargets = validTargets,
            missingCountByField = missingCountByField,
            existingAudioSkippedCount = existingSkipped,
            emptyTextSkippedCount = emptyTextSkipped,
            englishTargetsCount = englishCount,
            vietnameseTargetsCount = vietnameseCount,
            representativeEnglishText = repEnglish,
            representativeVietnameseText = repVietnamese,
            samplesByField = samplesByField
        )
    }

    /**
     * Legacy helper scanning targets with optional field filter.
     */
    fun scanTargets(
        items: List<PackageContentBrowserItem>,
        targetField: TtsField? = null,
        missingOnly: Boolean = false
    ): List<BatchTtsTarget> {
        val selectedFields = if (targetField != null) setOf(targetField) else TtsField.entries.toSet()
        val scopeScan = scanBatchScope(items, selectedFields)
        return if (missingOnly) scopeScan.validTargets else {
            val all = mutableListOf<BatchTtsTarget>()
            for (item in items) {
                val fieldsToInspect = if (targetField != null) listOf(targetField) else TtsField.entries
                for (field in fieldsToInspect) {
                    val (text, audioRef) = when (field) {
                        TtsField.QUESTION -> item.questionText to item.questionAudioRef
                        TtsField.ANSWER -> item.answerText to item.answerAudioRef
                        TtsField.EXAMPLE -> item.exampleText to item.exampleAudioRef
                        TtsField.TRANSLATION -> item.exampleTranslation to item.translationAudioRef
                    }
                    all.add(
                        BatchTtsTarget(
                            contentId = item.contentId.value,
                            field = field,
                            text = text.orEmpty(),
                            language = defaultLanguageFor(field),
                            isMissing = audioRef.isNullOrBlank(),
                            hasAudio = !audioRef.isNullOrBlank(),
                            previousAudioRef = audioRef
                        )
                    )
                }
            }
            all
        }
    }

    /**
     * Converts missing audio targets into executable batch jobs using the given voice configurations.
     */
    fun buildJobs(
        targets: List<BatchTtsTarget>,
        englishVoice: TtsVoice,
        vietnameseVoice: TtsVoice,
        englishRate: Int = 0,
        vietnameseRate: Int = 0
    ): List<BatchTtsJob> {
        val enStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, englishVoice)
        val viStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, vietnameseVoice)
        return buildJobsWithStrategy(targets, enStrategy, viStrategy, englishRate, vietnameseRate)
    }

    /**
     * Builds batch jobs incorporating advanced voice strategies (Single, Fallback Chain, Voice Rotation).
     */
    fun buildJobsWithStrategy(
        targets: List<BatchTtsTarget>,
        englishStrategy: VoiceStrategyConfig?,
        vietnameseStrategy: VoiceStrategyConfig?,
        englishRate: Int = 0,
        vietnameseRate: Int = 0,
        englishPitch: String? = null,
        vietnamesePitch: String? = null,
        englishVolume: String? = null,
        vietnameseVolume: String? = null
    ): List<BatchTtsJob> {
        var englishIndex = 0
        var vietnameseIndex = 0

        return targets
            .filter { it.canGenerate }
            .map { target ->
                val (candidateChain, settings) = when (target.language) {
                    TtsLanguage.ENGLISH -> {
                        val chain = requireNotNull(englishStrategy) { "English voice configuration is required for English targets" }
                            .candidateChainForTarget(englishIndex)
                        englishIndex++
                        chain to Triple(englishRate, englishPitch, englishVolume)
                    }
                    TtsLanguage.VIETNAMESE -> {
                        val chain = requireNotNull(vietnameseStrategy) { "Vietnamese voice configuration is required for Vietnamese targets" }
                            .candidateChainForTarget(vietnameseIndex)
                        vietnameseIndex++
                        chain to Triple(vietnameseRate, vietnamesePitch, vietnameseVolume)
                    }
                }
                val primaryVoice = candidateChain.first()
                BatchTtsJob(
                    contentId = target.contentId,
                    field = target.field,
                    text = target.text,
                    language = target.language,
                    voice = primaryVoice,
                    rate = settings.first,
                    pitch = settings.second,
                    volume = settings.third,
                    previousAudioRef = target.previousAudioRef,
                    candidateVoices = candidateChain
                        .filter { voice -> voice.language.equals(target.language.code, ignoreCase = true) || voice.locale.startsWith(target.language.code, ignoreCase = true) }
                        .distinctBy { it.id },
                    overwriteExisting = target.hasAudio
                )
            }
    }
}
