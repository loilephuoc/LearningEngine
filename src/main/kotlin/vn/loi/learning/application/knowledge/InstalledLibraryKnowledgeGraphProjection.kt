package vn.loi.learning.application.knowledge

import vn.loi.learning.domain.knowledge.model.KnowledgeGraph
import vn.loi.learning.domain.knowledge.model.KnowledgeGraphFactory
import vn.loi.learning.domain.knowledge.model.KnowledgeGraphValidationResult
import vn.loi.learning.domain.knowledge.model.KnowledgeNode
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeId
import vn.loi.learning.domain.knowledge.model.KnowledgeNodeKind
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.InstalledPackageRepository

/**
 * Service chiếu (projection) installed library packages thành KnowledgeGraph.
 *
 * - Chỉ project các package ở trạng thái ACTIVE.
 * - Mỗi package trở thành một KnowledgeNode có kind = PACKAGE.
 * - Node id = packageId.value (canonical package identity từ LP-004R).
 * - displayName = package.name.value.
 * - Không chứa learner state.
 * - Graph trả về là immutable; edges rỗng (flat projection, không có hierarchy).
 *
 * Kết quả là read-only snapshot tại thời điểm gọi.
 */
class InstalledLibraryKnowledgeGraphProjection(
    private val installedPackageRepository: InstalledPackageRepository
) {
    /**
     * Project toàn bộ installed active packages thành KnowledgeGraph.
     *
     * Trả về graph rỗng nếu không có package nào đang ACTIVE.
     * Trả về [KnowledgeGraph] hợp lệ — không bao giờ ném exception cho dữ liệu bình thường.
     */
    fun project(): KnowledgeGraph {
        val packages = installedPackageRepository.findAllByState(PackageState.ACTIVE)
        val nodes = packages.mapNotNull { pkg -> pkg.toKnowledgeNode() }
        return when (val result = KnowledgeGraphFactory.build(nodes, emptyList())) {
            is KnowledgeGraphValidationResult.Valid -> result.graph
            is KnowledgeGraphValidationResult.Invalid -> {
                // Nếu có duplicate packageId (không nên xảy ra do InstalledPackageRepository constraint),
                // deduplicate và thử lại với nodes unique.
                val deduplicated = nodes.distinctBy { it.id.value }
                val retry = KnowledgeGraphFactory.build(deduplicated, emptyList())
                (retry as? KnowledgeGraphValidationResult.Valid)?.graph
                    ?: KnowledgeGraphFactory.build(emptyList(), emptyList())
                        .let { (it as KnowledgeGraphValidationResult.Valid).graph }
            }
        }
    }

    private fun InstalledPackage.toKnowledgeNode(): KnowledgeNode? =
        try {
            KnowledgeNode(
                id = KnowledgeNodeId(packageId.value),
                kind = KnowledgeNodeKind.PACKAGE,
                displayName = name.value
            )
        } catch (_: IllegalArgumentException) {
            // packageId.value bị rỗng hoặc blank — bỏ qua node này (defensive)
            null
        }
}
