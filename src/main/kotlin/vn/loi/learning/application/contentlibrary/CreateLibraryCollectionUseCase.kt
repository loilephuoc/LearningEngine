package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.LibraryCollectionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionDescriptor

/**
 * Tạo một LibraryCollection thuộc ContentLibrary đã tồn tại.
 */
class CreateLibraryCollectionUseCase(
    private val contentLibraryRepository: ContentLibraryRepository,
    private val libraryCollectionRepository: LibraryCollectionRepository,
    private val transactionRunner: TransactionRunner
) {

    fun execute(
        command: CreateLibraryCollectionCommand
    ): LibraryCollection =
        transactionRunner.runInTransaction {
            requireLibraryExists(
                command
            )

            requireCollectionDoesNotExist(
                command
            )

            val collection =
                LibraryCollection(
                    id = command.collectionId,
                    libraryId = command.libraryId,
                    descriptor =
                        LibraryCollectionDescriptor(
                            name = command.name
                        )
                )

            libraryCollectionRepository.save(
                collection
            )

            collection
        }

    private fun requireLibraryExists(
        command: CreateLibraryCollectionCommand
    ) {
        if (
            contentLibraryRepository.findById(
                command.libraryId
            ) == null
        ) {
            throw ContentLibraryNotFoundException(
                command.libraryId
            )
        }
    }

    private fun requireCollectionDoesNotExist(
        command: CreateLibraryCollectionCommand
    ) {
        if (
            libraryCollectionRepository.findById(
                command.collectionId
            ) != null
        ) {
            throw LibraryCollectionAlreadyExistsException(
                command.collectionId
            )
        }
    }
}