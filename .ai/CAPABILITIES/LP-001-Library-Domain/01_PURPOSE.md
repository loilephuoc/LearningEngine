# LP-001: Library Domain — Purpose

## Mission Statement
The purpose of capability **LP-001: Library Domain** is to establish the pure, UI-independent, infrastructure-free **Library Platform Domain Layer** for Learning Engine 2.0.

LP-001 defines the core domain abstractions, aggregate roots (`Library`, `InstalledPackage`, `Collection`), value objects, business invariants, domain events, and repository interfaces governing content package installation, organization, cataloging, version tracking, and collection management.

## Core Objectives
1. **Pure Domain Isolation**: Provide a pure Kotlin domain model containing zero UI framework (Compose), database (SQLite/Room), filesystem IO, or OPD3 ZIP archive parsing dependencies.
2. **Package Lifecycle Management**: Define the immutable lifecycle states of installed content packages (`ACTIVE`, `ARCHIVED`, `REMOVED`).
3. **Collection Cataloging**: Enable learners to organize installed packages into logical groupings (Collections) with strict identity and uniqueness invariants.
4. **Clean Repository Interfaces**: Provide explicit domain repository interfaces (`LibraryRepository`, `InstalledPackageRepository`, `CollectionRepository`) to be implemented by infrastructure adapters in future capabilities.

## Architectural Alignment
LP-001 adheres strictly to **Architecture Lock Version 1.0** and Laws 1, 3, 4, 8, 10 of `00_ARCHITECTURE_CONSTITUTION.md`.
