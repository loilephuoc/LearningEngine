# Learning Engine 2.0 — System Vision

## Purpose
This document presents the overarching **System Vision** for Learning Engine 2.0. It defines the high-level system architecture, cross-platform deployment strategy, subsystem platform relationships, and the long-term vision of an intelligent, multi-device adaptive Teaching Engine.

## Responsibilities
- Provide a clear high-level architectural diagram of Learning Engine 2.0.
- Detail the sharing model of the core Kotlin/JVM engine across Desktop, Android, iOS, and Web.
- Explain the responsibilities and interactions of core platform subsystems: Package Platform, Library Platform, Workspace Platform, Learning Session, Scheduler, Statistics, AI Tutor, Sync, and Marketplace.

## Out of Scope
- Low-level database index configurations or SQL table definitions.
- Detailed UI widget composables or CSS stylesheets.

## Dependencies
- [`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md)
- Root Kotlin/JVM core build module.

---

## High-Level System Architecture

Learning Engine 2.0 separates presentation clients from core platform logic via explicit Use Case Ports:

```text
+-----------------------------------------------------------------------------------+
|                               PRESENTATION CLIENTS                                |
|  +--------------------+  +--------------------+  +------------+  +-------------+  |
|  |  Compose Desktop   |  |   Android Client   |  | iOS Client |  | Web Client  |  |
|  +---------+----------+  +---------+----------+  +-----+------+  +------+------+  |
+------------|-----------------------|-------------------|------------------|-------+
             |                       |                   |                  |
             +-----------------------+---------+---------+------------------+
                                               | (Invocation via Ports)
+----------------------------------------------v------------------------------------+
|                             CORE PLATFORM ENGINE (JVM/KMP)                        |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                             APPLICATION LAYER                               |  |
|  |  +-------------------+  +-------------------+  +-------------------------+  |  |
|  |  | Package Use Cases |  | Library Use Cases |  | Workspace Use Cases     |  |  |
|  |  +-------------------+  +-------------------+  +-------------------------+  |  |
|  |  +-----------------------------------+  +--------------------------------+  |  |
|  |  |  Learning Session Orchestration   |  |   Statistics & Analytics Use   |  |  |
|  |  +-----------------------------------+  +--------------------------------+  |  |
|  +-----------------------------------------------------------------------------+  |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                                DOMAIN LAYER                                 |  |
|  |  +---------------+ +---------------+ +---------------+ +------------------+  |  |
|  |  |  Content &    | |    Library    | |   Workspace   | | FSRS Scheduler   |  |  |
|  |  | Package Model | |   Aggregate   | |   Aggregate   | | & Review Model |  |  |
|  |  +---------------+ +---------------+ +---------------+ +------------------+  |  |
|  +-----------------------------------------------------------------------------+  |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                            INFRASTRUCTURE LAYER                             |  |
|  |  +------------------+ +-------------------+ +----------------------------+  |  |
|  |  | OPD3 Exporter/   | | JSON / File System| | FSRS Adapter &             |  |  |
|  |  | Inspector        | | Persistence     | | Media Reader             |  |  |
|  |  +------------------+ +-------------------+ +----------------------------+  |  |
|  +-----------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## Core Shared Engine Strategy

Learning Engine 2.0 adopts a **Single Core, Multi-Platform Client** strategy:

1. **Pure Kotlin Core**: All domain logic, review algorithms (FSRS), package validation, item selection, and review history logging are compiled into a pure Kotlin core.
2. **Desktop (Active Release Target)**: Compose Desktop executes directly against the JVM core module, providing immediate full-featured desktop capabilities for macOS, Windows, and Linux.
3. **Mobile & Web Expansion (Future Platforms)**: Android, iOS (via Kotlin Multiplatform / Native), and Web (via Kotlin/WASM) reuse 100% of the application use cases and domain logic, swapping only UI rendering and platform-specific IO drivers (e.g., Android Room vs JVM JSON).

---

## Subsystem Architecture & Platform Relationships

### 1. Package Platform
- **Role**: Ingestion, validation, media bundle collection, byte-for-byte deterministic export, and inspection of `.opd3` package files.
- **Relationship**: Serves as the primary content import boundary. Emits canonical package structures consumed by the Library Platform.

### 2. Library Platform
- **Role**: Cataloging installed packages, managing global content collections, organizing tags, and tracking package versions.
- **Relationship**: Maintains the registry of installed content. Supplies topic items to Workspace Platform upon binding.

### 3. Workspace Platform
- **Role**: Managing active learner workspaces, selecting study sources, binding specific topics/items, and isolating learner study contexts.
- **Relationship**: Connects Library content with active study configurations. Serves as the sole content supplier for Learning Sessions.

### 4. Learning Session & Scheduler
- **Role**: Orchestrating active study sessions, queue planning, sibling item avoidance, presentation rhythm, and integrating the FSRS review algorithm.
- **Relationship**: Reads items from Workspace, updates memory rating states, logs completed reviews, and notifies Statistics.

### 5. Statistics & Analytics
- **Role**: Calculating retention metrics, study streak tracking, due item projections, mastery heatmaps, and historical performance query APIs.
- **Relationship**: Consumes immutable Review History logs written during Learning Sessions.

### 6. AI Tutor (Future Expansion)
- **Role**: Intelligent sidecar generating contextual hints, mnemonic assistance, adaptive explanations, and personalized practice prompts.
- **Relationship**: Interacts via Application Ports without mutating raw domain items directly.

### 7. Sync & Marketplace (Future Expansion)
- **Role**: Multi-device state synchronization and peer-to-peer/cloud package publishing and discovery.
- **Relationship**: Integrates via package platform export/import interfaces and application sync ports.

---

## Future Evolution
The System Vision guides the progression of Learning Engine 2.0 across upcoming roadmap phases:
- **Phase 1-2**: Package Platform & Core Engine Hardening (Desktop focus).
- **Phase 3-4**: Library Platform, Workspace, & Advanced FSRS Session Orchestration.
- **Phase 5+**: Mobile client deployment, Sync engine, and Marketplace integration.

---

## Architecture Notes
- Core domain boundaries must remain clean and unpolluted by UI framework details.
- All inter-platform communication relies strictly on Application Ports and Use Case APIs.
