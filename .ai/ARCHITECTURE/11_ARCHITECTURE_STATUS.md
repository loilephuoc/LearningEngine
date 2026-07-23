# Learning Engine 2.0 — Architecture Status Matrix

## Purpose
This document provides the official status registry of all platform subsystems within Learning Engine 2.0. It defines the implementation maturity of each component, mapping documented blueprints to active repository capabilities.

## Status Classifications
- **Implemented**: Fully implemented in production Kotlin code, tested, and verified.
- **Partially Implemented**: Core components exist in code; advanced features documented in blueprint.
- **Blueprint**: Fully specified in `.ai/BLUEPRINTS/`; implementation targeted for upcoming roadmap phases.
- **Planned**: Architecturally scoped in System Vision; detailed blueprint pending.
- **Deferred**: Future vision target; deferred until post-beta milestones.

---

## Subsystem Architecture Status Matrix

| Subsystem | Status | Current Code Capabilities | Target Roadmap Phase | Key Components |
|---|---|---|---|---|
| **Package Platform** | **Implemented** | OPD3 Export, Media Packaging, Streaming Inspection, Verifier, Path Safety, Safety Limits | Phase 1 (Complete) | `Opd3PackageExporter`, `Opd3PackageInspector`, `Opd3PackageVerifier`, `Opd3PathValidator` |
| **Library Platform** | **Partially Implemented** | Topic cataloging, legacy JSON/PKG import, package metadata tracking | Phase 3 (Conflict-Aware Import) | `LegacyPairCanonicalConverter`, `ContentCollection`, `CollectionRepository` |
| **Workspace Platform** | **Blueprint** | Defined in `.ai/BLUEPRINTS/WorkspacePlatform.md` | Phase 4 (Workspace Capability) | `Workspace`, `StudySource`, `WorkspaceRepository` |
| **Learning Session** | **Partially Implemented** | Review queue, prompt/reveal rhythm, item presentation | Phase 4 (Session Refactoring) | `LearningEngine`, `StudyQueueFactory`, `LearningSession` |
| **Scheduler** | **Implemented** | FSRS algorithm, stability & difficulty calculations, interval updates | Phase 1 (Complete) | `FsrsScheduler`, `FSRSState`, `Rating` |
| **Statistics** | **Partially Implemented** | Progress tracking, review history logging, basic dashboard metrics | Phase 4 (Analytics Expansion) | `ReviewHistoryStore`, `LearningProgressService` |
| **AI Tutor** | **Planned** | Conceptual integration ports defined in System Vision | Phase 5+ | AI Hint Ports, Context Generators |
| **Sync** | **Deferred** | Local-first persistence; vector clock architecture scoped | Phase 5+ | Change Data Capture, Sync Adapters |
| **Marketplace** | **Deferred** | OPD3 byte-deterministic export ready for package sharing | Phase 5+ | Remote Repository Adapters, Package Discovery |

---

## Governance & Status Updates
Subsystem status transitions must be updated in this document whenever a capability is completed and verified by passing automated tests.
