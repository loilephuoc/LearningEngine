package vn.loi.learning.application.library.command

/**
 * Result kiểu định danh (typed result) đại diện cho kết quả thực thi của mọi Canonical Library Command.
 * Giúp Presentation / Desktop UI phân nhánh trực tiếp dựa trên kiểu dữ liệu mà không cần parse chuỗi exception.
 */
sealed interface LibraryCommandResult<out T> {

    data class Success<out T>(val value: T) : LibraryCommandResult<T>

    data class LibraryNotFound(val libraryId: String) : LibraryCommandResult<Nothing>

    data class PackageNotFound(val packageId: String) : LibraryCommandResult<Nothing>

    data class CollectionNotFound(val collectionId: String) : LibraryCommandResult<Nothing>

    data class InvalidState(val message: String) : LibraryCommandResult<Nothing>

    data class DuplicateCollection(val collectionName: String) : LibraryCommandResult<Nothing>

    data class AlreadyAssigned(val packageId: String, val collectionId: String) : LibraryCommandResult<Nothing>

    data class NotAssigned(val packageId: String, val collectionId: String) : LibraryCommandResult<Nothing>

    data class ActiveVersionConflict(val packageId: String) : LibraryCommandResult<Nothing>

    data class CrossLibraryConflict(val message: String) : LibraryCommandResult<Nothing>

    data class PersistenceFailure(val message: String, val cause: Throwable? = null) : LibraryCommandResult<Nothing>
}
