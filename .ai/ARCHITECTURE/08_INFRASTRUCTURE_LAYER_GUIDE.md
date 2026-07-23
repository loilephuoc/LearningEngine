# Learning Engine 2.0 — Infrastructure Layer Guide

## Purpose
This document provides implementation guidance for the **Infrastructure Layer** (`vn.loi.learning.infrastructure.*`) of Learning Engine 2.0.

## Responsibilities
- Implement Application Ports and Domain Repositories.
- Manage JSON persistence, filesystem IO, ZIP archive compression/decompression, and security validation.
- Implement cryptographic hashing (`Sha256PackageIntegrityHasher`) and byte-for-byte deterministic ZIP writing (`DeterministicZipWriter`).
- Enforce resource safety limits (`PackageSafetyLimits`).

## Out of Scope
- Core domain business logic or FSRS calculation algorithms.
- Presentation screen rendering or UI composables.

## Dependencies
- [`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md)
- [`07_APPLICATION_LAYER_GUIDE.md`](07_APPLICATION_LAYER_GUIDE.md)

---

## Infrastructure Layer Structure

```text
vn.loi.learning.infrastructure/
├── contentpackaging/      # DeterministicZipWriter, Package Readers
├── persistence/           # JSON Storage Stores, Mappers, Repositories
└── system/                # System Clock, Filesystem Adapters
```

---

## Key Infrastructure Building Blocks

### 1. Repository Implementations
- Implements application repository interfaces using concrete persistence technologies (e.g., JSON files, Room/SQLite, InMemory).
- Maps infrastructure data transfer objects (DTOs) to domain entities.
- Example:
  ```kotlin
  class JsonWorkspaceRepository(
      private val storageFile: File,
      private val json: Json
  ) : WorkspaceRepository {
      override fun findById(id: WorkspaceId): Workspace? { ... }
      override fun save(workspace: Workspace) { ... }
  }
  ```

### 2. Deterministic Archive Exporter (`DeterministicZipWriter`)
- Generates `.opd3` ZIP archives with 100% byte-for-byte reproducibility:
  - Fixed ZipEntry modification timestamp (`1577836800000L` / 2020-01-01T00:00:00Z UTC).
  - Deterministic entry sorting (`metadata.json`, `contents.json`, `learning-items.json`, `media-manifest.json`, `media/*`, `manifest.json`).
  - Strict UTF-8 character encoding.

### 3. Resource Safety & Path Security (`Opd3PathValidator` & `PackageSafetyLimits`)
- All infrastructure archive processors enforce streaming validation:
  - Bounded 8KB buffer reading.
  - Running byte counters for single-entry size (`500MB`) and total archive size (`2GB`).
  - Rejection of path traversal (`..`, `.`), drive letters, duplicate entries, and unlisted extra archive files.

### 4. Persistence Compatibility & Schema Evolution
- Legacy JSON array formats and OPD3 package schemas are product contracts.
- Parsers use lenient deserialization (`ignoreUnknownKeys = true`) with explicit schema version migration strategies. Never silently discard unparsed fields.

---

## Anti-Patterns to Avoid
1. **Leaking Infrastructure Models into Domain**: Infrastructure DTOs or database entities must never be returned across repository boundaries; always map to Domain Aggregates.
2. **Ignoring Serialization Exceptions**: Unhandled JSON parsing exceptions must be wrapped in structured application diagnostics.
3. **In-Place File Corruption**: Database or file updates must use atomic write-then-rename patterns to prevent data corruption during system crashes.

---

## Future Evolution
Platform expansion will introduce additional infrastructure adapters (e.g., Android Room SQLite storage, iOS Keychain adapters, Web IndexedDB drivers) implementing existing Application Ports without touching domain code.

---

## Architecture Notes
- All infrastructure adapters MUST be verified by integration and round-trip tests in `src/test/kotlin/`.
