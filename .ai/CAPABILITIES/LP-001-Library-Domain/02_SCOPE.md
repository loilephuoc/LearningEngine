# LP-001: Library Domain — Scope

## In-Scope Capabilities
LP-001 is bounded strictly to the **Pure Domain & Application Specification Layer** of the Library Platform:

1. **Domain Aggregates & Entities**:
   - `Library` Aggregate Root.
   - `InstalledPackage` Aggregate Root.
   - `Collection` Aggregate Root.
   - `LibraryEntry` Entity.
2. **Domain Value Objects**:
   - `LibraryId`, `InstalledPackageId`, `CollectionId`, `PackageId`, `TopicId`, `PackageVersion`, `CollectionName`, `PackageName`.
3. **Repository Interface Ports**:
   - `LibraryRepository` interface.
   - `InstalledPackageRepository` interface.
   - `CollectionRepository` interface.
4. **Use Case Specifications**:
   - `CreateLibraryUseCase`, `InstallPackageUseCase`, `ArchivePackageUseCase`, `RestorePackageUseCase`, `RemovePackageUseCase`, `CreateCollectionUseCase`, `RenameCollectionUseCase`, `DeleteCollectionUseCase`, `AssignPackageToCollectionUseCase`, `RemovePackageFromCollectionUseCase`, `ListInstalledPackagesUseCase`.
5. **Domain Events**:
   - `LibraryCreated`, `PackageInstalled`, `PackageArchived`, `PackageRestored`, `PackageRemoved`, `CollectionCreated`, `CollectionRenamed`, `CollectionDeleted`, `PackageAssignedToCollection`, `PackageRemovedFromCollection`.

## Out-of-Scope Elements (Strictly Excluded)
- ❌ Database implementation (SQLite, Room, JSON persistence adapters).
- ❌ Filesystem IO or raw `.opd3` ZIP file unpacking.
- ❌ Compose Desktop UI composables or ViewModels.
- ❌ Workspace aggregate bindings or active study session execution.
- ❌ FSRS scheduling calculations or review history persistence.
- ❌ Cloud synchronization, AI tutor sidecars, or remote marketplace networking.
