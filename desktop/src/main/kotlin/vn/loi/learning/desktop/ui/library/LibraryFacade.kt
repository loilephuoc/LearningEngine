package vn.loi.learning.desktop.ui.library

import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.application.library.command.LibraryCommandService
import vn.loi.learning.application.library.query.LibraryNavigationTree
import vn.loi.learning.application.library.query.LibraryQueryService
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId

/**
 * Presentation Facade cho Desktop Library.
 *
 * Đóng vai trò ranh giới tiêu thụ (consumer boundary) duy nhất giữa Desktop UI và Application Layer.
 * Tiếp nhận trực tiếp các cổng/dịch vụ Application (`LibraryQueryService` & `LibraryCommandService`) thông qua Dependency Injection.
 * Tuyệt đối KHÔNG phụ thuộc trực tiếp vào các lớp ngoài ranh giới Application.
 */
open class LibraryFacade(
    private val queryService: LibraryQueryService?,
    private val commandService: LibraryCommandService?,
    val libraryId: LibraryId
) {
    open fun loadNavigationTree(): LibraryNavigationTree {
        val query = queryService
            ?: throw LibraryServiceUnavailableException()
        return query.getNavigationTree(libraryId)
            ?: throw LibraryNotFoundException(libraryId)
    }

    open fun createCollection(
        name: CollectionName,
        description: String = ""
    ): LibraryCommandResult<Collection> {
        val command = commandService
            ?: throw LibraryServiceUnavailableException()
        return command.createCollection(
            libraryId = libraryId,
            name = name,
            description = description
        )
    }

    open fun renameCollection(
        collectionId: CollectionId,
        newName: CollectionName
    ): LibraryCommandResult<Collection> {
        val command = commandService
            ?: throw LibraryServiceUnavailableException()
        return command.renameCollection(
            libraryId = libraryId,
            collectionId = collectionId,
            newName = newName
        )
    }

    open fun deleteCollection(
        collectionId: CollectionId
    ): LibraryCommandResult<Collection> {
        val command = commandService
            ?: throw LibraryServiceUnavailableException()
        return command.deleteCollection(
            libraryId = libraryId,
            collectionId = collectionId
        )
    }

    open fun assignPackageToCollection(
        collectionId: CollectionId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<Collection> {
        val command = commandService
            ?: throw LibraryServiceUnavailableException()
        return command.assignPackageToCollection(
            libraryId = libraryId,
            collectionId = collectionId,
            installedPackageId = installedPackageId
        )
    }

    open fun removePackageFromCollection(
        collectionId: CollectionId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<Collection> {
        val command = commandService
            ?: throw LibraryServiceUnavailableException()
        return command.removePackageFromCollection(
            libraryId = libraryId,
            collectionId = collectionId,
            installedPackageId = installedPackageId
        )
    }

    open fun archivePackage(
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<InstalledPackage> {
        val command = commandService
            ?: throw LibraryServiceUnavailableException()
        return command.archivePackage(
            libraryId = libraryId,
            installedPackageId = installedPackageId
        )
    }

    open fun restorePackage(
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<InstalledPackage> {
        val command = commandService
            ?: throw LibraryServiceUnavailableException()
        return command.restorePackage(
            libraryId = libraryId,
            installedPackageId = installedPackageId
        )
    }
}
