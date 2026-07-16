package vn.loi.learning.application.importing

data class ImportContentResult(
    val registeredContentCount: Int,
    val registeredLearningItemCount: Int,
    val skippedRecordCount: Int
)