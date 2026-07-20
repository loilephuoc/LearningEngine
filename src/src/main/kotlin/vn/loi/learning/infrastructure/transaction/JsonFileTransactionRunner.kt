package vn.loi.learning.infrastructure.transaction

import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.application.port.TransactionRunner

class JsonFileTransactionRunner(
    private val paths: List<Path>
) : TransactionRunner {

    override fun <T> runInTransaction(
        block: () -> T
    ): T {
        val snapshots = paths.associateWith { path ->
            if (Files.exists(path)) Files.readAllBytes(path) else null
        }

        return try {
            block()
        } catch (failure: Throwable) {
            snapshots.forEach { (path, snapshot) ->
                if (snapshot == null) {
                    Files.deleteIfExists(path)
                } else {
                    path.parent?.let(Files::createDirectories)
                    Files.write(path, snapshot)
                }
            }

            throw failure
        }
    }
}
