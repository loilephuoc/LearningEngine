# LP-001: Library Domain — Invariants Specification

## Business Invariants

### 1. Single Active Version Invariant
- **Rule**: A `Library` may contain only ONE active `InstalledPackage` per `PackageId` at any point in time. Installing a newer version requires archiving or replacing the existing active instance.

### 2. Collection Name Uniqueness Invariant
- **Rule**: `CollectionName` must be unique within a single `Library` instance (case-insensitive). Two collections under the same library cannot share identical titles.

### 3. Aggregate Root Ownership Invariant
- **Rule**: `Library` owns all `InstalledPackage` entries. An `InstalledPackage` cannot exist as an orphan without referencing a parent `LibraryId`.

### 4. No Orphan Entity Invariant
- **Rule**: A `Collection` may only contain assignments (`InstalledPackageId`) to packages that exist in the parent `Library`. Removing a package from the `Library` automatically clears all references to it across all collections.

### 5. Collection Deletion Isolation Invariant
- **Rule**: Deleting a `Collection` aggregate removes only the grouping metadata; it must NOT delete or modify the underlying `InstalledPackage` aggregates.

### 6. Immutability & State Mutation Invariant
- **Rule**: Domain entities and value objects are strictly immutable. State mutations MUST produce a new aggregate instance and return an explicit domain event.

### 7. Zero Infrastructure Coupling Invariant
- **Rule**: Domain aggregates and value objects in LP-001 must have zero imports or coupling to JSON, SQLite, Room, Compose UI, or filesystem APIs.
