package vn.loi.learning.desktop.tts

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage

/**
 * Primary coordinator service for Desktop TTS capabilities.
 * Manages voice discovery, temporary preview synthesis, and permanent audio storage via [ContentMediaStorage].
 *
 * NOTE: As an explicit architectural invariant, [generatePermanentAudio] ONLY generates and stores
 * the media asset. It NEVER mutates Content or assigns audio references (Generate != Apply).
 */
class DesktopTtsAudioService(
    private val ttsEngine: TtsEngine,
    private val mediaStorage: ContentMediaStorage,
    private val fileNamer: TtsAudioFileNamer = TtsAudioFileNamer(),
    private val previewStore: TtsPreviewStore = TtsPreviewStore(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Lists all available TTS voices.
     */
    suspend fun listVoices(): List<TtsVoice> =
        ttsEngine.listVoices()

    /**
     * Lists English voices.
     */
    suspend fun listEnglishVoices(): List<TtsVoice> =
        listVoices().filter { it.isEnglish }

    /**
     * Lists Vietnamese voices.
     */
    suspend fun listVietnameseVoices(): List<TtsVoice> =
        listVoices().filter { it.isVietnamese }

    /**
     * Returns the default target language code for a logical [TtsField].
     */
    fun defaultLanguageFor(field: TtsField): String = when (field) {
        TtsField.QUESTION -> "en"
        TtsField.ANSWER -> "en"
        TtsField.EXAMPLE -> "en"
        TtsField.TRANSLATION -> "vi"
    }

    /**
     * Recommends a default voice for the given language code from the provided voice list.
     */
    fun defaultVoiceFor(languageCode: String, availableVoices: List<TtsVoice>): TtsVoice? {
        val lang = languageCode.trim().lowercase()
        return if (lang.startsWith("vi")) {
            availableVoices.firstOrNull { it.id == "vi-VN-HoaiMyNeural" }
                ?: availableVoices.firstOrNull { it.id == "vi-VN-NamMinhNeural" }
                ?: availableVoices.firstOrNull { it.isVietnamese }
        } else {
            availableVoices.firstOrNull { it.id == "en-US-AvaMultilingualNeural" }
                ?: availableVoices.firstOrNull { it.id == "en-US-JennyNeural" }
                ?: availableVoices.firstOrNull { it.id == "en-US-GuyNeural" }
                ?: availableVoices.firstOrNull { it.isEnglish }
        }
    }

    /**
     * Synthesizes audio to a temporary preview file in [previewStore].
     * Does NOT touch [ContentMediaStorage] and does NOT modify Learning Item state.
     */
    suspend fun preview(
        text: String,
        voice: TtsVoice,
        rate: Int = 0
    ): Path = withContext(ioDispatcher) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            throw TtsException(TtsError.InvalidText("Text content must not be blank for preview"))
        }

        val previewPath = previewStore.createPreviewFile()
        try {
            ttsEngine.synthesize(
                TtsSynthesisRequest(
                    text = trimmed,
                    voice = voice,
                    rate = rate
                ),
                previewPath
            )
            previewPath
        } catch (e: Exception) {
            previewStore.cleanPrevious()
            throw e
        }
    }

    /**
     * Synthesizes audio, stores it permanently in [ContentMediaStorage] for [packageName],
     * and returns the resulting [ContentMediaAsset].
     *
     * Invariant: Does NOT modify Content or audio references.
     */
    suspend fun generatePermanentAudio(
        contentId: String,
        packageName: String,
        field: TtsField,
        text: String,
        voice: TtsVoice,
        rate: Int = 0,
        language: String = defaultLanguageFor(field)
    ): ContentMediaAsset = withContext(ioDispatcher) {
        require(contentId.isNotBlank()) { "Content ID must not be blank" }
        require(packageName.isNotBlank()) { "Package name must not be blank" }

        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            throw TtsException(TtsError.InvalidText("Text content must not be blank"))
        }

        val fileName = fileNamer.generateUniqueFileName(
            contentId = contentId,
            field = field,
            languageCode = language
        ) { candidateName ->
            val relativePath = "$packageName/$candidateName"
            mediaStorage.exists(relativePath)
        }

        val tempWorkingFile = Files.createTempFile("tts-permanent-", ".mp3")
        try {
            ttsEngine.synthesize(
                TtsSynthesisRequest(
                    text = trimmed,
                    voice = voice,
                    rate = rate
                ),
                tempWorkingFile
            )

            val asset = mediaStorage.storeStream(
                packageName = packageName,
                fileName = fileName,
                source = tempWorkingFile
            )
            asset
        } finally {
            Files.deleteIfExists(tempWorkingFile)
        }
    }

    /**
     * Disposes temporary resources.
     */
    fun close() {
        previewStore.close()
    }
}
