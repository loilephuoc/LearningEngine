package vn.loi.learning.desktop.tts.ui

import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.tts.TtsAudioParameters
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice

/**
 * Target payload passed to Desktop TTS dialog.
 */
data class TtsDialogTarget(
    val contentId: String,
    val questionText: String,
    val answerText: String,
    val exampleText: String? = null,
    val exampleTranslation: String? = null,
    val questionAudioRef: String? = null,
    val answerAudioRef: String? = null,
    val exampleAudioRef: String? = null,
    val translationAudioRef: String? = null,
    val initialField: TtsField? = null
) {
    companion object {
        fun fromBrowserItem(
            item: PackageContentBrowserItem,
            initialField: TtsField? = null
        ): TtsDialogTarget = TtsDialogTarget(
            contentId = item.contentId.value,
            questionText = item.questionText,
            answerText = item.answerText,
            exampleText = item.exampleText,
            exampleTranslation = item.exampleTranslation,
            questionAudioRef = item.questionAudioRef,
            answerAudioRef = item.answerAudioRef,
            exampleAudioRef = item.exampleAudioRef,
            translationAudioRef = item.translationAudioRef,
            initialField = initialField
        )
    }

    fun textFor(field: TtsField): String = when (field) {
        TtsField.QUESTION -> questionText
        TtsField.ANSWER -> answerText
        TtsField.EXAMPLE -> exampleText.orEmpty()
        TtsField.TRANSLATION -> exampleTranslation.orEmpty()
    }

    fun audioRefFor(field: TtsField): String? = when (field) {
        TtsField.QUESTION -> questionAudioRef
        TtsField.ANSWER -> answerAudioRef
        TtsField.EXAMPLE -> exampleAudioRef
        TtsField.TRANSLATION -> translationAudioRef
    }

    fun hasAudioFor(field: TtsField): Boolean =
        !audioRefFor(field).isNullOrBlank()

    fun firstMissingField(): TtsField {
        val candidates = listOf(TtsField.QUESTION, TtsField.ANSWER, TtsField.EXAMPLE, TtsField.TRANSLATION)
        return initialField?.takeIf { !hasAudioFor(it) }
            ?: candidates.firstOrNull { !hasAudioFor(it) }
            ?: initialField
            ?: TtsField.QUESTION
    }
}

/**
 * Supported speech rates for Desktop TTS UI.
 */
enum class TtsRateOption(val rateValue: Int, val label: String) {
    VERY_SLOW(-40, "Very Slow (-40%)"),
    SLOW(-25, "Slow (-25%)"),
    SLIGHTLY_SLOW(-10, "Slightly Slow (-10%)"),
    NORMAL(0, "Normal"),
    SLIGHTLY_FAST(10, "Slightly Fast (+10%)"),
    FAST(25, "Fast (+25%)"),
    VERY_FAST(40, "Very Fast (+40%)")
}

/**
 * Supported gender filter options.
 */
enum class TtsGenderFilter(val label: String) {
    ALL("All"),
    FEMALE("Female"),
    MALE("Male")
}

/**
 * Preview playback state for Desktop TTS dialog.
 */
sealed class TtsPreviewState {
    object Idle : TtsPreviewState()
    object Synthesizing : TtsPreviewState()
    object Playing : TtsPreviewState()
    data class Failed(val message: String) : TtsPreviewState()
}

/**
 * Permanent generation state for Desktop TTS dialog.
 */
sealed class TtsGenerationState {
    object Idle : TtsGenerationState()
    object Generating : TtsGenerationState()
    data class Success(val asset: ContentMediaAsset) : TtsGenerationState()
    data class Failed(val message: String) : TtsGenerationState()
}

/**
 * Complete UI state for Desktop TTS dialog.
 */
data class DesktopTtsUiState(
    val target: TtsDialogTarget,
    val packageName: String,
    val selectedField: TtsField = target.firstMissingField(),
    val selectedLanguage: TtsLanguage = defaultLanguageForField(target.firstMissingField()),
    val selectedRegion: String = defaultRegionForLanguage(defaultLanguageForField(target.firstMissingField())),
    val selectedGender: TtsGenderFilter = TtsGenderFilter.ALL,
    val selectedVoice: TtsVoice? = null,
    val ratePercent: Int = 0,
    val pitchHz: Int = 0,
    val volumePercent: Int = 0,
    val allVoices: List<TtsVoice> = emptyList(),
    val isLoadingVoices: Boolean = false,
    val voiceLoadError: String? = null,
    val previewState: TtsPreviewState = TtsPreviewState.Idle,
    val generationState: TtsGenerationState = TtsGenerationState.Idle,
    val isPlayingGenerated: Boolean = false
) {
    val rate: Int get() = ratePercent
    val pitch: String get() = if (pitchHz >= 0) "+${pitchHz}Hz" else "${pitchHz}Hz"
    val volume: String get() = if (volumePercent >= 0) "+${volumePercent}%" else "${volumePercent}%"

    val selectedRate: TtsRateOption
        get() = TtsRateOption.entries.firstOrNull { it.rateValue == ratePercent } ?: TtsRateOption.NORMAL

    val isConfigValid: Boolean
        get() = ratePercent in TtsAudioParameters.MIN_RATE..TtsAudioParameters.MAX_RATE &&
                pitchHz in TtsAudioParameters.MIN_PITCH..TtsAudioParameters.MAX_PITCH &&
                volumePercent in TtsAudioParameters.MIN_VOLUME..TtsAudioParameters.MAX_VOLUME

    val currentText: String get() = target.textFor(selectedField).trim()
    val isCurrentFieldAlreadyPopulated: Boolean get() = target.hasAudioFor(selectedField)
    val isTextBlank: Boolean get() = currentText.isBlank()

    val availableVoicesForLanguage: List<TtsVoice> get() = when (selectedLanguage) {
        TtsLanguage.ENGLISH -> allVoices.filter { it.isEnglish }
        TtsLanguage.VIETNAMESE -> allVoices.filter { it.isVietnamese }
    }

    val availableRegions: List<String> get() {
        val locales = availableVoicesForLanguage.map { it.locale }.distinct().sorted()
        return if (locales.isEmpty()) {
            listOf(defaultRegionForLanguage(selectedLanguage))
        } else {
            locales
        }
    }

    val filteredVoices: List<TtsVoice> get() {
        return availableVoicesForLanguage
            .filter { voice ->
                if (selectedRegion.isBlank()) true else voice.locale.equals(selectedRegion, ignoreCase = true)
            }
            .filter { voice ->
                when (selectedGender) {
                    TtsGenderFilter.ALL -> true
                    TtsGenderFilter.FEMALE -> voice.gender?.equals("Female", ignoreCase = true) == true
                    TtsGenderFilter.MALE -> voice.gender?.equals("Male", ignoreCase = true) == true
                }
            }
    }

    val canPreview: Boolean get() =
        isConfigValid && !isTextBlank && selectedVoice != null && previewState !is TtsPreviewState.Synthesizing && generationState !is TtsGenerationState.Generating

    val canGenerate: Boolean get() =
        isConfigValid && !isTextBlank && !isCurrentFieldAlreadyPopulated && selectedVoice != null &&
                generationState !is TtsGenerationState.Generating &&
                previewState !is TtsPreviewState.Synthesizing

    val canApply: Boolean get() =
        generationState is TtsGenerationState.Success && !isCurrentFieldAlreadyPopulated

    companion object {
        fun defaultLanguageForField(field: TtsField): TtsLanguage =
            vn.loi.learning.desktop.tts.batch.BatchTtsLanguageResolver.resolveRequiredLanguage(null, null, field)

        fun defaultRegionForLanguage(language: TtsLanguage): String = when (language) {
            TtsLanguage.ENGLISH -> "en-US"
            TtsLanguage.VIETNAMESE -> "vi-VN"
        }

        fun regionDisplayName(locale: String): String = when (locale.lowercase()) {
            "en-us" -> "English (US)"
            "en-gb" -> "English (UK)"
            "en-au" -> "English (Australia)"
            "en-ca" -> "English (Canada)"
            "en-nz" -> "English (New Zealand)"
            "en-ie" -> "English (Ireland)"
            "en-in" -> "English (India)"
            "en-za" -> "English (South Africa)"
            "en-sg" -> "English (Singapore)"
            "vi-vn" -> "Vietnam (vi-VN)"
            else -> locale
        }
    }
}
