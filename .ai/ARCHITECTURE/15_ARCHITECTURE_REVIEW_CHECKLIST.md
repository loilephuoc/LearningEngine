# Learning Engine 2.0 — Architecture Review Checklist

## Purpose
This checklist provides the standardized audit verification tool used by Architects during formal Architecture Reviews. Every checklist item must be verified against codebase evidence before approving any architectural proposal or major capability increment.

---

## Architecture Audit Checklist

### 1. Domain-Driven Design (DDD)
- [ ] Are Domain Entities and Aggregates free from framework annotations and IO dependencies?
- [ ] Does every Aggregate Root have a unique immutable Value Object identity?
- [ ] Are invariants enforced immediately inside constructor / `init` blocks?
- [ ] Is domain logic isolated within domain models/services rather than leaking into ViewModels or database adapters?

### 2. Dependency Direction
- [ ] Does the Domain Layer have ZERO imports from `application`, `infrastructure`, `desktop`, or `kotlinx.serialization`?
- [ ] Does the Application Layer depend exclusively on Domain models and Application Ports?
- [ ] Are Infrastructure concrete adapters kept behind Application Ports? (Verify no direct imports like `Opd3PackageExporter` -> `DeterministicZipWriter`).
- [ ] Is presentation logic free from direct infrastructure persistence references?

### 3. Single Source of Truth & Ownership
- [ ] Does every domain concept (Package, InstalledPackage, Workspace, Session, Scheduler, Statistics) have exactly one owning Bounded Context?
- [ ] Are duplicate responsibilities across bounded contexts eliminated?

### 4. Events & Asynchronous Integration
- [ ] Are domain events named in past tense (`ReviewCompletedEvent`, `PackageInstalledEvent`)?
- [ ] Are events immutable data classes?
- [ ] Is inter-context communication decoupled via event publishing where appropriate?

### 5. Repositories & Ports
- [ ] Are repository interfaces defined in Domain/Application layers using domain entities?
- [ ] Do repository implementations reside in Infrastructure, returning domain aggregate roots?
- [ ] Are transaction ownership boundaries explicitly held at Application Use Case boundaries?

### 6. Ubiquitous Language & Naming
- [ ] Do class names, method signatures, and package structures match the vocabulary dictionary in `02_UBIQUITOUS_LANGUAGE.md`?
- [ ] Are legacy terms mapped clearly in the Vocabulary Alignment table?

### 7. Future Scalability
- [ ] Is the architecture prepared for multi-platform clients (Desktop, Android, iOS, Web)?
- [ ] Are persistence schemas designed to accommodate Cloud Sync change-data-capture logs?
- [ ] Can AI Tutor sidecars integrate via application ports without mutating core domain structures?

### 8. Testing & Verification
- [ ] Are unit tests provided at the narrowest useful boundary?
- [ ] Are integration and adversarial tests implemented for security/safety boundaries (`Opd3AdversarialTest`)?
- [ ] Does `.\gradlew.bat clean test` execute to `BUILD SUCCESSFUL`?

### 9. Backward Compatibility & Data Safety
- [ ] Are persistent records, JSON array envelopes, and OPD3 package schemas preserved or provided with explicit migrations?
- [ ] Does a failed operation roll back completely without leaving partial corrupted files?

### 10. Performance & Resource Safety
- [ ] Does ZIP archive reading use bounded 8KB streaming buffers instead of unbounded `readBytes()`?
- [ ] Are resource safety limits (`PackageSafetyLimits`) enforced during streaming inspection?
- [ ] Are package exports byte-for-byte deterministic across OS environments?

### 11. Security & Path Traversal Prevention
- [ ] Does `Opd3PathValidator` reject Zip Slip (`..`, `.`), drive letters, double slashes (`//`), and entries outside allowed layout?
- [ ] Are SHA-256 hex checksum strings validated for correct 64-char hex format?
