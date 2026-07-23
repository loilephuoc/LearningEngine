# LP-001: Library Domain — Aggregates Specification

## 1. `Library` Aggregate Root

### Purpose
The `Library` Aggregate Root represents the learner's top-level content library container holding the manifest registry of all installed package entries.

### Responsibilities
- Own the catalog index of installed packages (`LibraryEntry` entities).
- Enforce unique package installation invariants within the library.
- Coordinate library creation and entry registration.

### Owned Entities
- `LibraryEntry` (Entity representing an entry binding `InstalledPackageId` to `LibraryId`).

### Owned Value Objects
- `LibraryId`
- `InstalledPackageId`

### Invariants
- `LibraryId` must be non-blank and valid.
- `Library` name must not be blank.
- No duplicate `InstalledPackageId` entries may exist within a single `Library`.

### Allowed State Changes
- `registerPackage(packageId: InstalledPackageId)`: Adds a new package entry to the library.
- `unregisterPackage(packageId: InstalledPackageId)`: Removes a package entry from the library.

### Published Events
- `LibraryCreated`
- `PackageInstalled`
- `PackageRemoved`

### Repository
- `LibraryRepository`

### Transaction Boundary
- The `Library` aggregate forms a strict transactional consistency boundary for library entry registrations.

### Lifecycle
- Created via `Library.create(id, name)`. Persistent for the lifespan of a learner profile.

---

## 2. `InstalledPackage` Aggregate Root

### Purpose
The `InstalledPackage` Aggregate Root represents an installed OPD3 content package registered within the learner's local environment.

### Responsibilities
- Own package installation metadata, topic identity, content counts, version information, and lifecycle status (`ACTIVE`, `ARCHIVED`, `REMOVED`).
- Control state transitions between `ACTIVE`, `ARCHIVED`, and `REMOVED`.

### Owned Entities
- None (Self-contained Aggregate Root).

### Owned Value Objects
- `InstalledPackageId`
- `PackageId`
- `TopicId`
- `PackageName`
- `PackageVersion`
- `PackageState` (Enum: `ACTIVE`, `ARCHIVED`, `REMOVED`)

### Invariants
- Only one active `InstalledPackage` instance per `PackageId` is permitted in the active library.
- `contentCount` and `learningItemCount` must be non-negative (`>= 0`).
- State transitions must follow allowed lifecycle pathways (`ACTIVE` -> `ARCHIVED` -> `ACTIVE`, or `ACTIVE` / `ARCHIVED` -> `REMOVED`).

### Allowed State Changes
- `archive()`: Transitions state from `ACTIVE` to `ARCHIVED`.
- `restore()`: Transitions state from `ARCHIVED` to `ACTIVE`.
- `remove()`: Transitions state to `REMOVED`.

### Published Events
- `PackageInstalled`
- `PackageArchived`
- `PackageRestored`
- `PackageRemoved`

### Repository
- `InstalledPackageRepository`

### Transaction Boundary
- Single package state mutations operate within an independent transaction boundary.

### Lifecycle
- Created upon package import verification; transitions to `ARCHIVED` or `REMOVED` upon learner action.

---

## 3. `Collection` Aggregate Root

### Purpose
The `Collection` Aggregate Root represents a user-defined logical grouping of installed packages within a Library (e.g., "JLPT N2 Vocabulary").

### Responsibilities
- Manage the list of assigned `InstalledPackageId` references.
- Enforce collection naming constraints and package assignment uniqueness.

### Owned Entities
- None.

### Owned Value Objects
- `CollectionId`
- `LibraryId`
- `CollectionName`
- `InstalledPackageId`

### Invariants
- `CollectionName` must be unique within a single `Library`.
- A package (`InstalledPackageId`) cannot be assigned to the same collection more than once.
- Assigned package IDs must reference valid installed packages.

### Allowed State Changes
- `rename(newName: CollectionName)`: Updates collection title.
- `assignPackage(packageId: InstalledPackageId)`: Adds package assignment.
- `removePackage(packageId: InstalledPackageId)`: Removes package assignment.

### Published Events
- `CollectionCreated`
- `CollectionRenamed`
- `CollectionDeleted`
- `PackageAssignedToCollection`
- `PackageRemovedFromCollection`

### Repository
- `CollectionRepository`

### Transaction Boundary
- Single collection instance state changes execute within an independent transaction boundary.

### Lifecycle
- Created via `Collection.create(...)`, deleted via `Collection.delete()`.
