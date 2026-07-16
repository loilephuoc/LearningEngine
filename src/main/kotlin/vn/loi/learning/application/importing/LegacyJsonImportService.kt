package vn.loi.learning.application.importing

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.infrastructure.importer.legacy.LegacyJsonImporter

/**
 * Adapter service nối Legacy Importer với public LearningEngine API.
 */
class LegacyJsonImportService(
    private val engine: LearningEngine,
    private val importer: LegacyJsonImporter =
        LegacyJsonImporter()
) {

    fun import(
        sourceName: String,
        jsonText: String
    ): ImportContentResult {
        val result = importer.import(
            sourceName = sourceName,
            jsonText = jsonText
        )

        result.contents.forEach(engine::registerContent)
        result.learningItems.forEach(engine::registerLearningItem)

        return ImportContentResult(
            registeredContentCount =
                result.importedContentCount,
            registeredLearningItemCount =
                result.importedLearningItemCount,
            skippedRecordCount =
                result.skippedRecordCount
        )
    }
}