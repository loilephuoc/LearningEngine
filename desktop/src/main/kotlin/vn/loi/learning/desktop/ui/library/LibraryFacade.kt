package vn.loi.learning.desktop.ui.library

import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.application.library.query.LibraryNavigationTree
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.infrastructure.LearningApplicationContext

/**
 * Presentation Facade cho Desktop Library.
 *
 * Đóng vai trò ranh giới tiêu thụ (consumer boundary) duy nhất giữa Desktop UI và Application Layer.
 * Giao tiếp với Application Query Boundary (`LibraryQueryService`) và Command Boundary (`LibraryCommandService`).
 * Tuyệt đối KHÔNG truy cập trực tiếp tới repository, store, json file, hay transaction runner.
 */
open class LibraryFacade(
    private val applicationContext: LearningApplicationContext,
    val libraryId: LibraryId
) {
    open fun loadNavigationTree(): LibraryNavigationTree {
        val queryService = applicationContext.libraryQuery
            ?: throw LibraryServiceUnavailableException()
        return queryService.getNavigationTree(libraryId)
            ?: throw LibraryNotFoundException(libraryId)
    }

    open fun createCollection(
        name: CollectionName,
        description: String = ""
    ): LibraryCommandResult<Collection> {
        val commandService = applicationContext.libraryCommand
            ?: throw LibraryServiceUnavailableException()
        return commandService.createCollection(
            libraryId = libraryId,
            name = name,
            description = description
        )
    }

    open fun renameCollection(
        collectionId: CollectionId,
        newName: CollectionName
    ): LibraryCommandResult<Collection> {
        val commandService = applicationContext.libraryCommand
            ?: throw LibraryServiceUnavailableException()
        return commandService.renameCollection(
            libraryId = libraryId,
            collectionId = collectionId,
            newName = newName
        )
    }

    open fun deleteCollection(
        collectionId: CollectionId
    ): LibraryCommandResult<Collection> {
        val commandService = applicationContext.libraryCommand
            ?: throw LibraryServiceUnavailableException()
        return commandService.deleteCollection(
            libraryId = libraryId,
            collectionId = collectionId
        )
    }

    open fun assignPackageToCollection(
        collectionId: CollectionId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<Collection> {
        val commandService = applicationContext.libraryCommand
            ?: throw LibraryServiceUnavailableException()
        return commandService.assignPackageToCollection(
            libraryId = libraryId,
            collectionId = collectionId,
            installedPackageId = installedPackageId
        )
    }

    open fun removePackageFromCollection(
        collectionId: CollectionId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<Collection> {
        val commandService = applicationContext.libraryCommand
            ?: throw LibraryServiceUnavailableException()
        return commandService.removePackageFromCollection(
            libraryId = libraryId,
            collectionId = collectionId,
            installedPackageId = installedPackageId
        )
    }

    open fun archivePackage(
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<InstalledPackage> {
        val commandService = applicationContext.libraryCommand
            ?: throw LibraryServiceUnavailableException()
        return commandService.archivePackage(
            libraryId = libraryId,
            installedPackageId = installedPackageId
        )
    }

    open fun restorePackage(
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<InstalledPackage> {
        val commandService = applicationContext.libraryCommand
            ?: throw LibraryServiceUnavailableException()
        return commandService.restorePackage(
            libraryId = libraryId,
            installedPackageId = installedPackageId
        )
    }
}
