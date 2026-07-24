package vn.loi.learning.application.knowledge

import vn.loi.learning.domain.knowledge.model.KnowledgeGraph
import vn.loi.learning.domain.knowledge.repository.KnowledgeGraphRepository

/**
 * Use case lấy knowledge graph hiện tại.
 *
 * - Không expose record hay JSON ra ngoài application boundary.
 * - Trả về graph rỗng nếu chưa có dữ liệu nào được lưu.
 */
class GetKnowledgeGraphUseCase(
    private val repository: KnowledgeGraphRepository
) {
    fun execute(): KnowledgeGraph = repository.get()
}
