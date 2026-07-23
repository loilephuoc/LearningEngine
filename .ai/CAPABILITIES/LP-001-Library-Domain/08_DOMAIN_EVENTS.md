# LP-001: Library Domain — Domain Events Specification

All domain events implement the marker interface `vn.loi.learning.domain.common.event.DomainEvent` and are immutable data classes.

---

## 1. `LibraryCreated`
- **Fields**: `libraryId: LibraryId`, `name: String`, `occurredAt: Instant`
- **Trigger**: Published when a new `Library` aggregate is successfully initialized and saved.

## 2. `PackageInstalled`
- **Fields**: `installedPackageId: InstalledPackageId`, `libraryId: LibraryId`, `packageId: PackageId`, `topicId: TopicId`, `version: PackageVersion`, `occurredAt: Instant`
- **Trigger**: Published when a new content package is registered in active library state.

## 3. `PackageArchived`
- **Fields**: `installedPackageId: InstalledPackageId`, `packageId: PackageId`, `occurredAt: Instant`
- **Trigger**: Published when an active package is transitioned to `ARCHIVED` state.

## 4. `PackageRestored`
- **Fields**: `installedPackageId: InstalledPackageId`, `packageId: PackageId`, `occurredAt: Instant`
- **Trigger**: Published when an archived package is restored to `ACTIVE` state.

## 5. `PackageRemoved`
- **Fields**: `installedPackageId: InstalledPackageId`, `libraryId: LibraryId`, `packageId: PackageId`, `occurredAt: Instant`
- **Trigger**: Published when a package is removed from the library catalog.

## 6. `CollectionCreated`
- **Fields**: `collectionId: CollectionId`, `libraryId: LibraryId`, `name: CollectionName`, `occurredAt: Instant`
- **Trigger**: Published when a new user collection is created.

## 7. `CollectionRenamed`
- **Fields**: `collectionId: CollectionId`, `oldName: CollectionName`, `newName: CollectionName`, `occurredAt: Instant`
- **Trigger**: Published when a collection name is updated.

## 8. `CollectionDeleted`
- **Fields**: `collectionId: CollectionId`, `libraryId: LibraryId`, `occurredAt: Instant`
- **Trigger**: Published when a collection is deleted.

## 9. `PackageAssignedToCollection`
- **Fields**: `collectionId: CollectionId`, `installedPackageId: InstalledPackageId`, `occurredAt: Instant`
- **Trigger**: Published when an installed package is assigned to a collection.

## 10. `PackageRemovedFromCollection`
- **Fields**: `collectionId: CollectionId`, `installedPackageId: InstalledPackageId`, `occurredAt: Instant`
- **Trigger**: Published when a package assignment is removed from a collection.
