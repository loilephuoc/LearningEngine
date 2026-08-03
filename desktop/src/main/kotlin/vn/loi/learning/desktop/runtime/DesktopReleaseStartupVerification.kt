package vn.loi.learning.desktop.runtime

import java.util.ServiceLoader
import javax.sound.sampled.spi.AudioFileReader
import javax.sound.sampled.spi.FormatConversionProvider

internal data class DesktopReleaseStartupVerificationResult(
    val audioFileReaders: List<String>,
    val formatConversionProviders: List<String>
)

internal object DesktopReleaseStartupVerification {
    fun verify(): DesktopReleaseStartupVerificationResult {
        val readers =
            ServiceLoader.load(AudioFileReader::class.java)
                .map { it.javaClass.name }
                .sorted()
        val converters =
            ServiceLoader.load(FormatConversionProvider::class.java)
                .map { it.javaClass.name }
                .sorted()
        check(readers.any { it.startsWith("javazoom.spi.mpeg.sampled.file.") }) {
            "Java Sound did not discover the MP3 audio file reader provider."
        }
        check(converters.any { it.startsWith("javazoom.spi.mpeg.sampled.convert.") }) {
            "Java Sound did not discover the MP3 format conversion provider."
        }

        return DesktopReleaseStartupVerificationResult(readers, converters)
    }
}
