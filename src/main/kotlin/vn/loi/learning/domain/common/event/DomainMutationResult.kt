package vn.loi.learning.domain.common.event

/**
 * Kiểu kết quả bọc Aggregate đã biến đổi state cùng với Domain Event tương ứng được sinh ra.
 */
data class DomainMutationResult<out A, out E : DomainEvent>(
    val aggregate: A,
    val event: E
)
