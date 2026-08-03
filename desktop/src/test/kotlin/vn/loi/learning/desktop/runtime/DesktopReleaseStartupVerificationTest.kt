package vn.loi.learning.desktop.runtime

import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopReleaseStartupVerificationTest {
    @Test
    fun `discovers MP3 reader and conversion providers without opening an audio device`() {
        val result = DesktopReleaseStartupVerification.verify()

        assertTrue(result.audioFileReaders.any { it.startsWith("javazoom.spi.mpeg.sampled.file.") })
        assertTrue(result.formatConversionProviders.any { it.startsWith("javazoom.spi.mpeg.sampled.convert.") })
    }
}
