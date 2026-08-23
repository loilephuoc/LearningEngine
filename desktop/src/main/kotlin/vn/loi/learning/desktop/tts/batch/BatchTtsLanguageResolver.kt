package vn.loi.learning.desktop.tts.batch

import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage

/**
 * Single canonical resolver for determining the target language of a content field.
 *
 * In Learning Engine vocabulary/content schema:
 * - QUESTION maps to primaryText (Source language: English)
 * - ANSWER maps to translatedText (Target/translated definition: Vietnamese)
 * - EXAMPLE maps to exampleText (Source language: English)
 * - TRANSLATION maps to exampleTranslation (Target/translated example: Vietnamese)
 *
 * Fails closed (returns null / throws when unresolvable) without silently guessing or defaulting.
 */
object BatchTtsLanguageResolver {

    /**
     * Resolves canonical [TtsLanguage] for a given [field] within the context of a package and item.
     *
     * @param packageName Optional package name for context/custom schema resolution.
     * @param item Optional specific item being evaluated.
     * @param field The logical TTS field to resolve.
     * @return Resolved [TtsLanguage] or null if the language cannot be canonically resolved.
     */
    fun resolveTargetLanguage(
        packageName: String? = null,
        item: PackageContentBrowserItem? = null,
        field: TtsField
    ): TtsLanguage? {
        return when (field) {
            TtsField.QUESTION -> TtsLanguage.ENGLISH
            TtsField.ANSWER -> TtsLanguage.VIETNAMESE
            TtsField.EXAMPLE -> TtsLanguage.ENGLISH
            TtsField.TRANSLATION -> TtsLanguage.VIETNAMESE
        }
    }

    /**
     * Resolves canonical language for [field], requiring a valid non-null result or failing closed.
     */
    fun resolveRequiredLanguage(
        packageName: String? = null,
        item: PackageContentBrowserItem? = null,
        field: TtsField
    ): TtsLanguage {
        return resolveTargetLanguage(packageName, item, field)
            ?: throw IllegalStateException(
                "Cannot resolve target language for field '${field.displayName}' in package '${packageName ?: "unknown"}'"
            )
    }

    /**
     * Checks if a [language] is compatible with a voice's locale and language code.
     */
    fun isVoiceCompatibleWithLanguage(voiceLanguage: String, voiceLocale: String, targetLanguage: TtsLanguage): Boolean {
        val targetCode = targetLanguage.code.trim().lowercase()
        val lang = voiceLanguage.trim().lowercase()
        val loc = voiceLocale.trim().lowercase()
        return lang == targetCode || loc.startsWith("$targetCode-") || loc == targetCode
    }
}
