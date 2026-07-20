package vn.loi.learning.infrastructure.contentpackaging

import java.util.zip.ZipFile

fun interface Opd3EntryReader {

    fun readText(
        archive: ZipFile,
        entryName: String
    ): String?
}
