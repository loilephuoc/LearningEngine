package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.LibraryCollectionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.library.model.LibraryCollection

/**
 * Gỡ tham chiếu PackageId khỏi LibraryCollection.
 *
 * ContentPackage không bị xóa. Use case chỉ thay đổi quan hệ logic
 * giữa collection và package.
 */
class DetachPackageFromLibraryCollectionUseCase(
    private val libraryCollectionRepository: LibraryCollectionRepository,
    private val transactionRunner: TransactionRunner
) {

    fun execute(
        command: DetachPackageFromLibraryCollectionCommand
    ): LibraryCollection =
        transactionRunner.runInTransaction {
            val collection =
                libraryCollectionRepository.findById(
                    command.collectionId
                ) ?: throw LibraryCollectionNotFoundException(
                    command.collectionId
                )

            val updatedCollection =
                collection.detach(
                    command.packageId
                )

            if (updatedCollection !== collection) {
                libraryCollectionRepository.save(
                    updatedCollection
                )
            }

            updatedCollection
        }
}