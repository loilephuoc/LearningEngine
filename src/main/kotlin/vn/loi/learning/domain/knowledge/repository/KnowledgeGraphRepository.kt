package vn.loi.learning.domain.knowledge.repository

import vn.loi.learning.domain.knowledge.model.KnowledgeGraph

/**
 * Repository interface quản lý lưu trữ và truy vấn KnowledgeGraph.
 *
 * Domain không phụ thuộc vào infrastructure; interface này được implement ở infrastructure layer.
 */
interface KnowledgeGraphRepository {
    /**
     * Lấy knowledge graph hiện tại.
     * Trả về graph rỗng nếu chưa có dữ liệu nào được lưu.
     */
    fun get(): KnowledgeGraph

    /**
     * Lưu knowledge graph.
     */
    fun save(graph: KnowledgeGraph)
}
