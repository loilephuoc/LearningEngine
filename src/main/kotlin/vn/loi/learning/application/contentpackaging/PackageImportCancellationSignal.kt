package vn.loi.learning.application.contentpackaging

/**
 * Tín hiệu hủy thao tác import package.
 * Cho phép hủy hợp tác (cooperative cancellation) giữa UI thread và background worker.
 */
class PackageImportCancellationSignal {
    @Volatile
    private var cancelled = false

    fun cancel() {
        cancelled = true
    }

    fun isCancelled(): Boolean = cancelled

    fun checkCancelled() {
        if (cancelled) {
            throw PackageImportCancelledException()
        }
    }
}

class PackageImportCancelledException(
    message: String = "Package import was cancelled by user."
) : RuntimeException(message)
