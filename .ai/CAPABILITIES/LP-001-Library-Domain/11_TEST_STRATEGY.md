# LP-001: Library Domain — Test Strategy

## Test Specifications

### 1. Value Object Unit Tests
- **Target**: `LibraryId`, `InstalledPackageId`, `CollectionId`, `PackageVersion`, `CollectionName`.
- **Test Focus**: Validate instantiation succeeds for valid inputs and throws `IllegalArgumentException` / returns invalid result for blank/malformed strings. Verify structural equality.

### 2. Aggregate Unit Tests
- **Target**: `Library`, `InstalledPackage`, `Collection`.
- **Test Focus**: Verify allowed state transitions (`archive()`, `restore()`, `remove()`), invariant checks (unique collection package assignments), and entity encapsulation.

### 3. Invariant Boundary Tests
- **Target**: Domain business rules.
- **Test Focus**: Verify that attempting to register two active packages with the same `PackageId` fails immediately. Verify case-insensitive collection name uniqueness.

### 4. Repository Contract Tests
- **Target**: `LibraryRepository`, `InstalledPackageRepository`, `CollectionRepository` interface contracts.
- **Test Focus**: Provide an in-memory repository test harness (`InMemoryLibraryRepository`) to verify repository contract compliance (save, findById, delete, query methods).

### 5. Application Use Case Unit Tests
- **Target**: All 11 Use Cases (`InstallPackageUseCase`, `CreateCollectionUseCase`, etc.).
- **Test Focus**: Verify workflow orchestration, event emission, transaction boundary handling, and diagnostic error mapping.
