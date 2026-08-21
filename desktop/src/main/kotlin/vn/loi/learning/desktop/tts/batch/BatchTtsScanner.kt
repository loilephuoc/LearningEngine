package vn.loi.learning.desktop.tts.batch

import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice

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
     * Scans a collection of browser items for target fields.
     *
     * @param items List of package content browser items to scan.
     * @param targetField Optional specific field filter; if null, scans all 4 fields.
     * @param missingOnly When true, returns only candidates that lack audio.
     */
    fun scanTargets(
        items: List<PackageContentBrowserItem>,
        targetField: TtsField? = null,
        missingOnly: Boolean = false
    ): List<BatchTtsTarget> {
        val targets = mutableListOf<BatchTtsTarget>()

        for (item in items) {
            val fieldsToInspect = if (targetField != null) listOf(targetField) else TtsField.entries

            for (field in fieldsToInspect) {
                val (text, audioRef) = when (field) {
                    TtsField.QUESTION -> item.questionText to item.questionAudioRef
                    TtsField.ANSWER -> item.answerText to item.answerAudioRef
                    TtsField.EXAMPLE -> item.exampleText to item.exampleAudioRef
                    TtsField.TRANSLATION -> item.exampleTranslation to item.translationAudioRef
                }

                val hasAudio = !audioRef.isNullOrBlank()
                val isMissing = !hasAudio
                val language = defaultLanguageFor(field)

                val target = BatchTtsTarget(
                    contentId = item.contentId.value,
                    field = field,
                    text = text.orEmpty(),
                    language = language,
                    isMissing = isMissing,
                    hasAudio = hasAudio
                )

                if (!missingOnly || target.canGenerate) {
                    targets.add(target)
                }
            }
        }

        return targets
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
        return targets
            .filter { it.canGenerate }
            .map { target ->
                val (voice, rate) = when (target.language) {
                    TtsLanguage.ENGLISH -> englishVoice to englishRate
                    TtsLanguage.VIETNAMESE -> vietnameseVoice to vietnameseRate
                }
                BatchTtsJob(
                    contentId = target.contentId,
                    field = target.field,
                    text = target.text,
                    language = target.language,
                    voice = voice,
                    rate = rate
                )
            }
    }
}
