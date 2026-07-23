# Cross-Platform Strategy — Shared Engine Architecture

## 1. Executive Summary

Learning Engine 2.0 uses a **Shared Core Architecture**. Desktop (Compose Desktop) is the initial reference client where the core engine and Product Brain mature. Future target platforms—**Android**, **iOS**, and **Web**—will consume the exact same platform-neutral engine binaries without re-implementing teaching, scheduling, or flow logic.

---

## 2. Architecture Layering Across Platforms

```text
+-------------------------------------------------------------------------+
|                         PRESENTATION CLIENTS                            |
|  +------------------+  +-----------------+  +---------+  +-----------+  |
|  | Compose Desktop  |  | Android Jetpack |  | iOS SwiftUI |  | Web UI    |  |
|  +------------------+  +-----------------+  +---------+  +-----------+  |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|                        SHARED KOTLIN ENGINE CORE                        |
|  +-------------------------------------------------------------------+  |
|  | Product Brain (ObjectivePolicy, StrategyPlanner, ProductBrainPlanner)|  |
|  +-------------------------------------------------------------------+  |
|  | Learning Flow (InstantiationService, FlowPlanner, FlowController)  |  |
|  +-------------------------------------------------------------------+  |
|  | Scheduler & Memory Engine (FSRS Calculator, Review Transition)    |  |
|  +-------------------------------------------------------------------+  |
|  | Persistence & Packaging (Store Contracts, JSON Codecs, OPD3)      |  |
|  +-------------------------------------------------------------------+  |
|  | Domain Models (StudySession, LearningContent, ReviewEvent)        |  |
|  +-------------------------------------------------------------------+  |
+-------------------------------------------------------------------------+
```

---

## 3. Core Principles of Cross-Platform Strategy

1. **Zero Pedagogical Duplication**: Teaching rules, objective policies, strategy selection, scene templates, and scheduling math live strictly in the Shared Core. No platform client may implement custom scheduling heuristics or flow rules.
2. **Desktop as Reference Client**: Desktop is the primary release target for 1.0. Shared contracts are battle-tested on Desktop before expanding to Mobile or Web.
3. **Native Presentation, Shared Intelligence**: Platform clients own rendering, native UI components, screen focus, platform media APIs, and input gestures. They do not own learning logic.
4. **Platform Adaptation without Contract Pollution**: Mobile platforms may feature touch gestures or lock-screen integration, but these are presentation adapters. Shared engine contracts must not be distorted for platform-specific quirks.

---

## 4. Platform Rollout Roadmap

- **Desktop (Compose Desktop)**: Active reference client (Phase 1–7).
- **Android**: Post-1.0 client consuming Shared Core via Kotlin/JVM or KMP bindings.
- **iOS**: Post-1.0 client consuming Shared Core via Kotlin Multiplatform (KMP) framework output.
- **Web**: Post-1.0 client consuming Shared Core via Kotlin/Wasm or JS targets.
