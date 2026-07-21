package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.LibraryCollectionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.library.model.LibraryCollection

/**
 * Gắn tham chiếu của một ContentPackage đã tồn tại vào LibraryCollection.
 *
 * Collection chỉ lưu PackageId, không nhúng aggregate ContentPackage.
 */
class AttachPackageToLibraryCollectionUseCase(
    private val libraryCollectionRepository: LibraryCollectionRepository,
    private val contentPackageRepository: ContentPackageRepository,
    private val transactionRunner: TransactionRunner
) {

    fun execute(
        command: AttachPackageToLibraryCollectionCommand
    ): LibraryCollection =
        transactionRunner.runInTransaction {
            val collection =
                libraryCollectionRepository.findById(
                    command.collectionId
                ) ?: throw LibraryCollectionNotFoundException(
                    command.collectionId
                )

            if (
                contentPackageRepository.findById(
                    command.packageId
                ) == null
            ) {
                throw ContentPackageNotFoundException(
                    command.packageId
                )
            }

            val updatedCollection =
                collection.attach(
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