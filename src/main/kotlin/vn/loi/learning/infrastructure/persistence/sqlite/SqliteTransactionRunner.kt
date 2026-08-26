package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.TransactionRunner

class SqliteTransactionRunner(
    private val database: LearningEngineDatabase
) : TransactionRunner {
    override fun <T> runInTransaction(block: () -> T): T {
        return database.transactionWithResult {
            block()
        }
    }
}
