# Learning Engine 2.0 — Ubiquitous Language Dictionary

## Purpose
The **Ubiquitous Language Dictionary** establishes the unambiguous, official domain vocabulary used consistently across source code, class names, variable identifiers, documentation, test suites, and team discussions for Learning Engine 2.0.

## Responsibilities
- Provide rigorous definitions for all primary domain terms.
- Identify the owning bounded context for each term.
- Map related concepts and provide concrete domain examples.
- Prevent vocabulary fragmentation and domain concept drift.

## Out of Scope
- Code syntax conventions (see [`10_IMPLEMENTATION_RULES.md`](10_IMPLEMENTATION_RULES.md)).
- Ephemeral sprint jargon.

## Dependencies
- Strategic Domain-Driven Design (DDD) principles.

---

## Ubiquitous Language Dictionary

### Package (OPD3 Package)
- **Definition**: An immutable, single-file `.opd3` ZIP archive containing metadata (`metadata.json`), content units (`contents.json`), learning items (`learning-items.json`), media manifests (`media-manifest.json`), raw media assets (`media/*`), and SHA-256 integrity checksums (`manifest.json`).
- **Owner**: Package Platform Bounded Context
- **Related Concepts**: Installed Package, Canonical Topic Package, Media Bundle.
- **Example**: `english_vocabulary_n1.opd3` containing 500 word items and audio assets.

### Installed Package
- **Definition**: An OPD3 package that has been validated, extracted/registered into local application persistence, and made available for library cataloging.
- **Owner**: Library Platform Bounded Context
- **Related Concepts**: Package, Library, Collection.
- **Example**: An entry in local storage with `installedAt` timestamp, version `1.0`, and active status.

### Library
- **Definition**: The global catalog of all installed topic packages, user collections, tags, and content metadata stored within a learner's local database.
- **Owner**: Library Platform Bounded Context
- **Related Concepts**: Collection, Installed Package, Workspace.
- **Example**: The central repository holding installed Japanese N3, English Grammar, and Medical Terminology packages.

### Collection
- **Definition**: A user-defined logical grouping of installed packages or topics within the Library (e.g., "JLPT Prep", "Medical School Year 1").
- **Owner**: Library Platform Bounded Context
- **Related Concepts**: Library, Topic, Tag.
- **Example**: A collection named "JLPT N2 Vocabulary" grouping 5 separate installed vocabulary packages.

### Workspace
- **Definition**: An active study environment aggregate configured by a learner, serving as the sole source of items for active study sessions.
- **Owner**: Workspace Platform Bounded Context
- **Related Concepts**: Study Source, Learning Session, Learning Item.
- **Example**: "Morning Review Workspace" configured with 3 active vocabulary topics and a daily limit of 50 review items.

### Learning Item
- **Definition**: The fundamental testable unit of study, binding a Content item to a specific learning mode (e.g., Meaning Recognition, Spelled Recall, Listening Practice).
- **Owner**: Content & Study Domain
- **Related Concepts**: Content, Learning Mode, Review, Memory State.
- **Example**: `LearningItem(id="item-101", contentId="c-50", mode=MEANING_RECOGNITION)`.

### Topic
- **Definition**: A coherent unit of subject matter containing a structured list of Contents and corresponding Learning Items.
- **Owner**: Content Domain
- **Related Concepts**: TopicId, Content, Package.
- **Example**: "Kanji N5 Lesson 1" topic containing 20 Kanji character contents.

### Content
- **Definition**: The underlying educational knowledge record (e.g., word, phrase, sentence, grammar rule, image, audio link) independent of study mode.
- **Owner**: Content Domain
- **Related Concepts**: ContentId, ContentType, ContentText, Learning Item.
- **Example**: `Content(id="c-10", type=WORD, text=ContentText("neko", "cat"))`.

### Study Source
- **Definition**: A specific topic or collection bound to a Workspace from which learning items are drawn during study sessions.
- **Owner**: Workspace Platform Bounded Context
- **Related Concepts**: Workspace, Topic, Library.
- **Example**: A study source binding Topic `t-jlpt-n3` to Workspace `w-main`.

### Learning Session
- **Definition**: An active, stateful study workflow during which a learner reviews a queued sequence of Learning Items according to a scheduler rhythm.
- **Owner**: Learning Session Bounded Context
- **Related Concepts**: Review Queue, Scheduler, Review, FSRS.
- **Example**: A 15-minute active review session presenting 20 due cards with immediate rating prompts.

### Review
- **Definition**: A single interaction event within a Learning Session where a learner is presented a Learning Item, responds, rates their recall (e.g., AGAIN, HARD, GOOD, EASY), and receives feedback.
- **Owner**: Learning Session Bounded Context
- **Related Concepts**: Rating, Review Log, Memory State.
- **Example**: A user rating a card as "GOOD", triggering an update to its FSRS stability and difficulty scores.

### Scheduler (FSRS Scheduler)
- **Definition**: The mathematical scheduling engine (Free Spaced Repetition Scheduler) responsible for computing card due dates, memory stability, difficulty, and optimal review intervals based on rating history.
- **Owner**: Scheduler Domain
- **Related Concepts**: FSRS State, Memory Stability, Difficulty, Retrievability.
- **Example**: `FsrsScheduler.calculateNextState(previousState, rating, elapsedTime)`.

### Review History
- **Definition**: The immutable append-only historical log of all completed Review events, including timestamps, ratings, elapsed study time, and FSRS state transitions.
- **Owner**: Analytics & Review History Bounded Context
- **Related Concepts**: Review Log, Statistics, Mastery Metrics.
- **Example**: A persistent log record storing `ReviewLog(itemId="i-1", rating=GOOD, reviewTime=1700000000)`.

### Statistics
- **Definition**: Aggregated metrics, streak tracking, retention rates, mastery heatmaps, and forecast projections derived from Review History.
- **Owner**: Analytics Bounded Context
- **Related Concepts**: Review History, Retention Rate, Heatmap.
- **Example**: A calculated metric showing 94.2% retention over 30 days and a 14-day study streak.

### Version
- **Definition**: The explicit schema version (e.g., `1.0`) and package content version (e.g., `1`) ensuring package integrity and backward compatibility.
- **Owner**: Platform Core
- **Related Concepts**: Package, Schema Envelope.
- **Example**: `SchemaVersion("1.0")` in `metadata.json`.

### Repository (Domain Port)
- **Definition**: An abstraction interface in the Domain/Application layer defining data access operations for an Aggregate without exposing persistence mechanisms.
- **Owner**: Architecture Core
- **Related Concepts**: Domain Port, Infrastructure Adapter.
- **Example**: `interface WorkspaceRepository { fun findById(id: WorkspaceId): Workspace? }`.

### Aggregate
- **Definition**: A cluster of domain objects (Entities and Value Objects) bound together by a root Entity, evaluated as a single unit for data changes and consistency enforcement.
- **Owner**: Domain Layer
- **Related Concepts**: Aggregate Root, Entity, Value Object.
- **Example**: `Workspace` Aggregate Root controlling its internal `StudySource` list and selection settings.

### Value Object
- **Definition**: An immutable domain object defined entirely by its attribute values, possessing no conceptual identity.
- **Owner**: Domain Layer
- **Related Concepts**: Entity, Immutability.
- **Example**: `TopicId("topic-101")`, `ContentText("apple", "táo")`.

### Entity
- **Definition**: A domain object defined by its unique thread of identity over time, rather than its attribute values.
- **Owner**: Domain Layer
- **Related Concepts**: Aggregate Root, Value Object.
- **Example**: `Content` with unique `ContentId`.

---

## Future Evolution
New terms will be added to this dictionary as future Bounded Contexts (Sync, Marketplace, AI Tutor) are designed, maintaining single domain vocabulary integrity.

---

## Architecture Notes
- Class names, function names, and variable names in Kotlin source MUST strictly reflect these Ubiquitous Language terms.
