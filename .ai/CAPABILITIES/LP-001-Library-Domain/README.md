# LP-001: Library Domain Capability Specifications

## Purpose
This directory contains the complete **Capability Contract and Specification Set** for **LP-001: Library Domain** in Learning Engine 2.0. It defines the pure domain models, aggregate boundaries, value objects, repository interfaces, use case contracts, domain events, business invariants, acceptance criteria, and testing strategies for the Library Platform context.

---

## Document Index

1. **[`LP001_CAPABILITY_CONTRACT.md`](LP001_CAPABILITY_CONTRACT.md)**
   - Executive Capability Contract for LP-001 binding all domain rules and acceptance criteria.

2. **[`01_PURPOSE.md`](01_PURPOSE.md)**
   - Mission statement and domain goals of LP-001 Library Domain.

3. **[`02_SCOPE.md`](02_SCOPE.md)**
   - Explicit scope boundaries: what is included vs excluded in LP-001.

4. **[`03_DOMAIN_MODEL.md`](03_DOMAIN_MODEL.md)**
   - High-level domain model architecture, bounded context boundary, and entity relationship diagrams.

5. **[`04_AGGREGATES.md`](04_AGGREGATES.md)**
   - Detailed specification for Aggregates: `Library`, `InstalledPackage`, and `Collection`.

6. **[`05_VALUE_OBJECTS.md`](05_VALUE_OBJECTS.md)**
   - Specifications for Value Objects: `LibraryId`, `InstalledPackageId`, `CollectionId`, `PackageVersion`, `PackageId`, `TopicId`, `CollectionName`, `PackageName`.

7. **[`06_REPOSITORIES.md`](06_REPOSITORIES.md)**
   - Interface contracts for Repositories: `LibraryRepository`, `InstalledPackageRepository`, `CollectionRepository`.

8. **[`07_USE_CASES.md`](07_APPLICATION_USE_CASES.md)**
   - Specifications for 11 core Use Cases (Create Library, Install Package, Archive Package, Restore Package, Remove Package, Create Collection, Rename Collection, Delete Collection, Assign Package To Collection, Remove Package From Collection, List Installed Packages).

9. **[`08_DOMAIN_EVENTS.md`](08_DOMAIN_EVENTS.md)**
   - Specifications for 10 immutable Domain Events.

10. **[`09_INVARIANTS.md`](09_INVARIANTS.md)**
    - Non-negotiable business rules and domain invariants.

11. **[`10_ACCEPTANCE_CRITERIA.md`](10_ACCEPTANCE_CRITERIA.md)**
    - Measurable acceptance criteria for all LP-001 domain behaviors.

12. **[`11_TEST_STRATEGY.md`](11_TEST_STRATEGY.md)**
    - Test specifications covering Unit, Aggregate, Invariant, and Repository Contract tests.

13. **[`12_OUT_OF_SCOPE.md`](12_OUT_OF_SCOPE.md)**
    - Explicit list of features deferred to subsequent capabilities.

14. **[`13_ARCHITECTURE_CONSTRAINTS.md`](13_ARCHITECTURE_CONSTRAINTS.md)**
    - Clean Architecture constraints, Architecture Lock 1.0 alignment, and zero-infrastructure rules.

15. **[`14_TECHNICAL_DEBT.md`](14_TECHNICAL_DEBT.md)**
    - Technical debt tracking related to LP-001.

16. **[`15_DEFINITION_OF_DONE.md`](15_DEFINITION_OF_DONE.md)**
    - Definition of Done criteria for LP-001 completion.
