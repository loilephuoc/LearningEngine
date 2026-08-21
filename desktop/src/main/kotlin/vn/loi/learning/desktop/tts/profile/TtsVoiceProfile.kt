package vn.loi.learning.desktop.tts.profile

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.Properties
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice

/**
 * Saved voice profile for a language, specifying preferred region, voice, and rate.
 */
data class TtsVoiceProfile(
    val language: TtsLanguage,
    val region: String,
    val voiceId: String,
    val rate: Int = 0
) {
    companion object {
        val DEFAULT_ENGLISH = TtsVoiceProfile(
            language = TtsLanguage.ENGLISH,
            region = "en-US",
            voiceId = "en-US-AvaMultilingualNeural",
            rate = 0
        )

        val DEFAULT_VIETNAMESE = TtsVoiceProfile(
            language = TtsLanguage.VIETNAMESE,
            region = "vi-VN",
            voiceId = "vi-VN-HoaiMyNeural",
            rate = 0
        )
    }
}

/**
 * Collection of active voice profiles for all supported languages.
 */
data class TtsVoiceProfiles(
    val english: TtsVoiceProfile = TtsVoiceProfile.DEFAULT_ENGLISH,
    val vietnamese: TtsVoiceProfile = TtsVoiceProfile.DEFAULT_VIETNAMESE
) {
    fun profileFor(language: TtsLanguage): TtsVoiceProfile =
        when (language) {
            TtsLanguage.ENGLISH -> english
            TtsLanguage.VIETNAMESE -> vietnamese
        }

    fun profileForField(field: TtsField): TtsVoiceProfile =
        when (field) {
            TtsField.QUESTION, TtsField.ANSWER, TtsField.EXAMPLE -> english
            TtsField.TRANSLATION -> vietnamese
        }

    /**
     * Resolves the actual [TtsVoice] instance from available voices, with graceful fallback.
     */
    fun resolveVoice(language: TtsLanguage, availableVoices: List<TtsVoice>): TtsVoice? {
        val profile = profileFor(language)
        // 1. Exact match on voiceId
        availableVoices.firstOrNull { it.id.equals(profile.voiceId, ignoreCase = true) }?.let { return it }

        // 2. Fallback to same language & region
        availableVoices.firstOrNull { it.locale.equals(profile.region, ignoreCase = true) }?.let { return it }

        // 3. Fallback to language code
        availableVoices.firstOrNull { it.language.equals(language.code, ignoreCase = true) }?.let { return it }

        // 4. Default available
        return availableVoices.firstOrNull()
    }
}

/**
 * Lightweight persistence store for TTS voice profiles using properties file.
 */
class TtsVoiceProfilePreferencesStore(
    private val storageFile: File
) {
    constructor(storageDir: Path) : this(storageDir.resolve("tts_profiles.properties").toFile())

    fun loadProfiles(): TtsVoiceProfiles {
        if (!storageFile.exists()) {
            return TtsVoiceProfiles()
        }

        return try {
            val props = Properties()
            FileInputStream(storageFile).use { props.load(it) }

            val enRate = props.getProperty("en.rate", "0").toIntOrNull() ?: 0
            val viRate = props.getProperty("vi.rate", "0").toIntOrNull() ?: 0

            val enProfile = TtsVoiceProfile(
                language = TtsLanguage.ENGLISH,
                region = props.getProperty("en.region", TtsVoiceProfile.DEFAULT_ENGLISH.region),
                voiceId = props.getProperty("en.voiceId", TtsVoiceProfile.DEFAULT_ENGLISH.voiceId),
                rate = enRate
            )

            val viProfile = TtsVoiceProfile(
                language = TtsLanguage.VIETNAMESE,
                region = props.getProperty("vi.region", TtsVoiceProfile.DEFAULT_VIETNAMESE.region),
                voiceId = props.getProperty("vi.voiceId", TtsVoiceProfile.DEFAULT_VIETNAMESE.voiceId),
                rate = viRate
            )

            TtsVoiceProfiles(english = enProfile, vietnamese = viProfile)
        } catch (_: Exception) {
            TtsVoiceProfiles()
        }
    }

    fun saveProfiles(profiles: TtsVoiceProfiles) {
        try {
            storageFile.parentFile?.mkdirs()
            val props = Properties()

            props.setProperty("en.region", profiles.english.region)
            props.setProperty("en.voiceId", profiles.english.voiceId)
            props.setProperty("en.rate", profiles.english.rate.toString())

            props.setProperty("vi.region", profiles.vietnamese.region)
            props.setProperty("vi.voiceId", profiles.vietnamese.voiceId)
            props.setProperty("vi.rate", profiles.vietnamese.rate.toString())

            FileOutputStream(storageFile).use { props.store(it, "LearningEngine Desktop TTS Voice Profiles") }
        } catch (ex: Exception) {
            // Non-fatal persistence failure
        }
    }

    fun resetToDefaults(): TtsVoiceProfiles {
        val defaults = TtsVoiceProfiles()
        saveProfiles(defaults)
        return defaults
    }
}
