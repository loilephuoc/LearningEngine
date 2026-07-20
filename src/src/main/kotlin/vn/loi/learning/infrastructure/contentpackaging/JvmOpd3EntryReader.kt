package vn.loi.learning.infrastructure.contentpackaging

import java.util.zip.ZipFile

class JvmOpd3EntryReader : Opd3EntryReader {

    override fun readText(
        archive: ZipFile,
        entryName: String
    ): String? {
        val entry = archive.getEntry(entryName)
            ?: return null

        return archive.getInputStream(entry)
            .bufferedReader()
            .use { reader -> reader.readText() }
    }
}
