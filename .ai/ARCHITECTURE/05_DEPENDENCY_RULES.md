# Learning Engine 2.0 — Dependency Rules

## Purpose
This document defines the strict **Dependency Direction Rules** governing package imports, module boundaries, and architectural layer isolation within Learning Engine 2.0.

## Responsibilities
- Specify exact allowed and forbidden import paths across system layers.
- Detail compile-time and runtime dependency boundaries.
- Provide concrete code examples of valid vs invalid dependencies.

## Out of Scope
- External Gradle third-party library dependency resolution syntax.

## Dependencies
- [`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md)
- [`03_CONTEXT_MAP.md`](03_CONTEXT_MAP.md)

---

## Architectural Layer Dependency Rules

Dependencies must strictly point **inward** toward the Domain Layer:

```text
+-----------------------------------------------------------------------------+
|                         PRESENTATION LAYER (Desktop/Mobile/Web)            |
|  Imports: Application Layer Use Cases, ViewModels, UI Frameworks            |
+--------------------------------------|--------------------------------------+
                                       | (Allowed Import Direction)
                                       v
+-----------------------------------------------------------------------------+
|                         APPLICATION LAYER (Use Cases & Ports)               |
|  Imports: Domain Layer Models, Domain Services                              |
+--------------------------------------|--------------------------------------+
                                       | (Allowed Import Direction)
                                       v
+-----------------------------------------------------------------------------+
|                         DOMAIN LAYER (Entities, Aggregates, Value Objects)  |
|  Imports: Pure Standard Library ONLY (Zero Framework / IO Dependencies)     |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
|                         INFRASTRUCTURE LAYER (Adapters & Persistence)       |
|  Imports: Application Ports, Domain Layer Models, IO / DB Libraries         |
|  Implements: Application Ports & Domain Repository Interfaces               |
+-----------------------------------------------------------------------------+
```

---

## Package Import Matrix

| Package Boundary | May Import From | FORBIDDEN Imports |
|---|---|---|
| `vn.loi.learning.domain.*` | `kotlin.*`, `java.util.*`, `java.time.*` | `application.*`, `infrastructure.*`, `desktop.*`, `androidx.*`, `androidx.compose.*`, `kotlinx.serialization.*` |
| `vn.loi.learning.application.*` | `domain.*`, `kotlin.*`, `java.*` | `infrastructure.*`, `desktop.*`, `androidx.compose.*` |
| `vn.loi.learning.infrastructure.*` | `application.*`, `domain.*`, `kotlin.*`, `java.*`, `kotlinx.serialization.*`, `java.util.zip.*` | `desktop.*`, `androidx.compose.*` |
| `vn.loi.learning.desktop.*` | `application.*`, `domain.*`, `androidx.compose.*`, `kotlinx.coroutines.*` | Direct internal `infrastructure.*` implementation details (except at Desktop Composition Root `Main.kt`) |

---

## Forbidden Dependency Scenarios & Examples

### 1. Domain importing Infrastructure or Serialization
- ❌ **FORBIDDEN**:
  ```kotlin
  // Inside vn.loi.learning.domain.content.model.Content.kt
  import kotlinx.serialization.Serializable // FORBIDDEN! Domain must not depend on Serialization
  import java.io.File // FORBIDDEN! Domain must not depend on Filesystem IO
  ```
- ✅ **ALLOWED**:
  ```kotlin
  // Inside vn.loi.learning.domain.content.model.Content.kt
  package vn.loi.learning.domain.content.model
  
  data class Content(
      val id: ContentId,
      val type: ContentType,
      val text: ContentText
  )
  ```

### 2. Application Use Case depending on Desktop UI or Compose
- ❌ **FORBIDDEN**:
  ```kotlin
  // Inside vn.loi.learning.application.contentlibrary.ExportPackageUseCase.kt
  import androidx.compose.runtime.State // FORBIDDEN! Application must not depend on UI State
  ```
- ✅ **ALLOWED**:
  ```kotlin
  // Inside vn.loi.learning.application.contentlibrary.ExportPackageUseCase.kt
  package vn.loi.learning.application.contentlibrary

  import vn.loi.learning.domain.content.topic.model.TopicId

  class ExportPackageUseCase(
      private val packageExporter: Opd3PackageExporter
  ) {
      fun execute(topicId: TopicId): ExportResult { ... }
  }
  ```

### 3. UI Composables performing direct Database / Serialization operations
- ❌ **FORBIDDEN**:
  ```kotlin
  // Inside desktop/vn/loi/learning/desktop/ui/LibraryScreen.kt
  @Composable
  fun LibraryScreen() {
      val jsonBytes = File("package.opd3").readBytes() // FORBIDDEN! UI doing direct IO
  }
  ```
- ✅ **ALLOWED**:
  ```kotlin
  // Inside desktop/vn/loi/learning/desktop/ui/LibraryScreen.kt
  @Composable
  fun LibraryScreen(viewModel: LibraryViewModel) {
      val state by viewModel.uiState.collectAsState()
      Button(onClick = { viewModel.onExportRequested() }) { ... }
  }
  ```

---

## Enforcement Mechanisms
1. **Gradle Module Boundaries**: The Gradle build enforces compile-time isolation between presentation modules and engine internals.
2. **Automated Verification**: Architectural checks and `.\gradlew.bat clean test` verify that no forbidden imports cross layer boundaries.

---

## Future Evolution
When Kotlin Multiplatform (KMP) targets are introduced, domain and application packages remain 100% platform-neutral, while platform-specific infrastructure adapters are isolated within designated target modules.

---

## Architecture Notes
- Any PR introducing forbidden imports across layer boundaries will be rejected automatically.
