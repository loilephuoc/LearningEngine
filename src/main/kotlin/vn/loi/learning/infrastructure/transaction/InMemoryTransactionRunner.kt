package vn.loi.learning.infrastructure.transaction

import vn.loi.learning.application.port.TransactionRunner

/**
 * Transaction runner dành cho các repository in-memory.
 *
 * Hiện tại Engine chạy tuần tự nên implementation chỉ thực thi block.
 * Adapter có transaction thực sự như SQLite hoặc PostgreSQL sẽ thay thế class này.
 */
class InMemoryTransactionRunner : TransactionRunner {

    override fun <T> runInTransaction(
        block: () -> T
    ): T = block()
}