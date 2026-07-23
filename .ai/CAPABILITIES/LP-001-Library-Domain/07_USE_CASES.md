# LP-001: Library Domain — Use Cases Specification

## 1. `CreateLibraryUseCase`
- **Command**: `CreateLibraryCommand(libraryId: LibraryId, name: String)`
- **Output**: `Result<LibraryId, CreateLibraryError>`
- **Workflow**: Validates input -> Checks if LibraryId exists -> Instantiates `Library` aggregate -> Saves to `LibraryRepository` -> Emits `LibraryCreated` event.

## 2. `InstallPackageUseCase`
- **Command**: `InstallPackageCommand(libraryId: LibraryId, packageId: PackageId, topicId: TopicId, name: PackageName, version: PackageVersion, contentCount: Int, itemCount: Int)`
- **Output**: `Result<InstalledPackageId, InstallPackageError>`
- **Workflow**: Checks existing package -> Enforces unique active version invariant -> Instantiates `InstalledPackage` (`ACTIVE`) -> Saves to `InstalledPackageRepository` -> Registers entry in `Library` aggregate -> Emits `PackageInstalled` event.

## 3. `ArchivePackageUseCase`
- **Command**: `ArchivePackageCommand(installedPackageId: InstalledPackageId)`
- **Output**: `Result<Unit, ArchivePackageError>`
- **Workflow**: Finds `InstalledPackage` -> Invokes `archive()` -> Saves updated aggregate -> Emits `PackageArchived` event.

## 4. `RestorePackageUseCase`
- **Command**: `RestorePackageCommand(installedPackageId: InstalledPackageId)`
- **Output**: `Result<Unit, RestorePackageError>`
- **Workflow**: Finds `InstalledPackage` -> Invokes `restore()` -> Saves updated aggregate -> Emits `PackageRestored` event.

## 5. `RemovePackageUseCase`
- **Command**: `RemovePackageCommand(libraryId: LibraryId, installedPackageId: InstalledPackageId)`
- **Output**: `Result<Unit, RemovePackageError>`
- **Workflow**: Finds `InstalledPackage` -> Invokes `remove()` -> Unregisters from `Library` aggregate -> Removes package reference from all `Collection` aggregates -> Emits `PackageRemoved` event.

## 6. `CreateCollectionUseCase`
- **Command**: `CreateCollectionCommand(libraryId: LibraryId, collectionId: CollectionId, name: CollectionName, description: String)`
- **Output**: `Result<CollectionId, CreateCollectionError>`
- **Workflow**: Verifies collection name uniqueness in library -> Instantiates `Collection` -> Saves to `CollectionRepository` -> Emits `CollectionCreated` event.

## 7. `RenameCollectionUseCase`
- **Command**: `RenameCollectionCommand(collectionId: CollectionId, newName: CollectionName)`
- **Output**: `Result<Unit, RenameCollectionError>`
- **Workflow**: Finds collection -> Verifies new name uniqueness -> Invokes `rename()` -> Saves to `CollectionRepository` -> Emits `CollectionRenamed` event.

## 8. `DeleteCollectionUseCase`
- **Command**: `DeleteCollectionCommand(collectionId: CollectionId)`
- **Output**: `Result<Unit, DeleteCollectionError>`
- **Workflow**: Finds collection -> Deletes from `CollectionRepository` -> Emits `CollectionDeleted` event. (Note: InstalledPackages inside collection remain intact in Library).

## 9. `AssignPackageToCollectionUseCase`
- **Command**: `AssignPackageToCollectionCommand(collectionId: CollectionId, installedPackageId: InstalledPackageId)`
- **Output**: `Result<Unit, AssignPackageError>`
- **Workflow**: Verifies `InstalledPackageId` exists and is `ACTIVE` -> Invokes `collection.assignPackage()` -> Saves collection -> Emits `PackageAssignedToCollection` event.

## 10. `RemovePackageFromCollectionUseCase`
- **Command**: `RemovePackageFromCollectionCommand(collectionId: CollectionId, installedPackageId: InstalledPackageId)`
- **Output**: `Result<Unit, RemovePackageError>`
- **Workflow**: Finds collection -> Invokes `collection.removePackage()` -> Saves collection -> Emits `PackageRemovedFromCollection` event.

## 11. `ListInstalledPackagesUseCase`
- **Query**: `ListInstalledPackagesQuery(libraryId: LibraryId, filterState: PackageState? = null)`
- **Output**: `List<InstalledPackageSummaryDTO>`
- **Workflow**: Queries `InstalledPackageRepository` -> Filters by state if requested -> Maps to summary DTOs.
