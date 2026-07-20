package vn.loi.learning.application.port

import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState

/**
 * Read port dùng để truy vấn tập hợp MemoryState.
 *
 * Port này tách biệt với MemoryStateRepository vì:
 * - Repository quản lý từng aggregate cụ thể;
 * - Query phục vụ các use case đọc và projection;
 * - caller không cần biết danh sách LearningItemId;
 * - Application Layer không phải gọi find từng item.
 *
 * Kết quả chỉ bao gồm MemoryState thuộc learner được yêu cầu.
 */
fun interface MemoryStateQuery {

    fun findAll(
        learnerId: LearnerId
    ): List<MemoryState>
}