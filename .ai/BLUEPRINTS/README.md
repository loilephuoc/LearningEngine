# Learning Engine 2.0 — Subsystem Blueprints Index

## Purpose
This directory contains detailed **Subsystem Architecture Blueprints** for the major platform components of Learning Engine 2.0. Blueprints specify the exact domain model structures, application use cases, infrastructure adapters, security constraints, and integration boundaries for each platform subsystem.

---

## Subsystem Blueprint Index

1. **[`PackagePlatform.md`](PackagePlatform.md)**
   - Technical blueprint for OPD3 Package Generation, Media Packaging, Streaming Inspection, Verification, and Deterministic Byte Export.

2. **[`LibraryPlatform.md`](LibraryPlatform.md)**
   - Technical blueprint for Package Installation, Library Cataloging, Collection Organization, Versioning, Tag Filtering, and Conflict-Aware Import.

3. **[`WorkspacePlatform.md`](WorkspacePlatform.md)**
   - Technical blueprint for Active Learner Workspaces, Study Source Binding, Selection Settings, and Workspace State Management.

4. **[`LearningSessionPlatform.md`](LearningSessionPlatform.md)**
   - Technical blueprint for Interactive Learning Session Orchestration, Review Queue Planning, Sibling Item Spacing, FSRS Scheduler Integration, Review Logging, and Performance Statistics.

---

## Blueprint Writing Standard & Template

Every future platform blueprint added to this directory MUST follow this standard structure:

1. **Purpose**: Clear summary of what the platform subsystem accomplishes.
2. **Responsibilities**: Explicit list of domain and application responsibilities.
3. **Out of Scope**: Explicit list of behaviors handled by other bounded contexts.
4. **Dependencies**: External ports, domain models, and system components required.
5. **Subsystem Architecture & Components**: Detailed component diagram and class layout.
6. **Data Models & Contracts**: Explicit domain entities, value objects, and schemas.
7. **Security & Resource Limits**: Resource safety limits, path security, and validation rules.
8. **Future Evolution**: Roadmap progression and multi-platform expansion.
9. **Architecture Notes**: Key implementation rules and test verification expectations.
