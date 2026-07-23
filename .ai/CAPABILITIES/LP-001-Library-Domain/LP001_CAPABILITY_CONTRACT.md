# LP-001 Capability Contract — Library Domain

| Metadata | Specification |
|---|---|
| **Capability ID** | **LP-001** |
| **Capability Name** | **Library Domain** |
| **Subsystem** | **Library Platform** |
| **Architecture Version** | **1.0 LOCKED** |
| **Status** | **CONTRACT SPECIFIED & APPROVED** |
| **Target Layer** | **Pure Domain & Application Specification** |

---

## 1. Executive Summary & Goal
This **Capability Contract** binds the exact functional, domain model, invariant, and use case specifications for **LP-001: Library Domain**.

LP-001 defines **WHAT** must be implemented for the pure Library Domain without specifying implementation details, persistence frameworks, UI elements, or filesystem IO.

---

## 2. Mandatory Deliverables Summary

### Aggregates
- `Library`: Top-level catalog index aggregate root.
- `InstalledPackage`: Installed package lifecycle aggregate root (`ACTIVE`, `ARCHIVED`, `REMOVED`).
- `Collection`: User-defined package grouping aggregate root.

### Value Objects
- `LibraryId`, `InstalledPackageId`, `CollectionId`, `PackageId`, `TopicId`, `PackageVersion`, `CollectionName`, `PackageName`.

### Repository Interfaces (Ports)
- `LibraryRepository`, `InstalledPackageRepository`, `CollectionRepository`.

### Application Use Cases
- `CreateLibraryUseCase`, `InstallPackageUseCase`, `ArchivePackageUseCase`, `RestorePackageUseCase`, `RemovePackageUseCase`, `CreateCollectionUseCase`, `RenameCollectionUseCase`, `DeleteCollectionUseCase`, `AssignPackageToCollectionUseCase`, `RemovePackageFromCollectionUseCase`, `ListInstalledPackagesUseCase`.

### Domain Events
- `LibraryCreated`, `PackageInstalled`, `PackageArchived`, `PackageRestored`, `PackageRemoved`, `CollectionCreated`, `CollectionRenamed`, `CollectionDeleted`, `PackageAssignedToCollection`, `PackageRemovedFromCollection`.

---

## 3. Mandatory Invariants

1. **Single Active Version**: Only one `ACTIVE` `InstalledPackage` per `PackageId` within a `Library`.
2. **Collection Name Uniqueness**: `CollectionName` must be unique per `Library` (case-insensitive).
3. **No Orphan Packages**: `InstalledPackage` must reference a valid parent `LibraryId`.
4. **Cascade Assignment Cleanup**: Removing a package from `Library` clears references across all `Collection` aggregates.
5. **Zero Infrastructure Coupling**: Domain objects have zero imports from Compose UI, SQLite/Room, JSON, or filesystem APIs.

---

## 4. Contract Sign-Off & Approval
This contract is complete, unambiguous, and locked under **Architecture Version 1.0 LOCKED**. Implementation engineers may execute LP-001 strictly according to these specifications.
