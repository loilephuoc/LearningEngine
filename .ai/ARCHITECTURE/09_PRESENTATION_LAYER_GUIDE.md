# Learning Engine 2.0 — Presentation Layer Guide

## Purpose
This document provides UI and presentation client implementation guidance for the **Presentation Layer** (`vn.loi.learning.desktop.*` and future mobile/web clients) of Learning Engine 2.0.

## Responsibilities
- Render user interfaces using modern declarative UI frameworks (Compose Desktop, Compose Multiplatform, Web WASM).
- Implement Unidirectional Data Flow (UDF) using ViewModels / StateHolders and `StateFlow`.
- Translate user actions into Application Commands and Queries.
- Enforce Law 5 of the Constitution: **Zero Business Logic in UI**.

## Out of Scope
- Domain entity validation or FSRS scheduling logic.
- Raw file reading or JSON serialization.

## Dependencies
- [`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md)
- [`07_APPLICATION_LAYER_GUIDE.md`](07_APPLICATION_LAYER_GUIDE.md)

---

## Presentation Layer Architecture

The Desktop presentation client is structured into modular feature packages:

```text
desktop/src/main/kotlin/vn/loi/learning/desktop/
├── Main.kt                 # Composition Root & Window Entry Point
├── ui/
│   ├── components/         # Reusable UI Design Tokens & Atomic Widgets
│   ├── dashboard/          # Dashboard Screen & ViewModel
│   ├── library/            # Package Catalog Screen & ViewModel
│   ├── study/              # Active Learning Session Screen & ViewModel
│   ├── history/            # Review History Log Screen & ViewModel
│   └── statistics/         # Analytics Heatmap Screen & ViewModel
```

---

## Presentation Principles & Rules

### 1. Zero Business Logic in UI Law
- UI composables are strictly declarative views. They must NOT contain scheduling math, card selection algorithms, package validation, or file parsing.
- ❌ **BAD**:
  ```kotlin
  // Inside Composable
  if (card.reviewCount > 5 && rating == Rating.GOOD) { nextInterval = interval * 2.5 }
  ```
- ✅ **GOOD**:
  ```kotlin
  // Inside Composable
  Button(onClick = { viewModel.submitRating(Rating.GOOD) }) { Text("Good") }
  ```

### 2. Unidirectional Data Flow (UDF)
- UI state flows **downward** from StateHolders/ViewModels to Composables as immutable `StateFlow<ScreenState>`.
- User events flow **upward** from Composables to ViewModels as intent invocations.

```text
+-------------------------------------------------------------+
|                      VIEWMODEL / STATE                      |
|                                                             |
|   StateFlow<ScreenUiState>              Event Handlers      |
|          |                                   ^              |
|          v (State Flows Down)                | (Events Up)  |
|                                                             |
|                      COMPOSABLE UI                          |
+-------------------------------------------------------------+
```

### 3. Composition Root Wiring (`Main.kt`)
- All dependency wiring (instantiating repositories, use cases, view models) occurs explicitly at the Composition Root (`Main.kt`). No hidden service locators or global singletons.

### 4. Accessibility & Keyboard Control
- Presentation screens must support keyboard shortcuts (e.g., Space for reveal, keys `1-4` for ratings `AGAIN`, `HARD`, `GOOD`, `EASY`) to optimize rapid study rhythms.

---

## Future Evolution
Compose Desktop serves as the foundation. Future Android, iOS, and Web WASM clients will re-use 100% of ViewModels and Application Use Cases, replacing only platform UI entry points.

---

## Architecture Notes
- UI state models must be immutable data classes.
- UI composable rendering must be tested via Compose UI tests where applicable.
