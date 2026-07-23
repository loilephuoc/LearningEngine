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

### Invariants
- `LibraryId` must be non-blank and valid.
- `Library` name must not be blank.
- No duplicate `InstalledPackageId` entries may exist within a single `Library`.
- Single Active Version: At most one `ACTIVE` `InstalledPackage` for a given `PackageId` may be registered in a `Library`.

### Allowed State Changes
- `Library.create(id, name)`: Creates new Library aggregate and emits `LibraryCreatedEvent`. (Aggregate-local).
- `registerEntry(...)`: Internal aggregate registration method.
- `unregisterEntry(...)`: Internal aggregate unregistration method.

### Repository
- `LibraryRepository`

---

## 2. `InstalledPackage` Aggregate Root

### Purpose
The `InstalledPackage` Aggregate Root represents an installed OPD3 content package registered within the learner's local environment and acts as the Single Source of Truth for package lifecycle state.

### Invariants
- `contentCount` and `learningItemCount` must be non-negative (`>= 0`).
- Allowed transitions: `ACTIVE` -> `ARCHIVED` -> `ACTIVE`, or `ACTIVE` / `ARCHIVED` -> `REMOVED`. `REMOVED` is terminal.

### Operations & API Boundary Controls
- **`archive()` (Aggregate-Local)**: Transition to `ARCHIVED`. Publicly accessible because archive requires no cross-aggregate validation. Emits `PackageArchivedEvent`.
- **`restore(token: CoordinatorToken)` (Coordinator-Required)**: Transition from `ARCHIVED` to `ACTIVE`. Restricted by `CoordinatorToken` to enforce Single Active Version validation via `LibraryDomainCoordinator.restorePackage(...)`. Emits `PackageRestoredEvent`.
- **`remove(token: CoordinatorToken)` (Coordinator-Required)**: Transition to `REMOVED`. Restricted by `CoordinatorToken` to enforce Library unregistration and Collection reference cleanup via `LibraryDomainCoordinator.removePackage(...)`. Emits `PackageRemovedEvent`.
- **`reconstitute(...)` (Persistence Rehydration)**: Public factory for persistence layer rehydration. Validates aggregate-local invariants without emitting events.

### Repository
- `InstalledPackageRepository`

---

## 3. `Collection` Aggregate Root

### Purpose
The `Collection` Aggregate Root represents a user-defined logical grouping of installed packages within a Library (e.g., "JLPT N2 Vocabulary").

### Invariants
- `CollectionName` must be unique within a single `Library` (case-insensitive).
- A package (`InstalledPackageId`) cannot be assigned to the same collection more than once.
- Assigned package IDs must reference active packages in the parent Library.
- Once a collection transitions to `DELETED`, no state mutations (`rename`, `assignPackage`, `removePackage`, `delete`) are permitted.

### Operations & API Boundary Controls
- **`create(..., token: CoordinatorToken)` (Coordinator-Required)**: Creates new Collection. Restricted by `CoordinatorToken` so `LibraryDomainCoordinator.createCollection(...)` enforces case-insensitive name uniqueness in the parent Library. Emits `CollectionCreatedEvent`.
- **`rename(newName, token: CoordinatorToken)` (Coordinator-Required)**: Updates title. Restricted by `CoordinatorToken` so `LibraryDomainCoordinator.renameCollection(...)` enforces case-insensitive name uniqueness. Emits `CollectionRenamedEvent`.
- **`assignPackage(installedPackage, library)` (Aggregate-Local with context)**: Adds package assignment. Validates package active status in Library. Emits `PackageAssignedToCollectionEvent`.
- **`removePackage(installedPackageId)` (Aggregate-Local)**: Removes package assignment. Emits `PackageRemovedFromCollectionEvent`.
- **`delete()` (Aggregate-Local)**: Transitions state to `DELETED`. Emits `CollectionDeletedEvent`.
- **`reconstitute(...)` (Persistence Rehydration)**: Public factory for persistence rehydration. Validates aggregate-local invariants without emitting events.

### Repository
- `CollectionRepository`
