package vn.loi.learning.desktop.ui.browser.export

import kotlinx.serialization.Serializable

enum class ContentMaintenanceExportScope(val label: String) {
    CURRENT_FILTER_RESULTS("Current Filter Results"),
    SELECTED_ITEMS("Selected Items"),
    CURRENT_SEARCH_RESULTS("Current Search Results"),
    ALL_ITEMS("All Items")
}

@Serializable
data class ContentMaintenanceExportItemJson(
    val contentId: String,
    val question: String,
    val answer: String,
    val pronunciation: String? = null,
    val partOfSpeech: String? = null,
    val example: String? = null,
    val translation: String? = null,
    val imageRef: String? = null,
    val questionAudioRef: String? = null,
    val answerAudioRef: String? = null,
    val exampleAudioRef: String? = null,
    val translationAudioRef: String? = null,
    val partOfSpeechReviewStatus: String? = null
)

@Serializable
data class ContentMaintenanceExportDocumentJson(
    val format: String = "learning-engine-content-maintenance",
    val version: Int = 1,
    val packageId: String,
    val packageName: String,
    val scope: String,
    val filter: String,
    val itemCount: Int,
    val exportedAt: String,
    val items: List<ContentMaintenanceExportItemJson>
)

data class ContentMaintenanceExportState(
    val selectedScope: ContentMaintenanceExportScope,
    val targetDirectory: String,
    val targetFileName: String,
    val isExporting: Boolean = false,
    val exportSuccessMessage: String? = null,
    val exportedFilePath: String? = null,
    val errorMessage: String? = null
)
