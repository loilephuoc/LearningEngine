# LP-001: Library Domain — Aggregates Specification

## 1. `Library` Aggregate Root

### Purpose
The `Library` Aggregate Root represents the learner's top-level content library container holding the manifest registry of all installed package entries.

### Responsibilities
- Own the catalog index of installed packages (`LibraryEntry` entities storing `installedPackageId`, `packageId`, `registeredAt`).
- Enforce unique package entry registration and collection name uniqueness validation.
- Provide queries to evaluate active package status by cross-referencing `InstalledPackage` aggregate states.

### Owned Entities
- `LibraryEntry` (Entity binding `installedPackageId`, `packageId`, `registeredAt`). Note: `LibraryEntry` does NOT store a `PackageState` snapshot; `InstalledPackage` is the Single Source of Truth for package state.

### Owned Value Objects
- `LibraryId`
- `InstalledPackageId`
- `PackageId`

### Invariants
- `LibraryId` must be non-blank and valid.
- `Library` name must not be blank.
- No duplicate `InstalledPackageId` entries may exist within a single `Library`.
- Single Active Version: At most one `ACTIVE` `InstalledPackage` for a given `PackageId` may be registered in a `Library`.

### Allowed State Changes
- `registerEntry(installedPackageId: InstalledPackageId, packageId: PackageId)`: Adds a new package entry to the library index.
- `unregisterEntry(installedPackageId: InstalledPackageId)`: Removes a package entry from the library index.

### Published Events
- `LibraryCreated` (via `Library.create`)

### Repository
- `LibraryRepository`

---

## 2. `InstalledPackage` Aggregate Root

### Purpose
The `InstalledPackage` Aggregate Root represents an installed OPD3 content package registered within the learner's local environment and acts as the Single Source of Truth for package lifecycle state.

### Responsibilities
- Own package installation metadata, topic identity, content counts, version information, and lifecycle status (`ACTIVE`, `ARCHIVED`, `REMOVED`).
- Control state transitions between `ACTIVE`, `ARCHIVED`, and `REMOVED`.

### Owned Value Objects
- `InstalledPackageId`
- `PackageId`
- `TopicId`
- `PackageName`
- `PackageVersion`
- `PackageState` (`ACTIVE`, `ARCHIVED`, `REMOVED`)

### Invariants
- `contentCount` and `learningItemCount` must be non-negative (`>= 0`).
- Allowed transitions: `ACTIVE` -> `ARCHIVED` -> `ACTIVE`, or `ACTIVE` / `ARCHIVED` -> `REMOVED`. `REMOVED` is terminal.

### Allowed State Changes
- `archive()`: Transitions state from `ACTIVE` to `ARCHIVED`. Emits `PackageArchivedEvent`.
- `restore()`: Transitions state from `ARCHIVED` to `ACTIVE`. Emits `PackageRestoredEvent`.
- `remove()`: Transitions state to `REMOVED`. Emits `PackageRemovedEvent`.

### Published Events
- `PackageArchived`
- `PackageRestored`
- `PackageRemoved`

### Repository
- `InstalledPackageRepository`

---

## 3. `Collection` Aggregate Root

### Purpose
The `Collection` Aggregate Root represents a user-defined logical grouping of installed packages within a Library (e.g., "JLPT N2 Vocabulary").

### Responsibilities
- Manage assigned `InstalledPackageId` references.
- Enforce collection naming constraints, active status checks, and package assignment uniqueness.

### Owned Value Objects
- `CollectionId`
- `LibraryId`
- `CollectionName`
- `CollectionState` (`ACTIVE`, `DELETED`)
- `InstalledPackageId`

### Invariants
- `CollectionName` must be unique within a single `Library` (case-insensitive).
- A package (`InstalledPackageId`) cannot be assigned to the same collection more than once.
- Assigned package IDs must reference active packages in the parent Library.
- Once a collection transitions to `DELETED`, no state mutations (`rename`, `assignPackage`, `removePackage`, `delete`) are permitted.

### Allowed State Changes
- `rename(newName: CollectionName)`: Updates collection title. Emits `CollectionRenamedEvent`.
- `assignPackage(installedPackage, library)`: Adds package assignment. Emits `PackageAssignedToCollectionEvent`.
- `removePackage(installedPackageId)`: Removes package assignment. Emits `PackageRemovedFromCollectionEvent`.
- `delete()`: Transitions state to `DELETED`. Emits `CollectionDeletedEvent`.

### Published Events
- `CollectionCreated` (via `LibraryDomainCoordinator.createCollection`)
- `CollectionRenamed`
- `CollectionDeleted`
- `PackageAssignedToCollection`
- `PackageRemovedFromCollection`

### Repository
- `CollectionRepository`
