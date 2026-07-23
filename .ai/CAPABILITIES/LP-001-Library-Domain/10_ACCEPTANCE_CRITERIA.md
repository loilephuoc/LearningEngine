# LP-001: Library Domain — Acceptance Criteria

## Measurable Acceptance Criteria

### AC-1: Package Installation & Registration
- **Given** a valid `InstallPackageCommand` with `PackageId("pkg-1")`, `TopicId("topic-1")`, and `PackageVersion("1.0")`,
- **When** `InstallPackageUseCase.execute` is invoked,
- **Then**:
  1. An `InstalledPackage` aggregate is instantiated with state `ACTIVE`.
  2. The package entry is registered in `LibraryRepository`.
  3. A `PackageInstalled` domain event is emitted.

### AC-2: Duplicate Active Version Rejection
- **Given** an active `InstalledPackage` with `PackageId("pkg-1")` already exists in `Library`,
- **When** another `InstallPackageCommand` with `PackageId("pkg-1")` is executed without archiving the existing instance,
- **Then**:
  1. The use case returns a failure result (`InstallPackageError.DuplicateActivePackage`).
  2. No new package is registered.
  3. No `PackageInstalled` event is emitted.

### AC-3: Package Archival & Restoration
- **Given** an active `InstalledPackage` aggregate,
- **When** `ArchivePackageUseCase` is executed,
- **Then** state transitions to `ARCHIVED` and `PackageArchived` event is emitted.
- **When** `RestorePackageUseCase` is subsequently executed,
- **Then** state transitions back to `ACTIVE` and `PackageRestored` event is emitted.

### AC-4: Collection Creation & Name Uniqueness
- **Given** a library without any collection named "JLPT N3",
- **When** `CreateCollectionUseCase` is executed with name `"JLPT N3"`,
- **Then** a new `Collection` aggregate is created and `CollectionCreated` event is emitted.
- **When** `CreateCollectionUseCase` is executed again with name `"jlpt n3"` (different case),
- **Then** the operation fails with `CreateCollectionError.DuplicateName`.

### AC-5: Package Removal Cleanup
- **Given** an `InstalledPackage` assigned to Collection `"Col-A"`,
- **When** `RemovePackageUseCase` is executed for that package,
- **Then**:
  1. The package state transitions to `REMOVED`.
  2. The package ID is removed from `Collection` `"Col-A"`'s assigned list.
  3. `PackageRemoved` event is emitted.
