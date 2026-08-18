package vn.loi.learning.desktop.ui.browser.export

import java.io.File
import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.ui.browser.ContentProblemFilter

object ContentMaintenanceExporter {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
    }

    fun buildJsonString(
        packageId: String,
        packageName: String,
        scope: ContentMaintenanceExportScope,
        filter: ContentProblemFilter,
        items: List<PackageContentBrowserItem>,
        timestamp: Instant = Instant.now()
    ): String {
        val document = ContentMaintenanceExportDocumentJson(
            packageId = packageId,
            packageName = packageName,
            scope = scope.name,
            filter = filter.name,
            itemCount = items.size,
            exportedAt = timestamp.toString(),
            items = items.map { item ->
                ContentMaintenanceExportItemJson(
                    contentId = item.contentId.value,
                    question = item.questionText,
                    answer = item.answerText,
                    pronunciation = item.pronunciation?.takeIf(String::isNotBlank),
                    partOfSpeech = item.partOfSpeech?.takeIf(String::isNotBlank),
                    example = item.exampleText?.takeIf(String::isNotBlank),
                    translation = item.exampleTranslation?.takeIf(String::isNotBlank),
                    imageRef = item.imageRef?.takeIf(String::isNotBlank),
                    questionAudioRef = item.questionAudioRef?.takeIf(String::isNotBlank),
                    answerAudioRef = item.answerAudioRef?.takeIf(String::isNotBlank),
                    exampleAudioRef = item.exampleAudioRef?.takeIf(String::isNotBlank),
                    translationAudioRef = item.translationAudioRef?.takeIf(String::isNotBlank),
                    partOfSpeechReviewStatus = item.partOfSpeechReviewStatus?.takeIf(String::isNotBlank)
                )
            }
        )
        return json.encodeToString(document)
    }

    fun exportToFile(
        targetFile: File,
        packageId: String,
        packageName: String,
        scope: ContentMaintenanceExportScope,
        filter: ContentProblemFilter,
        items: List<PackageContentBrowserItem>,
        timestamp: Instant = Instant.now()
    ): File {
        val jsonContent = buildJsonString(
            packageId = packageId,
            packageName = packageName,
            scope = scope,
            filter = filter,
            items = items,
            timestamp = timestamp
        )
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(jsonContent, Charsets.UTF_8)
        return targetFile
    }

    fun generateSuggestedFileName(
        packageName: String,
        filter: ContentProblemFilter,
        scope: ContentMaintenanceExportScope
    ): String {
        val sanitizedPackage = packageName.replace(Regex("""[^a-zA-Z0-9_-]+"""), "_").trim('_')
        val sanitizedFilter = filter.name.lowercase().replace('_', '-')
        val date = java.time.LocalDate.now().toString()
        return "${sanitizedPackage}_${sanitizedFilter}_$date.json"
    }
}
