package vn.loi.learning.infrastructure.persistence.json

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/** Reads persisted text through stream APIs available on both Android and desktop JVMs. */
internal object PortableTextFileReader {
    fun read(
        filePath: Path,
        charset: Charset = StandardCharsets.UTF_8,
        open: (Path) -> InputStream = Files::newInputStream
    ): String = open(filePath).bufferedReader(charset).use { reader -> reader.readText() }
}
