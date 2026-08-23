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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import vn.loi.learning.desktop.tts.batch.BatchTtsEventLogger

/**
 * JVM-native Edge Read Aloud TTS engine implementation.
 * Wraps tts-edge-java with isolated daemon worker execution, structured error translation,
 * hard attempt cancellation/abandonment, deterministic voice sorting, and safe output cleanup.
 */
class EdgeTtsEngine(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val executor: ExecutorService = defaultExecutor,
    private val eventLogger: BatchTtsEventLogger = BatchTtsEventLogger.NoOp
) : TtsEngine, AutoCloseable {

    @Volatile
    private var cachedVoices: List<TtsVoice>? = null

    val abandonedWorkerCount = AtomicInteger(0)
    val activeWorkerCount = AtomicInteger(0)

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
    ): TtsSynthesisResult {
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

        return suspendCancellableCoroutine { continuation ->
            val isAbandoned = AtomicBoolean(false)
            activeWorkerCount.incrementAndGet()

            val future = executor.submit {
                val tempWorkDir = runCatching { Files.createTempDirectory("edge-tts-work-").toFile() }.getOrNull()
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
                        .storage(tempWorkDir?.absolutePath ?: "")
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

                    if (isAbandoned.get()) {
                        return@submit
                    }

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

                    if (!isAbandoned.get() && continuation.isActive) {
                        continuation.resume(
                            TtsSynthesisResult(
                                outputFile = outputFile,
                                byteCount = finalSize
                            )
                        )
                    }
                } catch (e: Throwable) {
                    if (!isAbandoned.get() && continuation.isActive) {
                        Files.deleteIfExists(outputFile)
                        continuation.resumeWithException(mapToTtsException(e, "Synthesis failed for voice: ${request.voice.id}"))
                    }
                } finally {
                    activeWorkerCount.decrementAndGet()
                    tempWorkDir?.deleteRecursively()
                }
            }

            continuation.invokeOnCancellation {
                if (isAbandoned.compareAndSet(false, true)) {
                    val count = abandonedWorkerCount.incrementAndGet()
                    eventLogger.logAttemptAbandoned(
                        targetIndex = 0,
                        voiceId = request.voice.id,
                        attemptNumber = 0,
                        reason = "Coroutine cancelled or timed out; worker marked abandoned (total abandoned: $count)"
                    )
                    future.cancel(true)
                    Files.deleteIfExists(outputFile)
                }
            }
        }
    }

    override fun close() {
        // defaultExecutor is a daemon pool and does not hold process open
    }

    companion object {
        private val workerCounter = AtomicInteger(0)
        private val defaultExecutor: ExecutorService by lazy {
            Executors.newCachedThreadPool { runnable ->
                Thread(runnable).apply {
                    isDaemon = true
                    name = "edge-tts-worker-${workerCounter.incrementAndGet()}"
                }
            }
        }

        private fun mapToTtsException(e: Throwable, fallbackMessage: String): TtsException = when (e) {
            is TtsException -> e
            is SocketTimeoutException, is TimeoutException -> TtsException(
                TtsError.Timeout,
                e
            )
            is UnknownHostException -> TtsException(
                TtsError.NoNetwork,
                e
            )
            is ConnectException -> TtsException(
                TtsError.ProviderUnavailable("Unable to connect to Edge TTS service: ${e.message}"),
                e
            )
            is CancellationException -> TtsException(
                TtsError.Cancelled,
                e
            )
            is IOException -> {
                val msg = e.message ?: ""
                if (msg.contains("network", ignoreCase = true) || msg.contains("connection", ignoreCase = true) || msg.contains("socket", ignoreCase = true)) {
                    TtsException(TtsError.NoNetwork, e)
                } else if (msg.contains("permission", ignoreCase = true) || msg.contains("access", ignoreCase = true) || msg.contains("disk", ignoreCase = true)) {
                    TtsException(TtsError.OutputWriteFailed("Disk write error: $msg"), e)
                } else {
                    TtsException(TtsError.GenerationFailed(msg.ifBlank { fallbackMessage }), e)
                }
            }
            else -> TtsException(
                TtsError.GenerationFailed(e.message?.ifBlank { fallbackMessage } ?: fallbackMessage),
                e
            )
        }
    }
}
