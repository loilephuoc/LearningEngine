# ADR-0004: Shared Platform-Neutral Core Across Desktop, Mobile, and Web

- **Status**: Accepted
- **Date**: 2026-07-23
- **Deciders**: Architecture Team, Product Architect

## Context

Learning Engine 2.0 will eventually target multiple platforms (Desktop, Android, iOS, Web). If each platform client re-implements core learning logic, scheduling algorithms, and content packaging, the application will suffer from behavioral drift, inconsistent learning outcomes, and high maintenance costs.

## Decision

We decide to **build all learning logic, Product Brain intelligence, flow management, scheduler algorithms, domain models, and persistence contracts within a single platform-neutral Kotlin shared core.**

- Compose Desktop serves as the 1.0 reference client.
- Future clients (Android, iOS, Web) will consume the same shared core via JVM dependencies, Kotlin Multiplatform (KMP) framework bindings, or Kotlin/Wasm modules.
- Shared core code must contain zero dependencies on Compose Desktop, Android SDK, platform windowing, or platform-specific UI frameworks.

## Consequences

### Positive
- Identical learning experience and scheduling behavior across all devices.
- Single codebase for bug fixes, pedagogical updates, and algorithmic improvements.
- Seamless sync and cross-platform compatibility for persisted data.

### Negative / Trade-offs
- Requires strict enforcement of platform-neutral boundaries in core modules.
- Desktop UI cannot shortcut shared contracts with desktop-only convenience APIs.
