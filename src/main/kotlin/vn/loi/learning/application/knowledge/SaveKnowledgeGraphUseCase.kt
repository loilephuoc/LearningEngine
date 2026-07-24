package vn.loi.learning.application.knowledge

import vn.loi.learning.domain.knowledge.model.KnowledgeGraph
import vn.loi.learning.domain.knowledge.repository.KnowledgeGraphRepository

/**
 * Use case lưu knowledge graph.
 *
 * - Không expose record hay JSON ra ngoài application boundary.
 * - Graph vẫn immutable sau khi lưu.
 */
class SaveKnowledgeGraphUseCase(
    private val repository: KnowledgeGraphRepository
) {
    fun execute(graph: KnowledgeGraph) {
        repository.save(graph)
    }
}
