# Learning Engine 2.0

Learning Engine 2.0 is an adaptive **Teaching Engine**—not a flashcard app or Anki clone. It features a platform-neutral core with Product Brain pedagogical intelligence, FSRS-oriented memory scheduling, reusable Learning Flow templates, JSON persistence, content-package import/export, and a reference Compose Desktop client.

## Repository Knowledge System & Constitution

The core product philosophy, architecture laws, and AI design rules live in [`docs/`](docs/):

- **Product Philosophy**: [`docs/PRODUCT_PHILOSOPHY.md`](docs/PRODUCT_PHILOSOPHY.md)
- **Repository Constitution**: [`docs/REPOSITORY_CONSTITUTION.md`](docs/REPOSITORY_CONSTITUTION.md)
- **Product Brain Framework**: [`docs/PRODUCT_BRAIN.md`](docs/PRODUCT_BRAIN.md)
- **Product Brain Specification**: [`docs/PRODUCT_BRAIN_SPECIFICATION.md`](docs/PRODUCT_BRAIN_SPECIFICATION.md)
- **Knowledge Model Specification**: [`docs/KNOWLEDGE_MODEL.md`](docs/KNOWLEDGE_MODEL.md)
- **Learning Experience Architecture**: [`docs/LEARNING_EXPERIENCE_ARCHITECTURE.md`](docs/LEARNING_EXPERIENCE_ARCHITECTURE.md)
- **Learning Scene Framework**: [`docs/LEARNING_SCENE_FRAMEWORK.md`](docs/LEARNING_SCENE_FRAMEWORK.md)
- **Learning Scene Library**: [`docs/LEARNING_SCENE_LIBRARY.md`](docs/LEARNING_SCENE_LIBRARY.md)
- **Learning Principles**: [`docs/LEARNING_PRINCIPLES.md`](docs/LEARNING_PRINCIPLES.md)
- **System Overview**: [`docs/SYSTEM_OVERVIEW.md`](docs/SYSTEM_OVERVIEW.md)
- **Cross-Platform Strategy**: [`docs/CROSS_PLATFORM_STRATEGY.md`](docs/CROSS_PLATFORM_STRATEGY.md)
- **AI Design Rules**: [`docs/AI_DESIGN_RULES.md`](docs/AI_DESIGN_RULES.md)
- **Architecture Specification**: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- **Architectural Decisions (ADRs)**: [`docs/adr/`](docs/adr/)







## Requirements

- JDK 21
- Gradle Wrapper

## Build and test

From the repository root:

```powershell
.\gradlew clean test
```

Full project build:

```powershell
.\gradlew build
```

## Run

Root CLI:

```powershell
.\gradlew run --args="<legacy-json-path>"
```

Legacy package import task:

```powershell
.\gradlew legacyPackageImport --args="<arguments>"
```

Desktop application:

```powershell
.\gradlew :desktop:run
```

## Repository workflow

- `main`: stable milestone branch.
- `develop`: active integration branch and the baseline for new batches.
- Git is the only source of truth.
- Apply ZIP files are transport artifacts, not repository content.
- Store and extract apply packages outside the repository, under:

```text
C:\Users\M72Q\LearningEngine_Batches\
```

Run an extracted batch script from the repository root:

```powershell
& "C:\Users\M72Q\LearningEngine_Batches\BatchXX_APPLY\apply_batch.ps1"
```
