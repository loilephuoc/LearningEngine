package vn.loi.learning.application.library.command

import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.repository.CollectionRepository
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.library.repository.LibraryRepository
import vn.loi.learning.domain.library.service.LibraryDomainCoordinator

/**
 * Command Boundary chính thức cho Canonical Library Platform.
 * Quản lý các thao tác ghi / thay đổi trạng thái trong Library (Archive, Restore, Collection lifecycle, Assignment).
 *
 * Nguyên tắc:
 * - Phụ thuộc duy nhất vào domain.library và application abstractions.
 * - KHÔNG import infrastructure, adapter, hoặc desktop.
 * - Trả về kết quả kiểu định danh (LibraryCommandResult) cho mọi kịch bản.
 * - Đảm bảo tính nguyên tử (atomic transaction) và không để lại trạng thái bán phần khi lỗi.
 */
class LibraryCommandService(
    private val libraryRepository: LibraryRepository,
    private val installedPackageRepository: InstalledPackageRepository,
    private val collectionRepository: CollectionRepository,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Chuyển một gói nội dung sang trạng thái ARCHIVED.
     * - Chỉ archive gói đang ở trạng thái ACTIVE.
     * - Giữ nguyên toàn bộ learner state.
     */
    fun archivePackage(
        libraryId: LibraryId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<InstalledPackage> {
        val library = libraryRepository.findById(libraryId)
            ?: return LibraryCommandResult.LibraryNotFound(libraryId.value)

        val installedPackage = installedPackageRepository.findById(installedPackageId)
            ?: return LibraryCommandResult.PackageNotFound(installedPackageId.value)

        if (installedPackage.libraryId != libraryId) {
            return LibraryCommandResult.CrossLibraryConflict(
                "Package (${installedPackageId.value}) belongs to library (${installedPackage.libraryId.value}), not target library (${libraryId.value})."
            )
        }

        if (installedPackage.state != PackageState.ACTIVE) {
            return LibraryCommandResult.InvalidState(
                "Cannot archive package (${installedPackageId.value}): package state is ${installedPackage.state}, expected ACTIVE."
            )
        }

        return try {
            transactionRunner.runInTransaction {
                val mutation = installedPackage.archive()
                val updatedPackage = mutation.aggregate
                installedPackageRepository.save(updatedPackage)
                LibraryCommandResult.Success(updatedPackage)
            }
        } catch (e: Exception) {
            LibraryCommandResult.PersistenceFailure("Failed to archive package: ${e.message}", e)
        }
    }

    /**
     * Khôi phục một gói nội dung từ ARCHIVED về ACTIVE.
     * - Chỉ restore gói đang ở trạng thái ARCHIVED.
     * - Thắt chặt quy tắc Single Active Version trong Library.
     */
    fun restorePackage(
        libraryId: LibraryId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<InstalledPackage> {
        val library = libraryRepository.findById(libraryId)
            ?: return LibraryCommandResult.LibraryNotFound(libraryId.value)

        val installedPackage = installedPackageRepository.findById(installedPackageId)
            ?: return LibraryCommandResult.PackageNotFound(installedPackageId.value)

        if (installedPackage.libraryId != libraryId) {
            return LibraryCommandResult.CrossLibraryConflict(
                "Package (${installedPackageId.value}) belongs to library (${installedPackage.libraryId.value}), not target library (${libraryId.value})."
            )
        }

        if (installedPackage.state != PackageState.ARCHIVED) {
            return LibraryCommandResult.InvalidState(
                "Cannot restore package (${installedPackageId.value}): package state is ${installedPackage.state}, expected ARCHIVED."
            )
        }

        val allPackagesInLibrary = installedPackageRepository.findAllByLibraryId(libraryId)
        val otherPackages = allPackagesInLibrary.filterNot { it.id == installedPackageId }

        if (otherPackages.any { it.packageId == installedPackage.packageId && it.isActive }) {
            return LibraryCommandResult.ActiveVersionConflict(installedPackage.packageId.value)
        }

        return try {
            transactionRunner.runInTransaction {
                val restoreResult = LibraryDomainCoordinator.restorePackage(
                    library = library,
                    installedPackage = installedPackage,
                    installedPackagesInLibrary = allPackagesInLibrary
                )
                installedPackageRepository.save(restoreResult.installedPackage)
                LibraryCommandResult.Success(restoreResult.installedPackage)
            }
        } catch (e: IllegalStateException) {
            if (e.message?.contains("ACTIVE InstalledPackage") == true) {
                LibraryCommandResult.ActiveVersionConflict(installedPackage.packageId.value)
            } else {
                LibraryCommandResult.InvalidState(e.message ?: "Invalid state for restore operation.")
            }
        } catch (e: Exception) {
            LibraryCommandResult.PersistenceFailure("Failed to restore package: ${e.message}", e)
        }
    }

    /**
     * Tạo một bộ sưu tập (Collection) mới.
     * - Tên bộ sưu tập phải duy nhất trong Library.
     */
    fun createCollection(
        libraryId: LibraryId,
        name: CollectionName,
        description: String = "",
        collectionId: CollectionId = CollectionId("col-${java.util.UUID.randomUUID()}")
    ): LibraryCommandResult<Collection> {
        val library = libraryRepository.findById(libraryId)
            ?: return LibraryCommandResult.LibraryNotFound(libraryId.value)

        val existingCollections = collectionRepository.findAllByLibraryId(libraryId)

        if (existingCollections.any { it.libraryId == libraryId && it.isActive && it.name == name }) {
            return LibraryCommandResult.DuplicateCollection(name.trimmedValue)
        }

        return try {
            transactionRunner.runInTransaction {
                val mutation = LibraryDomainCoordinator.createCollection(
                    library = library,
                    id = collectionId,
                    name = name,
                    existingCollections = existingCollections,
                    description = description
                )
                val newCollection = mutation.aggregate
                collectionRepository.save(newCollection)
                LibraryCommandResult.Success(newCollection)
            }
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("already exists") == true) {
                LibraryCommandResult.DuplicateCollection(name.trimmedValue)
            } else {
                LibraryCommandResult.InvalidState(e.message ?: "Invalid collection creation data.")
            }
        } catch (e: Exception) {
            LibraryCommandResult.PersistenceFailure("Failed to create collection: ${e.message}", e)
        }
    }

    /**
     * Đổi tên một Collection hiện có.
     * - Tên mới phải duy nhất trong Library.
     * - Giữ nguyên CollectionId.
     */
    fun renameCollection(
        libraryId: LibraryId,
        collectionId: CollectionId,
        newName: CollectionName
    ): LibraryCommandResult<Collection> {
        val library = libraryRepository.findById(libraryId)
            ?: return LibraryCommandResult.LibraryNotFound(libraryId.value)

        val collection = collectionRepository.findById(collectionId)
            ?: return LibraryCommandResult.CollectionNotFound(collectionId.value)

        if (collection.libraryId != libraryId) {
            return LibraryCommandResult.CrossLibraryConflict(
                "Collection (${collectionId.value}) belongs to library (${collection.libraryId.value}), not target library (${libraryId.value})."
            )
        }

        if (!collection.isActive) {
            return LibraryCommandResult.InvalidState("Cannot rename collection (${collectionId.value}): collection is DELETED.")
        }

        val existingCollections = collectionRepository.findAllByLibraryId(libraryId)
        val otherCollections = existingCollections.filterNot { it.id == collectionId }

        if (otherCollections.any { it.isActive && it.name == newName }) {
            return LibraryCommandResult.DuplicateCollection(newName.trimmedValue)
        }

        return try {
            transactionRunner.runInTransaction {
                val mutation = LibraryDomainCoordinator.renameCollection(
                    library = library,
                    collection = collection,
                    newName = newName,
                    existingCollections = existingCollections
                )
                val updatedCollection = mutation.aggregate
                collectionRepository.save(updatedCollection)
                LibraryCommandResult.Success(updatedCollection)
            }
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("already exists") == true) {
                LibraryCommandResult.DuplicateCollection(newName.trimmedValue)
            } else {
                LibraryCommandResult.InvalidState(e.message ?: "Invalid collection rename data.")
            }
        } catch (e: Exception) {
            LibraryCommandResult.PersistenceFailure("Failed to rename collection: ${e.message}", e)
        }
    }

    /**
     * Xóa mềm một Collection (chuyển sang trạng thái DELETED).
     * - Không xóa hay đụng chạm các InstalledPackage trong Library.
     */
    fun deleteCollection(
        libraryId: LibraryId,
        collectionId: CollectionId
    ): LibraryCommandResult<Collection> {
        val library = libraryRepository.findById(libraryId)
            ?: return LibraryCommandResult.LibraryNotFound(libraryId.value)

        val collection = collectionRepository.findById(collectionId)
            ?: return LibraryCommandResult.CollectionNotFound(collectionId.value)

        if (collection.libraryId != libraryId) {
            return LibraryCommandResult.CrossLibraryConflict(
                "Collection (${collectionId.value}) belongs to library (${collection.libraryId.value}), not target library (${libraryId.value})."
            )
        }

        if (!collection.isActive) {
            return LibraryCommandResult.InvalidState("Cannot delete collection (${collectionId.value}): collection is already DELETED.")
        }

        return try {
            transactionRunner.runInTransaction {
                val mutation = collection.delete()
                val deletedCollection = mutation.aggregate
                collectionRepository.save(deletedCollection)
                LibraryCommandResult.Success(deletedCollection)
            }
        } catch (e: Exception) {
            LibraryCommandResult.PersistenceFailure("Failed to delete collection: ${e.message}", e)
        }
    }

    /**
     * Gán một gói nội dung vào Collection.
     * - Gói phải đang ở trạng thái ACTIVE.
     * - Gói và Collection phải thuộc cùng Library.
     */
    fun assignPackageToCollection(
        libraryId: LibraryId,
        collectionId: CollectionId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<Collection> {
        val library = libraryRepository.findById(libraryId)
            ?: return LibraryCommandResult.LibraryNotFound(libraryId.value)

        val collection = collectionRepository.findById(collectionId)
            ?: return LibraryCommandResult.CollectionNotFound(collectionId.value)

        val installedPackage = installedPackageRepository.findById(installedPackageId)
            ?: return LibraryCommandResult.PackageNotFound(installedPackageId.value)

        if (collection.libraryId != libraryId || installedPackage.libraryId != libraryId) {
            return LibraryCommandResult.CrossLibraryConflict(
                "Cross library assignment forbidden: collection library is ${collection.libraryId.value}, package library is ${installedPackage.libraryId.value}, target library is ${libraryId.value}."
            )
        }

        if (!collection.isActive) {
            return LibraryCommandResult.InvalidState("Cannot assign package to DELETED collection (${collectionId.value}).")
        }

        if (!installedPackage.isActive) {
            return LibraryCommandResult.InvalidState("Cannot assign package (${installedPackageId.value}) to collection: package state is ${installedPackage.state}, expected ACTIVE.")
        }

        if (collection.containsPackage(installedPackageId)) {
            return LibraryCommandResult.AlreadyAssigned(
                packageId = installedPackageId.value,
                collectionId = collectionId.value
            )
        }

        return try {
            transactionRunner.runInTransaction {
                val registeredLibrary = if (!library.hasPackage(installedPackageId)) {
                    val updated = library.registerEntry(installedPackageId, installedPackage.packageId)
                    libraryRepository.save(updated)
                    updated
                } else {
                    library
                }
                val mutation = collection.assignPackage(installedPackage, registeredLibrary)
                val updatedCollection = mutation.aggregate
                collectionRepository.save(updatedCollection)
                LibraryCommandResult.Success(updatedCollection)
            }
        } catch (e: IllegalStateException) {
            if (e.message?.contains("already assigned") == true) {
                LibraryCommandResult.AlreadyAssigned(installedPackageId.value, collectionId.value)
            } else {
                LibraryCommandResult.InvalidState(e.message ?: "Invalid state for package assignment.")
            }
        } catch (e: IllegalArgumentException) {
            LibraryCommandResult.InvalidState(e.message ?: "Invalid argument for package assignment.")
        } catch (e: Exception) {
            LibraryCommandResult.PersistenceFailure("Failed to assign package to collection: ${e.message}", e)
        }
    }

    /**
     * Gỡ gán một gói nội dung khỏi Collection.
     */
    fun removePackageFromCollection(
        libraryId: LibraryId,
        collectionId: CollectionId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<Collection> {
        val library = libraryRepository.findById(libraryId)
            ?: return LibraryCommandResult.LibraryNotFound(libraryId.value)

        val collection = collectionRepository.findById(collectionId)
            ?: return LibraryCommandResult.CollectionNotFound(collectionId.value)

        if (collection.libraryId != libraryId) {
            return LibraryCommandResult.CrossLibraryConflict(
                "Collection (${collectionId.value}) belongs to library (${collection.libraryId.value}), not target library (${libraryId.value})."
            )
        }

        if (!collection.isActive) {
            return LibraryCommandResult.InvalidState("Cannot remove package from DELETED collection (${collectionId.value}).")
        }

        if (!collection.containsPackage(installedPackageId)) {
            return LibraryCommandResult.NotAssigned(
                packageId = installedPackageId.value,
                collectionId = collectionId.value
            )
        }

        return try {
            transactionRunner.runInTransaction {
                val mutation = collection.removePackage(installedPackageId)
                val updatedCollection = mutation.aggregate
                collectionRepository.save(updatedCollection)
                LibraryCommandResult.Success(updatedCollection)
            }
        } catch (e: IllegalStateException) {
            if (e.message?.contains("not assigned") == true) {
                LibraryCommandResult.NotAssigned(installedPackageId.value, collectionId.value)
            } else {
                LibraryCommandResult.InvalidState(e.message ?: "Invalid state for package unassignment.")
            }
        } catch (e: Exception) {
            LibraryCommandResult.PersistenceFailure("Failed to remove package from collection: ${e.message}", e)
        }
    }
}
