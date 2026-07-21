package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.LibraryCollectionRepository
import vn.loi.learning.application.port.TransactionRunner

/**
 * Xóa một LibraryCollection đã tồn tại.
 *
 * Việc xóa collection không xóa ContentPackage.
 * Collection chỉ quản lý các tham chiếu PackageId.
 */
class DeleteLibraryCollectionUseCase(
    private val libraryCollectionRepository: LibraryCollectionRepository,
    private val transactionRunner: TransactionRunner
) {

    fun execute(
        command: DeleteLibraryCollectionCommand
    ) {
        transactionRunner.runInTransaction {
            requireCollectionExists(
                command
            )

            libraryCollectionRepository.deleteById(
                command.collectionId
            )
        }
    }

    private fun requireCollectionExists(
        command: DeleteLibraryCollectionCommand
    ) {
        if (
            libraryCollectionRepository.findById(
                command.collectionId
            ) == null
        ) {
            throw LibraryCollectionNotFoundException(
                command.collectionId
            )
        }
    }
}