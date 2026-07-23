# LP-001: Library Domain — Definition of Done (DoD)

Capability **LP-001: Library Domain** is DONE when:

- [ ] All 3 Aggregate Roots (`Library`, `InstalledPackage`, `Collection`) are specified with complete purpose, invariants, state transitions, and published events.
- [ ] All 8 Value Objects (`LibraryId`, `InstalledPackageId`, `CollectionId`, `PackageId`, `TopicId`, `PackageVersion`, `CollectionName`, `PackageName`) are defined with explicit validation and equality rules.
- [ ] All 3 Repository interfaces (`LibraryRepository`, `InstalledPackageRepository`, `CollectionRepository`) are specified.
- [ ] All 11 Use Cases are specified with explicit command inputs, outputs, and workflows.
- [ ] All 10 Domain Events are defined as immutable value types.
- [ ] Test Strategy specifies unit, aggregate, invariant, and repository contract tests.
- [ ] Architecture Lock 1.0 constraints are 100% satisfied (zero infrastructure imports).
- [ ] Executive Contract [`LP001_CAPABILITY_CONTRACT.md`](LP001_CAPABILITY_CONTRACT.md) is signed and locked.
