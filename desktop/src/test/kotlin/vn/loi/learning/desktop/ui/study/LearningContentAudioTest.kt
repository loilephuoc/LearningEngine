package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LearningContentAudioTest {
    @Test
    fun `real MP3 SPI decodes imported-style path with spaces and unicode`() {
        val audio = mp3File("Gói học có dấu/word audio.mp3")

        DesktopAudioDecoder.open(audio).use { stream ->
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, stream.format.encoding)
            assertTrue(stream.read(ByteArray(512)) > 0)
        }
    }

    @Test
    fun `real adapter publishes starting playing and completion while releasing output`() {
        val audio = mp3File("audio/prompt.mp3")
        val output = CapturingDesktopAudioOutput()
        val states = CopyOnWriteArrayList<LearningContentAudioState>()
        val completed = CountDownLatch(1)
        val player = JavaSoundLearningContentAudioPlayer(
            DesktopAudioOutputFactory { output }
        )
        player.listen { state ->
            states += state
            if (state == LearningContentAudioState.Idle && states.size > 1) completed.countDown()
        }

        player.play(audio)

        assertTrue(completed.await(5, TimeUnit.SECONDS))
        assertTrue(states.any { it is LearningContentAudioState.Starting })
        assertTrue(states.any { it is LearningContentAudioState.Playing })
        assertTrue(output.bytesWritten > 0)
        assertTrue(output.closed)
        player.close()
    }

    @Test
    fun `starting another clip cancels the prior output and stale completion is inert`() {
        val first = mp3File("audio/first.mp3")
        val second = mp3File("audio/second.mp3")
        val outputs = CopyOnWriteArrayList<BlockingDesktopAudioOutput>()
        val secondPlaying = CountDownLatch(1)
        val player = JavaSoundLearningContentAudioPlayer(
            DesktopAudioOutputFactory {
                BlockingDesktopAudioOutput().also(outputs::add)
            }
        )
        player.listen { state ->
            if (state is LearningContentAudioState.Playing && state.path == second) {
                secondPlaying.countDown()
            }
        }

        player.play(first)
        waitUntil { outputs.size == 1 }
        player.play(second)

        assertTrue(secondPlaying.await(5, TimeUnit.SECONDS))
        assertTrue(outputs.first().stopped)
        assertIs<LearningContentAudioState.Playing>(player.state).also {
            assertEquals(second, it.path)
        }
        player.stop()
        assertEquals(LearningContentAudioState.Idle, player.state)
        assertTrue(outputs.all { it.closed })
    }

    @Test
    fun `missing audio reports failure and never retains playback`() {
        val missing = Files.createTempDirectory("missing-audio").resolve("none.mp3")
        val failed = CountDownLatch(1)
        val player = JavaSoundLearningContentAudioPlayer(
            DesktopAudioOutputFactory { error("output must not open") }
        )
        player.listen { if (it is LearningContentAudioState.Failed) failed.countDown() }

        player.play(missing)

        assertTrue(failed.await(5, TimeUnit.SECONDS))
        assertEquals(missing.toAbsolutePath().normalize(), assertIs<LearningContentAudioState.Failed>(player.state).path)
        player.close()
    }

    private fun mp3File(relative: String): Path {
        val root = Files.createTempDirectory("learning audio kiểm thử ")
        val target = root.resolve(relative)
        Files.createDirectories(target.parent)
        val repairedPrefix = MP3_BASE64.replace(
            "D/40jEAD",
            "A".repeat(24) + "D/40jEAD",
        )
        val repairedFixture = repairedPrefix.dropLast(2) + "V".repeat(10) + repairedPrefix.takeLast(2)
        Files.write(target, Base64.getDecoder().decode(repairedFixture))
        return target.toAbsolutePath().normalize()
    }

    private fun waitUntil(assertion: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!assertion() && System.nanoTime() < deadline) Thread.yield()
        assertTrue(assertion())
    }

    private class CapturingDesktopAudioOutput : DesktopAudioOutput {
        var bytesWritten = 0
        var closed = false
        override fun start() = Unit
        override fun write(bytes: ByteArray, offset: Int, length: Int) { bytesWritten += length }
        override fun drain() = Unit
        override fun stop() = Unit
        override fun close() { closed = true }
    }

    private class BlockingDesktopAudioOutput : DesktopAudioOutput {
        @Volatile var stopped = false
        @Volatile var closed = false
        override fun start() = Unit
        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            while (!stopped && !closed) Thread.yield()
        }
        override fun drain() = Unit
        override fun stop() { stopped = true }
        override fun close() { closed = true }
    }

    private companion object {
        const val MP3_BASE64 =
            "SUQzBAAAAAAAIlRTU0UAAAAOAAADTGF2ZjYxLjcuMTAwAAAAAAAAAAAAAAD/40jAAAAAAAAAAAAASW5mbwAAAA8AAAAEAAAFoABmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmaZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZmZzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzP////////////////////////////////8AAAAATGF2YzYxLjE5AAAAAAAAAAAAAAAAJARAAAAAAAAABaBNKclOAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD/40jEADgiakgDWzACgYCAmAgJgICYCAmAgICAyzZeNFNAIWwAAAYABGBARgAAWYLwJEIByz5gYKYOEmEg4CC172S55hgMIgEyVFNjfTkZ86enOhkzfWEz0XQVTXT0AAAYAAFoEHF0Q5blbts7Z219/5/RPTAAAAhByYQhl3rGEAGTve9kyYAIIREGECAWTJ3dkyZMIIECEGECBAmTJkyZMmTJkCBAgQIECBAmTJk07JkyZAgQiIgxAmTTu7u7QBAEAQBAHwfB8HwcBAEAQBAHwfB8HwcBAEAQBAHwfB8HwcBAEAQc8T/8///wQOaVMAcCAwJQJjAzBIMAwCvHGtk3VH+4pIwMANzBIBlMBsEYwVQarVW7SlkG8JgHTATAbML/40jEMkkrwkwBntAAbDaMEAIE05CGDGzE+jxgOAFGnWOsYUwDgYA2YGIC5ozH4mHODOaSydiacaaYU8aE4Z1Fl3HHmR9WZgEQQkNICAosWBgwWYwWYoeXmMCAMCETRLk6////8HIwoAT4DiBe9e4kHR4LYrSRWRWaSqVUrpM6Yd//////7NEDE523S7UweRSC53ngFyXJiT/P9Mv7Ga0al3///////6v2GQIvxh8jZu1iQNAa3I6tLS6pst48/L///////////b9rE45jlz73uRRPY5d+D3c1/7/8t446yy3jjrL///////////+9Bzv9kT+YyF/8pFDm5bD+pdDikoADFQjEYjEYLOodkKwCK13Hf//hcCVijGJTHDLM1yP/40jEIEU7coG5mtgDX//pdGNjHx8HPBb/KU//+YOII5pco8tMZMpSv1wpqXQD///+b3iG6m5xBea5NmtFRt5AabAGkku8a2sqv4/////5lgCY0bmMAwKSjFy4xEHCD0xIpMMBA42mtZTO8a2sqv//////mIkhhIKLFphxCYICDxUYaOGAgpMSmFDYABCgj3jW1lV3jW1lV////////MJGASCkQ4YQLgwEHhswcUCoKNChgwmFgQWEzBxARgu8a2squ8a2squ+f////////4kIGCB4oCBweYKHDIKGBBggaOAgQFmChRCCg4EMCCyoDA4HMDCCoD1tdq75W12rvlbXas1MQU1FMy4xMDBVVVVVVVVVVVVVVVVVVVVVVVVVVVX/40jEAAAAA0gBwAAATEFNRTMuMTAwVVVVVVVVVVVVVVVMQU1FMy4xMDBVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVU="
    }
}
