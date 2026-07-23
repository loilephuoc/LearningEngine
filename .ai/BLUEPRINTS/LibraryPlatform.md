# Library Platform Blueprint

## Purpose
The **Library Platform** subsystem manages the catalog of installed OPD3 packages, user collections, tag organization, package versioning, and conflict-aware package imports.

## Responsibilities
- Register and install validated OPD3 content packages into local application persistence.
- Manage user-created Collections (logical groupings of installed packages).
- Track installed package metadata, installation timestamps, and version history.
- Handle conflict detection during package re-import (e.g., version mismatch, topic ID collision).
- Exclude installed packages or archive topics when requested by the learner.

## Out of Scope
- Raw ZIP byte streaming validation and cryptographic hashing (handled by Package Platform).
- Active study session execution and card scheduling (handled by Learning Session Platform).

## Dependencies
- Package Platform (`CanonicalTopicPackage`, `Opd3PackageInspector`, `Opd3PackageVerifier`).
- Application persistence layer (`LibraryRepository`, `CollectionRepository`).

---

## Subsystem Architecture & Components

```text
+-----------------------------------------------------------------------------------+
|                                 LIBRARY PLATFORM                                  |
|                                                                                   |
|  +------------------------------+             +--------------------------------+  |
|  |     InstallPackageUseCase    |============>|    ConflictAwareImporter       |  |
|  |     (Package Registration)   |             |    (Version & Collision Check) |  |
|  +--------------+---------------+             +---------------+----------------+  |
|                 |                                             |                   |
|                 v                                             v                   |
|  +------------------------------+             +--------------------------------+  |
|  |     LibraryRepository        |============>|    CollectionRepository        |  |
|  |     (Installed Package Store)|             |    (User Collection Catalog)   |  |
|  +------------------------------+             +--------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## Data Models & Schema Contracts

### InstalledPackage Aggregate
- `packageId`: Unique identifier for the installed package instance.
- `topicId`: Associated `TopicId`.
- `name`: Logical topic name.
- `version`: Installed package version string.
- `installedAt`: Timestamp of installation.
- `contentCount`: Number of content items in topic.
- `learningItemCount`: Number of testable learning items.
- `mediaCount`: Number of associated media assets.

### Collection Aggregate
- `id`: Collection identifier.
- `name`: User-defined collection title (e.g., "JLPT N3 Prep").
- `description`: Optional collection description.
- `installedPackageIds`: List of bound installed package IDs.

---

## Conflict Resolution Strategy

When importing an OPD3 package whose `TopicId` already exists in the Library:
1. **SAME_VERSION**: Ignore or update metadata without duplicating records.
2. **NEWER_VERSION**: Upgrade installed package contents and update version registry.
3. **OLDER_VERSION**: Reject import with `ConflictDiagnostic.OlderVersionRejected`.
4. **TOPIC_COLLISION**: Prompt user or resolve according to configured conflict policy.

---

## Future Evolution
- Support for remote package synchronization with cloud repositories.
- Support for automated background package updates.

---

## Architecture Notes
- All package installation operations are transactional and atomic.
