package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.study.learning.model.LearningItem

/**
 * Dữ liệu Domain đã được đọc và chuyển đổi từ package source.
 *
 * DTO này chưa được lưu vào repository.
 */
data class ImportedPackageContent(
    val contents: List<Content>,
    val learningItems: List<LearningItem>,
    val libraries: List<ContentLibrary> = emptyList(),
    val report: PackageImportReport = PackageImportReport(),
    val warnings: List<String> = emptyList(),
    val onCommit: (() -> Unit)? = null,
    val onRollback: (() -> Unit)? = null
) {

    val importedLibraryCount: Int
        get() = libraries.size
}


