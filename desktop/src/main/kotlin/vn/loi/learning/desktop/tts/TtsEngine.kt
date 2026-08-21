package vn.loi.learning.desktop.tts

import java.nio.file.Path

/**
 * Narrow abstraction for Text-to-Speech synthesis engines.
 */
interface TtsEngine {

    /**
     * Lists available TTS voices in deterministic order.
     */
    suspend fun listVoices(): List<TtsVoice>

    /**
     * Synthesizes the requested text and writes the generated audio to [outputFile].
     *
     * @throws TtsException with typed [TtsError] on failure.
     */
    suspend fun synthesize(
        request: TtsSynthesisRequest,
        outputFile: Path
    ): TtsSynthesisResult
}
