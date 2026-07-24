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
 * Nguyên tắc tuân thủ (R1 - R3):
 * 1. Toàn bộ chu trình `load -> validate -> mutate -> save` diễn ra hoàn toàn TRONG transaction scope (`runInTransaction`).
 * 2. Tuyệt đối KHÔNG parse hay so sánh chuỗi exception message. Invariant checks được thực hiện bằng typed state/query trước khi mutate.
 * 3. Assignment KHÔNG đóng vai trò repair workflow: không tự đăng ký package vào Library, không save Library nếu package chưa được đăng ký.
 */
class LibraryCommandService(
    private val libraryRepository: LibraryRepository,
    private val installedPackageRepository: InstalledPackageRepository,
    private val collectionRepository: CollectionRepository,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Chuyển một gói nội dung sang trạng thái ARCHIVED.
     * - Chỉ archive gói đang ở trạng thái ACTIVE và đã được đăng ký trong Library.
     * - Giữ nguyên toàn bộ learner state.
     */
    fun archivePackage(
        libraryId: LibraryId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<InstalledPackage> {
        return try {
            transactionRunner.runInTransaction {
                val library = libraryRepository.findById(libraryId)
                    ?: return@runInTransaction LibraryCommandResult.LibraryNotFound(libraryId.value)

                val installedPackage = installedPackageRepository.findById(installedPackageId)
                    ?: return@runInTransaction LibraryCommandResult.PackageNotFound(installedPackageId.value)

                if (installedPackage.libraryId != libraryId) {
                    return@runInTransaction LibraryCommandResult.CrossLibraryConflict(
                        "Package (${installedPackageId.value}) belongs to library (${installedPackage.libraryId.value}), not target library (${libraryId.value})."
                    )
                }

                if (!library.hasPackage(installedPackageId)) {
                    return@runInTransaction LibraryCommandResult.PackageNotRegisteredInLibrary(
                        packageId = installedPackageId.value,
                        libraryId = libraryId.value
                    )
                }

                if (installedPackage.state != PackageState.ACTIVE) {
                    return@runInTransaction LibraryCommandResult.InvalidState(
                        "Cannot archive package (${installedPackageId.value}): package state is ${installedPackage.state}, expected ACTIVE."
                    )
                }

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
     * - Chỉ restore gói đang ở trạng thái ARCHIVED và đã được đăng ký trong Library.
     * - Thắt chặt quy tắc Single Active Version trong cùng transaction scope.
     */
    fun restorePackage(
        libraryId: LibraryId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<InstalledPackage> {
        return try {
            transactionRunner.runInTransaction {
                val library = libraryRepository.findById(libraryId)
                    ?: return@runInTransaction LibraryCommandResult.LibraryNotFound(libraryId.value)

                val installedPackage = installedPackageRepository.findById(installedPackageId)
                    ?: return@runInTransaction LibraryCommandResult.PackageNotFound(installedPackageId.value)

                if (installedPackage.libraryId != libraryId) {
                    return@runInTransaction LibraryCommandResult.CrossLibraryConflict(
                        "Package (${installedPackageId.value}) belongs to library (${installedPackage.libraryId.value}), not target library (${libraryId.value})."
                    )
                }

                if (!library.hasPackage(installedPackageId)) {
                    return@runInTransaction LibraryCommandResult.PackageNotRegisteredInLibrary(
                        packageId = installedPackageId.value,
                        libraryId = libraryId.value
                    )
                }

                if (installedPackage.state != PackageState.ARCHIVED) {
                    return@runInTransaction LibraryCommandResult.InvalidState(
                        "Cannot restore package (${installedPackageId.value}): package state is ${installedPackage.state}, expected ARCHIVED."
                    )
                }

                val allPackagesInLibrary = installedPackageRepository.findAllByLibraryId(libraryId)
                val otherPackages = allPackagesInLibrary.filterNot { it.id == installedPackageId }

                if (otherPackages.any { it.packageId == installedPackage.packageId && it.isActive }) {
                    return@runInTransaction LibraryCommandResult.ActiveVersionConflict(installedPackage.packageId.value)
                }

                val restoreResult = LibraryDomainCoordinator.restorePackage(
                    library = library,
                    installedPackage = installedPackage,
                    installedPackagesInLibrary = allPackagesInLibrary
                )
                installedPackageRepository.save(restoreResult.installedPackage)
                LibraryCommandResult.Success(restoreResult.installedPackage)
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
        return try {
            transactionRunner.runInTransaction {
                val library = libraryRepository.findById(libraryId)
                    ?: return@runInTransaction LibraryCommandResult.LibraryNotFound(libraryId.value)

                val existingCollections = collectionRepository.findAllByLibraryId(libraryId)

                if (existingCollections.any { it.libraryId == libraryId && it.isActive && it.name == name }) {
                    return@runInTransaction LibraryCommandResult.DuplicateCollection(name.trimmedValue)
                }

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
        return try {
            transactionRunner.runInTransaction {
                val library = libraryRepository.findById(libraryId)
                    ?: return@runInTransaction LibraryCommandResult.LibraryNotFound(libraryId.value)

                val collection = collectionRepository.findById(collectionId)
                    ?: return@runInTransaction LibraryCommandResult.CollectionNotFound(collectionId.value)

                if (collection.libraryId != libraryId) {
                    return@runInTransaction LibraryCommandResult.CrossLibraryConflict(
                        "Collection (${collectionId.value}) belongs to library (${collection.libraryId.value}), not target library (${libraryId.value})."
                    )
                }

                if (!collection.isActive) {
                    return@runInTransaction LibraryCommandResult.InvalidState("Cannot rename collection (${collectionId.value}): collection is DELETED.")
                }

                val existingCollections = collectionRepository.findAllByLibraryId(libraryId)
                val otherCollections = existingCollections.filterNot { it.id == collectionId }

                if (otherCollections.any { it.isActive && it.name == newName }) {
                    return@runInTransaction LibraryCommandResult.DuplicateCollection(newName.trimmedValue)
                }

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
        return try {
            transactionRunner.runInTransaction {
                val library = libraryRepository.findById(libraryId)
                    ?: return@runInTransaction LibraryCommandResult.LibraryNotFound(libraryId.value)

                val collection = collectionRepository.findById(collectionId)
                    ?: return@runInTransaction LibraryCommandResult.CollectionNotFound(collectionId.value)

                if (collection.libraryId != libraryId) {
                    return@runInTransaction LibraryCommandResult.CrossLibraryConflict(
                        "Collection (${collectionId.value}) belongs to library (${collection.libraryId.value}), not target library (${libraryId.value})."
                    )
                }

                if (!collection.isActive) {
                    return@runInTransaction LibraryCommandResult.InvalidState("Cannot delete collection (${collectionId.value}): collection is already DELETED.")
                }

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
     * - Gói và Collection phải thuộc cùng Library.
     * - Gói phải đang ở trạng thái ACTIVE.
     * - Gói phải đã được đăng ký trong Library (không tự động đăng ký/repair).
     */
    fun assignPackageToCollection(
        libraryId: LibraryId,
        collectionId: CollectionId,
        installedPackageId: InstalledPackageId
    ): LibraryCommandResult<Collection> {
        return try {
            transactionRunner.runInTransaction {
                val library = libraryRepository.findById(libraryId)
                    ?: return@runInTransaction LibraryCommandResult.LibraryNotFound(libraryId.value)

                val collection = collectionRepository.findById(collectionId)
                    ?: return@runInTransaction LibraryCommandResult.CollectionNotFound(collectionId.value)

                val installedPackage = installedPackageRepository.findById(installedPackageId)
                    ?: return@runInTransaction LibraryCommandResult.PackageNotFound(installedPackageId.value)

                if (collection.libraryId != libraryId || installedPackage.libraryId != libraryId) {
                    return@runInTransaction LibraryCommandResult.CrossLibraryConflict(
                        "Cross library assignment forbidden: collection library is ${collection.libraryId.value}, package library is ${installedPackage.libraryId.value}, target library is ${libraryId.value}."
                    )
                }

                if (!library.hasPackage(installedPackageId)) {
                    return@runInTransaction LibraryCommandResult.PackageNotRegisteredInLibrary(
                        packageId = installedPackageId.value,
                        libraryId = libraryId.value
                    )
                }

                if (!collection.isActive) {
                    return@runInTransaction LibraryCommandResult.InvalidState("Cannot assign package to DELETED collection (${collectionId.value}).")
                }

                if (!installedPackage.isActive) {
                    return@runInTransaction LibraryCommandResult.InvalidState("Cannot assign package (${installedPackageId.value}) to collection: package state is ${installedPackage.state}, expected ACTIVE.")
                }

                if (collection.containsPackage(installedPackageId)) {
                    return@runInTransaction LibraryCommandResult.AlreadyAssigned(
                        packageId = installedPackageId.value,
                        collectionId = collectionId.value
                    )
                }

                val mutation = collection.assignPackage(installedPackage, library)
                val updatedCollection = mutation.aggregate
                collectionRepository.save(updatedCollection)
                LibraryCommandResult.Success(updatedCollection)
            }
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
        return try {
            transactionRunner.runInTransaction {
                val library = libraryRepository.findById(libraryId)
                    ?: return@runInTransaction LibraryCommandResult.LibraryNotFound(libraryId.value)

                val collection = collectionRepository.findById(collectionId)
                    ?: return@runInTransaction LibraryCommandResult.CollectionNotFound(collectionId.value)

                if (collection.libraryId != libraryId) {
                    return@runInTransaction LibraryCommandResult.CrossLibraryConflict(
                        "Collection (${collectionId.value}) belongs to library (${collection.libraryId.value}), not target library (${libraryId.value})."
                    )
                }

                if (!collection.isActive) {
                    return@runInTransaction LibraryCommandResult.InvalidState("Cannot remove package from DELETED collection (${collectionId.value}).")
                }

                if (!collection.containsPackage(installedPackageId)) {
                    return@runInTransaction LibraryCommandResult.NotAssigned(
                        packageId = installedPackageId.value,
                        collectionId = collectionId.value
                    )
                }

                val mutation = collection.removePackage(installedPackageId)
                val updatedCollection = mutation.aggregate
                collectionRepository.save(updatedCollection)
                LibraryCommandResult.Success(updatedCollection)
            }
        } catch (e: Exception) {
            LibraryCommandResult.PersistenceFailure("Failed to remove package from collection: ${e.message}", e)
        }
    }
}
