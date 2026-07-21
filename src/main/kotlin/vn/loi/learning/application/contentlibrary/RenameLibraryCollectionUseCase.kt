package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.LibraryCollectionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.library.model.LibraryCollection

/**
 * Đổi tên LibraryCollection mà không thay đổi identity, library sở hữu
 * hoặc các package reference hiện có.
 */
class RenameLibraryCollectionUseCase(
    private val libraryCollectionRepository: LibraryCollectionRepository,
    private val transactionRunner: TransactionRunner
) {

    fun execute(
        command: RenameLibraryCollectionCommand
    ): LibraryCollection =
        transactionRunner.runInTransaction {
            val collection =
                libraryCollectionRepository.findById(
                    command.collectionId
                ) ?: throw LibraryCollectionNotFoundException(
                    command.collectionId
                )

            val renamedCollection =
                collection.rename(
                    name = command.name
                )

            if (renamedCollection !== collection) {
                libraryCollectionRepository.save(
                    renamedCollection
                )
            }

            renamedCollection
        }
}