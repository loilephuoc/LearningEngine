package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.study.learning.model.LearningItem

/**
 * Dữ liệu Domain cần thiết để xuất thành một content package.
 *
 * Payload không chứa đường dẫn file hoặc chi tiết ZIP/JSON vì các thông tin đó
 * thuộc Infrastructure Layer.
 */
data class PackageExportPayload(
    val descriptor: PackageDescriptor,
    val contents: List<Content>,
    val learningItems: List<LearningItem>,
    val libraries: List<ContentLibrary> = emptyList()
)
