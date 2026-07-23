package vn.loi.learning.application.library.query

/**
 * Node phân cấp đại diện cho Collection cùng danh sách tóm tắt InstalledPackageSummary
 * của các gói đang ACTIVE được gán vào Collection đó.
 */
data class CollectionNode(
    val collection: CollectionSummary,
    val assignedPackages: List<InstalledPackageSummary>
)
