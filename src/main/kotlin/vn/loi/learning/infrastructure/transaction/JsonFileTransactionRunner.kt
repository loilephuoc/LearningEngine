package vn.loi.learning.infrastructure.transaction

import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.application.port.TransactionRunner

/**
 * Transaction runner dành cho các repository lưu bằng JSON file.
 *
 * Trước khi thực thi transaction, runner chụp snapshot của toàn bộ file
 * được quản lý.
 *
 * Khi block thất bại:
 * - file đã tồn tại được khôi phục về nội dung cũ;
 * - file mới tạo trong transaction bị xóa;
 * - rollback tiếp tục với các file còn lại ngay cả khi một file rollback lỗi;
 * - lỗi nghiệp vụ ban đầu luôn được giữ nguyên;
 * - lỗi rollback được gắn vào suppressedExceptions của lỗi ban đầu.
 */
class JsonFileTransactionRunner(
    paths: List<Path>
) : TransactionRunner {

    private val paths: List<Path> =
        paths
            .map { path ->
                path.toAbsolutePath().normalize()
            }
            .distinct()

    override fun <T> runInTransaction(
        block: () -> T
    ): T {
        val snapshots =
            paths.associateWith { path ->
                if (Files.exists(path)) {
                    Files.readAllBytes(path)
                } else {
                    null
                }
            }

        return try {
            block()
        } catch (failure: Throwable) {
            rollback(
                snapshots =
                    snapshots,
                transactionFailure =
                    failure
            )

            throw failure
        }
    }

    private fun rollback(
        snapshots: Map<Path, ByteArray?>,
        transactionFailure: Throwable
    ) {
        snapshots.forEach { (path, snapshot) ->
            try {
                restoreSnapshot(
                    path =
                        path,
                    snapshot =
                        snapshot
                )
            } catch (rollbackFailure: Throwable) {
                transactionFailure.addSuppressed(
                    rollbackFailure
                )
            }
        }
    }

    private fun restoreSnapshot(
        path: Path,
        snapshot: ByteArray?
    ) {
        if (snapshot == null) {
            Files.deleteIfExists(
                path
            )

            return
        }

        path.parent?.let { parent ->
            Files.createDirectories(
                parent
            )
        }

        Files.write(
            path,
            snapshot
        )
    }
}