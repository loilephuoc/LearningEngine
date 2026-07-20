package vn.loi.learning.infrastructure.contentpackaging

import java.io.File
import java.util.zip.ZipFile

class PackageZipReader {
    fun read(zipPath: String): Map<String, String> {
        val file = File(zipPath)
        require(file.exists()) { "Package ZIP not found: $zipPath" }
        ZipFile(file).use { zip ->
            return zip.entries().asSequence().filter { !it.isDirectory }.associate { entry ->
                val content = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                entry.name to content
            }
        }
    }
}

