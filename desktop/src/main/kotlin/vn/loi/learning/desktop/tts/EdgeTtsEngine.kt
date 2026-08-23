package vn.loi.learning.desktop.tts

import io.github.whitemagic2014.tts.TTS
import io.github.whitemagic2014.tts.TTSVoice
import io.github.whitemagic2014.tts.bean.Voice
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

/**
 * JVM-native Edge Read Aloud TTS engine implementation.
 * Wraps tts-edge-java with coroutine dispatch, structured error translation,
 * deterministic voice sorting, and safe output cleanup.
 */
class EdgeTtsEngine(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TtsEngine {

    @Volatile
    private var cachedVoices: List<TtsVoice>? = null

    override suspend fun listVoices(): List<TtsVoice> = withContext(ioDispatcher) {
        cachedVoices?.let { return@withContext it }

        try {
            val rawVoices = TTSVoice.provides().orEmpty()
            val mapped = rawVoices.map { raw ->
                TtsVoice(
                    id = raw.shortName.orEmpty().ifBlank { raw.name.orEmpty() },
                    displayName = raw.friendlyName.orEmpty().ifBlank { raw.shortName.orEmpty() },
                    locale = raw.locale.orEmpty(),
                    language = raw.locale?.substringBefore('-')?.lowercase().orEmpty(),
                    gender = raw.gender
                )
            }.filter { it.id.isNotBlank() }
                .sortedWith(
                    compareBy(
                        { it.locale.lowercase() },
                        { it.displayName.lowercase() },
                        { it.id.lowercase() }
                    )
                )

            if (mapped.isNotEmpty()) {
                cachedVoices = mapped
            }
            mapped
        } catch (e: Exception) {
            throw mapToTtsException(e, "Failed to retrieve voice list")
        }
    }

    override suspend fun synthesize(
        request: TtsSynthesisRequest,
        outputFile: Path
    ): TtsSynthesisResult = runInterruptible(ioDispatcher) {
        val trimmedText = request.text.trim()
        if (trimmedText.isBlank()) {
            throw TtsException(TtsError.InvalidText("Text content must not be blank"))
        }

        if (request.voice.id.isBlank()) {
            throw TtsException(TtsError.VoiceUnavailable("Voice ID must not be blank"))
        }

        val targetParent = outputFile.parent?.toAbsolutePath()?.normalize()
            ?: throw TtsException(TtsError.OutputWriteFailed("Invalid output file path parent"))
        Files.createDirectories(targetParent)

        // Temporary directory for the generation intermediate file
        val tempWorkDir = Files.createTempDirectory("edge-tts-work-").toFile()
        val tempBaseName = "tts_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"

        try {
            val voice = Voice().apply {
                shortName = request.voice.id
                name = request.voice.id
                locale = request.voice.locale
            }

            val tts = TTS(voice, trimmedText)
                .findHeadHook()
                .isRateLimited(true)
                .storage(tempWorkDir.absolutePath)
                .fileName(tempBaseName)
                .overwrite(true)
                .formatMp3()

            if (request.rate != 0) {
                val sign = if (request.rate > 0) "+" else ""
                tts.voiceRate("$sign${request.rate}%")
            }

            request.pitch?.takeIf { it.isNotBlank() }?.let { tts.voicePitch(it) }
            request.volume?.takeIf { it.isNotBlank() }?.let { tts.voiceVolume(it) }

            val generatedName = tts.trans()
            val generatedFile = File(tempWorkDir, "$tempBaseName.mp3")

            if (!generatedFile.exists() || generatedFile.length() <= 0L) {
                throw TtsException(
                    TtsError.GenerationFailed("TTS provider produced an empty or missing audio file: $generatedName")
                )
            }

            Files.copy(
                generatedFile.toPath(),
                outputFile,
                StandardCopyOption.REPLACE_EXISTING
            )

            val finalSize = Files.size(outputFile)
            if (finalSize <= 0L) {
                Files.deleteIfExists(outputFile)
                throw TtsException(TtsError.GenerationFailed("Output audio file has 0 bytes"))
            }

            TtsSynthesisResult(
                outputFile = outputFile,
                byteCount = finalSize
            )
        } catch (e: CancellationException) {
            Files.deleteIfExists(outputFile)
            throw TtsException(TtsError.Cancelled, e)
        } catch (e: TtsException) {
            Files.deleteIfExists(outputFile)
            throw e
        } catch (e: Exception) {
            Files.deleteIfExists(outputFile)
            throw mapToTtsException(e, "Synthesis failed for voice: ${request.voice.id}")
        } finally {
            tempWorkDir.deleteRecursively()
        }
    }

    private fun mapToTtsException(e: Throwable, fallbackMessage: String): TtsException {
        if (e is TtsException) return e
        return when (e) {
            is UnknownHostException, is ConnectException ->
                TtsException(TtsError.NoNetwork, e)
            is SocketTimeoutException, is TimeoutException ->
                TtsException(TtsError.Timeout, e)
            is IOException ->
                TtsException(TtsError.ProviderUnavailable(e.message ?: fallbackMessage), e)
            else ->
                TtsException(TtsError.GenerationFailed(e.message ?: fallbackMessage), e)
        }
    }
}
