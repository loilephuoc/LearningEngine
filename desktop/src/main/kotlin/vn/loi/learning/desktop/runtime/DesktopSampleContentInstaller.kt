package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.LearningApplicationContext

object DesktopSampleContentInstaller {
    fun install(applicationContext: LearningApplicationContext) {
        val directory = Files.createTempDirectory("learning-engine-sample")
        try {
            val archive = directory.resolve("learning-engine-sample.opd3")
            ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
                entries.forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
            val result = applicationContext.packageImporter(directory).importAllDetailed(
                PackageCatalogId("desktop-content-library")
            )
            check(result.failures.isEmpty() && result.successfulImports.size == 1) {
                "Sample content import failed."
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private val entries = mapOf(
        "manifest.json" to """{"name":"Learning Engine Starter","version":"1.0.0","format":"OPD3","contentCount":2,"learningItemCount":2}""",
        "metadata.json" to """{"name":"Learning Engine Starter","version":"1.0.0","format":"OPD3"}""",
        "contents.json" to """{"contents":[{"id":"starter-retrieve","type":"WORD","primaryText":"retrieve","translatedText":"gợi nhớ","title":"Retrieval practice","group":"Learning Engine Starter","section":"Foundations","lesson":"Retrieval"},{"id":"starter-spacing","type":"WORD","primaryText":"spacing","translatedText":"ôn cách quãng","title":"Spaced practice","group":"Learning Engine Starter","section":"Foundations","lesson":"Spacing"}]}""",
        "learning-items.json" to """{"learningItems":[{"id":"starter-item-retrieve","contentId":"starter-retrieve","mode":"MEANING_RECOGNITION","isEnabled":true},{"id":"starter-item-spacing","contentId":"starter-spacing","mode":"MEANING_RECOGNITION","isEnabled":true}]}"""
    )
}
