package vn.loi.learning.application.port

/**
 * Thực thi một khối nghiệp vụ trong transaction.
 *
 * Application chỉ biết rằng toàn bộ block phải được thực hiện như một đơn vị.
 * Chi tiết begin, commit và rollback thuộc về Infrastructure.
 */
interface TransactionRunner {

    fun <T> runInTransaction(
        block: () -> T
    ): T
}