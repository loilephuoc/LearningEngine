# Library and Topic Persistence Architecture Audit

Audit date: 2026-07-23
Verified baseline: `develop` at `e6a14a2` (`feat: desktop alpha 04 session completion`)

## Scope and evidence standard

This audit covers the current OPD3 bundle path, legacy JSON/PKG import paths, installed-package
and library models, study-session recovery, scheduler and learner-progress ownership, JSON
persistence, package upgrade/uninstall behavior, and the Desktop lesson consumer boundary.

The repository is the only evidence source. “Exists” below means a production implementation
and, where stated, a test establish the capability. A reusable lower-level writer is not counted
as an installed-topic export product flow. Content package state and learner progress state are
treated as separate contracts throughout.

## Beta-L01 resolution update

Beta-L01 resolves the topic-identity and topic-selective active-session gaps identified in F06,
F09, F11, F12 and part of F19:

- installed `ContentPackage` records now persist a durable `TopicId` distinct from release
  `PackageId`;
- existing package records without the optional field derive a deterministic identity from
  logical package name and format, and compatible package replacement carries the existing ID;
- `StudySession` records optionally persist `TopicId`, allowing active checkpoint lookup by
  `(LearnerId, TopicId)` without duplicating item-level memory/review/scheduler state;
- Desktop switching clears transient projection and asks Application to resume the selected
  topic before creating a new scoped session;
- mapper/store compatibility and installed OPD3/Desktop A → B → A restart behavior are covered.

The remaining export blockers are unchanged: OPD3 does not yet serialize the explicit topic ID,
installed-topic export/media assembly is absent, and conflict-aware update must define how a
portable package preserves the ID. F07 library identity, F08 legacy content/item ID churn, and
the delete/archive/order/collection findings remain open. Beta-L02 is the next capability.

## Executive assessment

The repository can read modern ZIP-based OPD3 bundles and can import legacy same-name JSON/PKG
pairs through the same persisted import service. It also has reusable factories that serialize
package metadata, content, learning items and a hash manifest, plus ZIP and directory writers.
That is a real canonical OPD3 content writer at the application/infrastructure boundary.

However, there is no production orchestration that selects an installed topic, reconstructs its
complete content and media, and exports one portable OPD3 file. The existing export payload is
caller-assembled, library membership is not serialized, and referenced media bytes are not added
to the export plan. Learner state is correctly absent from the OPD3 content payload, but current
content identities and package/library ownership do not yet provide a safe update/re-import
contract.

The largest architectural gap is the absence of a durable `TopicId`. Desktop currently infers a
lesson/topic from content metadata and display text. Learner memory is durable per
`(learnerId, learningItemId)`, while session scope is a set of `ContentId` values. This permits
progress to survive only when learning-item IDs remain unchanged; it does not establish
learner-topic ownership, topic-selective resume, safe replacement, or conflict-aware migration.

### Product-requirement disposition

| # | Requirement | Current disposition |
|---|---|---|
| 1 | One legacy topic is a same-name JSON + PKG pair | Supported by the persisted compatibility importer; the dedicated legacy scanner has a duplicate-case ambiguity gap. |
| 2 | Convert that pair into one canonical OPD3 package | Building blocks exist, but no end-to-end conversion/export use case exists. |
| 3 | Export an installed topic as one OPD3 file | Not implemented as a product capability; ZIP writer exists but media and installed-topic assembly are missing. |
| 4 | Modern OPD3 and legacy pair import coexist | Implemented through format detection and compatibility routing. |
| 5 | Independent progress/resume per learner-topic | Item progress is independent; topic identity and topic-selective resume are missing. |
| 6 | Switching topics persists current and restores selected | Session/queue writes persist, but switching can create multiple active sessions and recovery restores only the latest learner session. |
| 7 | Re-import/update preserves compatible progress | Not guaranteed; duplicate validation rejects overlap and identity generation changes under several normal operations. |
| 8 | Delete/archive/replacement does not silently destroy progress | Not satisfied; archive is absent and uninstall/upgrade do not coordinate learner state or active queues. |
| 9 | Topic order and collection membership persist independently | Collection membership is persisted separately, but uses unordered package-ID sets; topic order is not modeled. |

## Current capability inventory

### OPD3 capabilities that exist

- Modern `.opd3` discovery and import:
  `JvmDirectoryPackageScanner`, `JvmPackageFormatDetector`,
  `JvmOpd3ArchiveReader`, `BundlePackageReader`,
  `BundlePackageContentImporter`, and `PackageImportService`.
- Structural and integrity validation:
  required entries, schema/format checks, archive limits, path validation, declared-file checks,
  and SHA-256 manifest verification.
- Legacy compatibility:
  `PackageContentImporterCompat` routes a binary OPD3-magic `.pkg` to its same-basename JSON,
  while legacy ZIP `.pkg` remains supported.
- Persisted installation:
  imported libraries, contents, learning items, package registration and catalog membership are
  written in a transaction.
- Canonical content-bundle writing primitives:
  `PackageExportPlanFactory` emits `metadata.json`, `contents.json`,
  `learning-items.json`, and `manifest.json`; `PackageZipWriter` writes the bundle as one ZIP.
- Deterministic content and learning-item serialization, manifest hashing, and import/export
  round-trip tests for the four core entries.

### What does not yet exist

- A durable topic aggregate or value object connecting package, library, lesson content,
  learner progress, session resume, ordering and collections.
- An installed-topic export query/use case.
- Export of referenced media bytes into the OPD3 archive.
- Serialization of library/collection membership in the current bundle.
- A legacy-pair-to-OPD3 conversion command or Desktop flow.
- Conflict-aware re-import/update that maps compatible identities and retains progress.
- Archive semantics, progress-aware deletion, or replacement migration.
- Persisted topic order.

## Detailed findings

### F01 — Modern and legacy import coexist behind one compatibility boundary

**Source evidence**

- `src/main/kotlin/vn/loi/learning/infrastructure/contentpackaging/ContentPackageImportFactory.kt`
  builds the scanner, descriptor reader and compatibility importer.
- `src/main/kotlin/vn/loi/learning/infrastructure/contentpackaging/PackageContentImporterCompat.kt`
  routes ZIP `.opd3`, ZIP `.pkg`, and OPD3-magic binary `.pkg` to distinct readers.
- `src/main/kotlin/vn/loi/learning/infrastructure/contentpackaging/JvmPackageFormatDetector.kt`
  detects by content signature rather than extension alone.
- `src/main/kotlin/vn/loi/learning/application/contentpackaging/PackageImportService.kt`
  validates and persists each candidate through the same application boundary.

**Assessment:** Requirement 4 is implemented. Modern OPD3 and both legacy package forms can
coexist without forcing a destructive migration.

**Risk:** Compatibility behavior is split across format detection, extension routing and two
legacy scanners, so later changes can accidentally make the paths disagree.

**Recommendation:** Retain `PackageContentImporterCompat` as the single production routing
boundary and add conversion/re-import contract tests through `PackageImportService`, not only
reader-level tests.

**Priority:** P1
**Must be fixed before OPD3 export:** No

### F02 — Same-name legacy JSON/PKG matching is implemented, with one ambiguity inconsistency

**Source evidence**

- `src/main/kotlin/vn/loi/learning/infrastructure/contentpackaging/JvmOpd3PairResolver.kt`
  resolves `<base>.json` case-insensitively and rejects zero or multiple matches.
- `src/main/kotlin/vn/loi/learning/application/contentpackaging/Opd3BinaryPackageExceptions.kt`
  exposes explicit missing- and ambiguous-pair failures.
- `src/main/kotlin/vn/loi/learning/infrastructure/contentpackaging/JvmLegacyPackageScanner.kt`
  groups direct-child `.json` and `.pkg` files by lower-cased basename, but uses
  `associateBy`; case-variant duplicates overwrite one another and orphans are omitted.

**Assessment:** Requirement 1 is satisfied on the main compatibility path. Matching is
same-directory, same-basename and case-insensitive. The standalone legacy scanner is less safe
than `JvmOpd3PairResolver`.

**Risk:** A directory containing case-variant duplicates can select a file implicitly in the
dedicated legacy workflow. Orphan omission can hide an operator error.

**Recommendation:** Reuse the resolver’s explicit zero/one/many contract in every legacy-pair
entry point and report orphan/ambiguous pairs as structured diagnostics.

**Priority:** P1
**Must be fixed before OPD3 export:** Yes, for reliable legacy conversion

### F03 — A canonical four-file OPD3 content writer exists, but installed-topic export does not

**Source evidence**

- `src/main/kotlin/vn/loi/learning/application/contentpackaging/PackageExportPlanFactory.kt`
  creates the four canonical entries and their hashes.
- `src/main/kotlin/vn/loi/learning/application/contentpackaging/PackageExportService.kt`
  requires a caller-provided `PackageExportPayload` and destination.
- `src/main/kotlin/vn/loi/learning/infrastructure/contentpackaging/PackageZipWriter.kt`
  writes a `PackageExportBundle` to one archive.
- `src/test/kotlin/vn/loi/learning/application/contentpackaging/PackageExportServiceBundleTest.kt`
  and
  `src/test/kotlin/vn/loi/learning/application/contentpackaging/PackageExportIntegrationTest.kt`
  cover bundle/ZIP creation.
- No production caller was found that builds the payload from an installed package/topic.

**Assessment:** Canonical OPD3 writing exists as reusable infrastructure. Requirement 3 is not
implemented end to end, and requirement 2 has no conversion orchestration.

**Risk:** Calling the low-level export API directly can produce a syntactically valid archive
that is not a complete portable installed topic.

**Recommendation:** Add an application use case that resolves one installed topic/package,
collects its libraries, contents, learning items and media through existing repositories, then
delegates to `PackageExportService`. Keep the existing writer as the output port.

**Priority:** P0
**Must be fixed before OPD3 export:** Yes

### F04 — Current OPD3 is content-only and intentionally excludes learner state

**Source evidence**

- `src/main/kotlin/vn/loi/learning/application/contentpackaging/PackageExportPayload.kt`
  contains a descriptor, contents, learning items and optional libraries only.
- `src/main/kotlin/vn/loi/learning/application/contentpackaging/PackageImportBundle.kt`
  recognizes only metadata, contents, learning items and manifest.
- `src/main/kotlin/vn/loi/learning/domain/study/memory/model/MemoryState.kt`,
  `ReviewEvent.kt`, and `domain/study/session/model/StudySession.kt` are separate learner
  aggregates.
- `LearningApplicationFactory.kt` assigns separate JSON files for packages/content and
  `memory-states.json`, `review-events.json`, `study-sessions.json`, `study-queues.json`.

**Assessment:** OPD3 currently contains content package state, not learner progress state. This
is the correct default boundary for portable topic files.

**Risk:** Future export work could accidentally treat the unused `libraries` payload property
as learner state or add progress to a public package without a privacy/merge contract.

**Recommendation:** State explicitly in the OPD3 application contract that learner state is
excluded. If backup/sync is later required, design a separate learner-data format and consent,
identity, merge and privacy policy.

**Priority:** P0
**Must be fixed before OPD3 export:** Yes, as a documented contract; no learner data should be added

### F05 — Referenced media paths are serialized, but media bytes are not exported

**Source evidence**

- `PackageExportContentJson.kt` serializes `Content.media` path/reference fields.
- `PackageExportPlanFactory.kt` only creates metadata, contents, learning items and manifest
  entries.
- `LegacyOpd3PackageImporter.kt` and `JvmPackageContentImporter.kt` extract imported media into
  configured storage and rewrite/verify references.
- No export-side media repository/reader is used by `PackageExportService`.

**Assessment:** The writer preserves media references as strings but cannot create a
self-contained topic archive when content uses audio or images.

**Risk:** Export/re-import can yield missing media or references to machine-local storage,
violating the one-file portability requirement.

**Recommendation:** Extend export planning with verified media assets, canonical archive-relative
paths, collision detection, size/path limits and manifest hashes. Fail before writing when a
required referenced asset cannot be read; do not silently omit it.

**Priority:** P0
**Must be fixed before OPD3 export:** Yes

### F06 — Package identity is deterministic but version-sensitive

**Source evidence**

- `Sha256PackageIdGenerator.kt` hashes `descriptor.name|descriptor.version|descriptor.format`
  and returns `package-<24 hex>`.
- `ContentBasedPackageDescriptorReader.kt` synthesizes legacy-pair metadata from the JSON
  basename with version `1` and format `OPD3`.
- `PackageUpgradePlanner.kt` treats same name/format and a newer numeric version as an upgrade.

**Assessment:** Re-reading the same descriptor gives the same `PackageId`; renaming a legacy
pair or changing package version gives a new ID by design.

**Risk:** `PackageId` cannot by itself represent a stable topic across versions. Collection
membership currently points to version-specific package IDs, so upgrade can orphan membership.

**Recommendation:** Introduce a durable topic identity contract distinct from package-release
identity. Preserve `PackageId` as an immutable release/install identity.

**Priority:** P0
**Must be fixed before OPD3 export:** Yes

### F07 — Library identity changes with source path or package version

**Source evidence**

- `LegacyOpd3PackageImporter.createLibraryId` hashes the trimmed JSON source path.
- `JvmPackageContentImporter.createLibraryId` hashes the legacy package source path.
- `BundlePackageContentImporter.createLibraryId` hashes manifest name, version and format.

**Assessment:** `ContentLibraryId` is deterministic only within the same source path or exact
modern descriptor. Moving a legacy pair or importing a new package version changes library
identity.

**Risk:** Library-scoped UI selection, collections and future topic mapping do not remain stable
across export/re-import or upgrade.

**Recommendation:** Define whether a library is a user-owned container or a package-owned
installation projection. Generate its durable identity from the chosen ownership contract, not
filesystem location or release version, and provide backward-compatible mapping for existing
records.

**Priority:** P0
**Must be fixed before OPD3 export:** Yes

### F08 — Content and learning-item identity preservation is conditional

**Source evidence**

- Modern `BundlePackageContentImporter` reconstructs domain objects from IDs stored in
  `contents.json` and `learning-items.json`.
- `PackageExportContentJson` and `PackageExportLearningItemJson` serialize the existing domain
  IDs.
- `LegacyJsonImporter` generates `ContentId` from source name, group, section, lesson, English
  text, Vietnamese text and record index; learning-item IDs append normalized learning mode.

**Assessment:** Modern export/re-import can preserve `ContentId` and `LearningItemId` if the
installed IDs are exported unchanged. Legacy IDs are deterministic for the exact source name,
record order and relevant text, but are not stable after rename, reorder or compatible edits.

**Risk:** Any changed learning-item ID severs the `(learnerId, learningItemId)` progress key.
Using record index makes benign legacy reordering appear as all-new content.

**Recommendation:** Beta-L01 must establish durable content/item identity and an explicit
old-to-new compatibility mapping. Beta-L02 should convert once to those canonical IDs and export
them unchanged thereafter.

**Priority:** P0
**Must be fixed before OPD3 export:** Yes

### F09 — No durable topic identity exists

**Source evidence**

- No domain `TopicId` or topic aggregate exists.
- `StudyFacade.startLessonStudy` groups content by equality of source, group, section and lesson.
- `StudyFacade` uses `metadata.lesson ?: displayName` as `studyTitle`.
- Product-brain bootstrap accepts `topicId: String`, and Desktop passes presentation text rather
  than a persisted domain identity.

**Assessment:** “Topic” is currently a presentation/grouping convention, not a persisted
contract. `packageId`, `libraryId`, `contentId` and `learningItemId` all exist, but none serves
as a stable learner-topic identity.

**Risk:** Same-named topics can collide in UI/resume logic; renaming metadata changes perceived
topic; update/delete/order/collection operations have no authoritative topic key.

**Recommendation:** Add a durable `TopicId` and a persisted topic record that links the current
installed package release and content scope without embedding learner progress. Do not overload
`PackageId` or lesson display name.

**Priority:** P0
**Must be fixed before OPD3 export:** Yes

### F10 — Learner progress is item-scoped, not global, library-scoped or topic-scoped

**Source evidence**

- `MemoryState` owns `learnerId` and `learningItemId`.
- `ReviewEvent` requires before/after states to keep the same learner and learning item.
- `MemoryStateRecord` persists those two keys and scheduler state.
- Scheduler validation preserves both identifiers, and selection queries load memory by learner
  and item.

**Assessment:** Scheduling progress is independently owned by each learner-item pair. Topic
progress is derived by filtering/aggregating items; it is not a separate aggregate. Library and
package IDs are absent from memory state.

**Risk:** This is safe and reusable while item IDs remain stable, but offers no direct way to
answer whether progress belongs to a replaced/deleted topic when referenced content is gone.

**Recommendation:** Keep memory item-scoped. Add topic/content ownership indexes and queries
outside `MemoryState`; do not duplicate scheduler state into package or topic records.

**Priority:** P0
**Must be fixed before OPD3 export:** Yes, ownership resolution is required; memory schema need not change

### F11 — Session checkpoints persist exact scope and current presentation

**Source evidence**

- `StudySession` persists immutable `includedContentIds`, reviewed item/content IDs, current item,
  presentation time, answer visibility and pending/undo state.
- `StudySessionRecord` and the persisted queue record store those values in separate JSON files.
- `RecoverActiveStudySessionUseCase` restores an active session with a non-completed queue and
  safely closes sessions with missing/completed queues.
- Restart integration tests include
  `StudyFacadeLessonScopeRestartIntegrationTest` and
  `PersistedStudyQueueLifecycleRestartTest`.

**Assessment:** There is a real durable session/queue checkpoint and lesson-scope restart
capability.

**Risk:** The checkpoint contains content/item IDs but no topic ID or package release ownership.
Replacement/deletion can make a valid persisted checkpoint unresolvable.

**Recommendation:** Associate sessions with durable `TopicId` while retaining the immutable
content-ID snapshot needed for deterministic resume. Validate referenced items before package
replacement/removal.

**Priority:** P0
**Must be fixed before OPD3 export:** No for writing; yes before safe replacement/import

### F12 — Topic switching is not topic-selective and can leave multiple active sessions

**Source evidence**

- `StudyFacade.startLessonStudy` always creates a random new session ID and starts a new session.
- `StartStudySessionUseCase` checks only session-ID uniqueness; it does not close or suspend an
  existing active learner session.
- `StudySessionRepository.findActiveByLearner` has no topic argument.
- `StoreBackedStudySessionRepository.findActiveByLearner` returns only the most recently started
  active session.

**Assessment:** Starting another topic persists the previous session and queue as records, but
there is no explicit switch transaction and selecting an earlier topic does not restore that
topic’s checkpoint. Restart restores only the latest active learner session.

**Risk:** Multiple active sessions become hidden, progress position appears lost, and later
content replacement/deletion can strand queues.

**Recommendation:** Add a learner-topic resume repository/query and a switch use case that
atomically checkpoints the current topic, selects the target topic, and resumes or creates its
session according to one explicit active/suspended policy.

**Priority:** P0
**Must be fixed before OPD3 export:** No, but required before Beta-L04/L05 safety claims

### F13 — Duplicate detection is strong but conflict resolution is absent

**Source evidence**

- `DuplicateContentValidator` rejects duplicate semantic fingerprints within an import.
- `InstalledContentConflictValidator` rejects installed content-ID overlap, learning-item-ID
  overlap, same-content/different-ID and same-item/different-ID cases.
- `ContentFingerprintFactory` and `LearningItemFingerprintFactory` provide deterministic semantic
  fingerprints.
- `PackageRegistrationOperation` accepts an identical existing package record but rejects an ID
  collision with different metadata.

**Assessment:** Import fails before mutation with useful categories; it does not merge, replace,
map or preserve progress. Even an exact content/item overlap is rejected by the installed-content
validator before registration.

**Risk:** Idempotent re-import and compatible update cannot use the normal import path. Users
cannot distinguish “same topic”, “new version”, “duplicate copy” and “conflicting fork” through a
durable policy.

**Recommendation:** Reuse the fingerprint factories to produce a conflict plan before mutation:
unchanged, compatible mapped item, added, removed, incompatible, or unrelated. Require an
explicit policy for incompatible replacement and preserve learner state only through verified
stable/mapped item IDs.

**Priority:** P0
**Must be fixed before OPD3 export:** No for file creation; yes before supported export/re-import

### F14 — Package upgrade changes registry metadata only

**Source evidence**

- `PackageUpgradeOperation` validates same name, newer numeric version and dependencies, registers
  the replacement package, swaps catalog IDs, and deletes the old package record.
- It has no content, learning-item, library, collection, memory, review-event, session or queue
  repository dependency.

**Assessment:** The existing operation is a package-registration upgrade, not an installed-topic
content upgrade.

**Risk:** Calling it for a real topic replacement can leave old content/library ownership in
place, fail to install replacement content, retain collection references to the old package ID,
and provide no progress compatibility decision.

**Recommendation:** Do not reuse `PackageUpgradeOperation` alone as Beta-L04. Create a higher
application workflow that imports and validates replacement content, builds an identity/progress
plan, updates ownership and collections, and commits all related writes transactionally.

**Priority:** P0
**Must be fixed before OPD3 export:** No, but required before update/re-import

### F15 — Uninstall is content-aware but not learner-progress-aware

**Source evidence**

- `PackageUninstallOperation` preserves libraries/content shared by another installed package,
  then deletes unshared learning items, content, libraries, catalog membership and package record.
- `PackageRemovalDependencyGuard` checks installed-package dependencies.
- The uninstall operation has no memory-state, review-event, session, queue or collection
  repository dependency.

**Assessment:** Content ownership sharing is handled, but requirement 8 is not satisfied.
Learner state is neither preserved through a declared archive policy nor blocked/migrated before
deletion.

**Risk:** Memory and immutable review history can become orphaned; active sessions/queues can
reference deleted items; collections can retain dangling package IDs. Later cleanup could then
silently destroy progress because ownership is no longer resolvable.

**Recommendation:** Add a preflight impact report and explicit archive/delete policy. Default to
non-destructive archive or block when progress/checkpoints exist. Keep review history immutable;
define retention and restoration for memory/checkpoints before allowing destructive removal.

**Priority:** P0
**Must be fixed before OPD3 export:** No

### F16 — Archive capability is absent

**Source evidence**

- Repository search found archive readers/structure exceptions for package files but no installed
  topic/package archived state, archive use case or archived-topic query.
- Installed package and catalog models represent present/absent membership only.

**Assessment:** “Archive” in requirement 8 is not implemented; archive terminology in OPD3 code
means ZIP archive structure, not user-facing topic lifecycle.

**Risk:** UI/workflows have only keep or uninstall choices, encouraging destructive removal when
the user merely wants to hide a topic.

**Recommendation:** Model archive as persisted topic lifecycle metadata independent of OPD3
content. Archived topics should retain package/content ownership, learner state, collection
membership and resumability unless a separate delete action is confirmed.

**Priority:** P1
**Must be fixed before OPD3 export:** No

### F17 — Collection membership is separately persisted but version-coupled and unordered

**Source evidence**

- `LibraryCollection` stores `libraryId` plus a `Set<PackageId>`.
- `LibraryCollectionRecord` persists the same fields in `library-collections.json`, separate from
  package content.
- `LibraryCollectionQueryService` sorts collections by name/ID and package IDs lexically for
  presentation.
- `PackageUpgradeOperation` and `PackageUninstallOperation` do not update collections.

**Assessment:** Independent persistence for collection membership exists, satisfying part of
requirement 9. Membership points to version-specific package IDs, and set semantics cannot
represent user-defined ordering.

**Risk:** Upgrade/uninstall leaves dangling membership; lexical sorting is deterministic but is
not persisted user order.

**Recommendation:** Point collections at stable `TopicId` values and persist explicit position
or ordered membership records. Keep collection metadata outside OPD3 package content.

**Priority:** P1
**Must be fixed before OPD3 export:** No

### F18 — No persisted topic/library ordering contract exists

**Source evidence**

- `ContentLibrary.contentIds`, `PackageCatalog.packageIds`, and
  `LibraryCollection.packageIds` are sets.
- Query services impose deterministic name/ID or ID sorting at read time.
- Persistence records store sets and contain no rank, predecessor, position or order key.

**Assessment:** Deterministic display sorting exists; user-controlled topic order does not.
Requirement 9 is therefore only partially satisfied.

**Risk:** Reordering cannot survive restart, and changing names/IDs changes derived display order.

**Recommendation:** Add an independent ordered topic-membership model with stable topic ID and
explicit order value. Define deterministic insertion, move, deletion and migration behavior.

**Priority:** P2
**Must be fixed before OPD3 export:** No

### F19 — Persistence is separated by aggregate but cross-aggregate lifecycle migration is missing

**Source evidence**

- `LearningApplicationFactory` composes separate stores for libraries, collections, content,
  learning items, memory, review events, sessions, queues, packages and catalogs.
- `JsonFileTransactionRunner` is available and import/uninstall application use cases use
  transaction boundaries.
- Existing records have local schemas, but no topic-identity record or old/new item mapping record
  exists.

**Assessment:** The storage architecture is reusable and transaction-capable. The missing piece
is a migration/lifecycle plan spanning content ownership and learner references.

**Risk:** Adding a topic ID only to new records would leave existing installations, collections
and sessions unmapped. Partial migration could make content or progress unreachable.

**Recommendation:** Define a deterministic, restart-safe migration that derives topic records
from current package/library/content relationships, reports ambiguity, writes atomically, and
never discards unknown/incompatible records.

**Priority:** P0
**Must be fixed before OPD3 export:** Yes

## Identity summary

| Identity | Current generation/ownership | Stability assessment |
|---|---|---|
| `PackageId` | SHA-256 of package name, version and format, truncated to 24 hex | Stable for exact descriptor; changes on version/name/format. |
| `ContentLibraryId` (legacy pair/ZIP) | SHA-256 of source path | Changes when source moves or path spelling changes. |
| `ContentLibraryId` (modern OPD3) | SHA-256 of manifest name, version and format | Changes on package version/name/format. |
| `TopicId` | No durable domain/persistence identity; Desktop uses lesson/display strings | Not stable and not authoritative. |
| `ContentId` (modern OPD3) | Read from/written to package JSON | Stable if exporter preserves it and importer accepts it. |
| `ContentId` (legacy) | Hash of source name, metadata/text fields and record index | Changes on rename, reorder or relevant edit. |
| `LearningItemId` | Modern package value; legacy content ID plus learning mode | Progress-safe only while unchanged or explicitly mapped. |
| Learner progress key | `(LearnerId, LearningItemId)` | Correct item-level ownership; independent of package/library but vulnerable to item-ID churn. |
| Resume key | Latest active session by learner; scope is `Set<ContentId>` | Durable checkpoint, but not selectable by topic. |

## Reusable APIs and abstractions

The following should be reused rather than duplicated:

- `PackageExportService`, `PackageExportPlanFactory`, `PackageZipWriter` and
  `PackageContentExporter` for canonical output.
- `PackageImportService`, `PackageContentImporterCompat`, `BundlePackageReader` and archive
  validators for the real import/composition boundary.
- `LegacyJsonImporter`, `LegacyOpd3PackageImporter` and media extraction/path verification for
  legacy conversion.
- `ContentFingerprintFactory`, `LearningItemFingerprintFactory`,
  `DuplicateContentValidator` and `InstalledContentConflictValidator` as inputs to a richer
  conflict plan.
- Existing content, learning-item, library, package and catalog repositories for installed-topic
  assembly.
- `MemoryStateRepository`, `ReviewEventRepository`, `StudySessionRepository` and
  `StudyQueueRepository` for impact analysis and resume preservation.
- `TransactionRunner` for import, replacement, switching and deletion workflows.
- `LibraryCollectionRepository` and its separate JSON store for collection metadata, after
  replacing version-specific membership with stable topic membership.

`PackageUpgradeOperation` is reusable only for its package metadata validation concepts; it is
not a complete topic replacement workflow. `PackageExportPayload.libraries` is not currently
serialized and must not be assumed to provide installed-library portability.

## Required capability sequence

### Beta-L01 — Topic identity and resume state

**Outcome:** Establish a durable topic identity and independent learner-topic checkpoint without
moving scheduler state out of `(learnerId, learningItemId)`.

1. Define the persisted topic ownership contract linking stable `TopicId` to installed package
   release, library and content scope.
2. Specify deterministic migration for existing installed records, with explicit ambiguity
   diagnostics and no silent discard.
3. Add learner-topic session selection/checkpoint queries and an atomic switch workflow.
4. Resume the selected topic, not merely the latest learner session; retain content/item snapshot
   IDs for deterministic queue recovery.
5. Cover multiple learners, two same-name topics, restart, switch away/back, missing queue and
   legacy data migration.

**Exit gate:** Stable topic identity survives restart and package version change; each
learner-topic pair restores its own checkpoint; existing item memory remains unchanged.

### Beta-L02 — Legacy pair conversion

**Outcome:** Convert exactly one validated same-name JSON/PKG pair into the canonical installed
topic model and one canonical OPD3 export payload.

1. Unify pair resolution around explicit missing/ambiguous/orphan diagnostics.
2. Assign/preserve canonical topic, content and learning-item identities independent of source
   path and record order after the initial conversion.
3. Reuse legacy JSON conversion and media extraction/verification.
4. Assemble the canonical package payload plus media assets; fail before output on invalid or
   missing required data.
5. Test representative pair conversion through persisted import, restart and export planning,
   including the largest authorized fixture when available.

**Exit gate:** One pair deterministically produces one topic and a complete portable export plan;
repeating conversion does not create a second identity.

### Beta-L03 — OPD3 export

**Outcome:** Export one installed topic as one self-contained OPD3 file for future one-file
import.

1. Add an installed-topic export query/use case that resolves content through stable `TopicId`.
2. Preserve package/content/item IDs and package metadata according to the identity contract.
3. Add media entries with canonical relative paths and manifest hashes.
4. Explicitly exclude learner memory, review history, sessions, queues, order and collections.
5. Write atomically to the requested destination and verify export → clean import → semantic
   equality, media readability and stable identities.

**Exit gate:** An installed topic exports and imports on a clean data directory as the same topic
content identity with all media, while learner-state files are absent.

### Beta-L04 — Conflict-aware import

**Outcome:** Make modern import, legacy conversion and exported-file re-import distinguish
idempotent, compatible update, duplicate, fork and incompatible replacement.

1. Build a pre-mutation conflict plan using stable topic identity, IDs and existing fingerprints.
2. Preserve memory/history for unchanged or explicitly mapped learning items.
3. Report added, removed and incompatible items; require explicit policy for destructive
   replacement.
4. Replace package/library/content ownership and collection references in one transaction.
5. Cover rollback, restart, version upgrade, exact re-import, moved legacy source, compatible
   content edit and incompatible fork.

**Exit gate:** Compatible updates preserve learner progress; ambiguous or destructive conflicts
fail or require an explicit decision before mutation.

### Beta-L05 — Delete/archive

**Outcome:** Provide non-destructive archive and progress-aware deletion.

1. Persist topic lifecycle state independently of OPD3 package content.
2. Archive without deleting content, progress, resume state, order or collection membership.
3. Produce a delete impact report covering shared content, memory, review history, active/suspended
   sessions, queues and collections.
4. Block by default when progress would become unreachable; implement an explicit retention or
   confirmed destructive policy.
5. Ensure transaction rollback and restart safety; never partially remove a topic.

**Exit gate:** Archive is reversible; deletion cannot silently destroy or orphan progress.

### Beta-L06 — Ordering and collections

**Outcome:** Persist user topic order and collection membership independently of package content
and release version.

1. Change membership ownership from version-specific `PackageId` to stable `TopicId`.
2. Add explicit ordered membership/position with deterministic insertion and move semantics.
3. Migrate existing collection records and deterministic lexical display into stable initial
   order.
4. Keep order/collections out of OPD3 export and preserve them through compatible update,
   archive and restore.
5. Cover restart, reorder, collection moves, upgrade, archive and deletion cleanup.

**Exit gate:** Order and membership survive restart and package replacement without becoming part
of the portable content package.

## Recommended architectural boundaries

```text
Desktop library/topic UI
        ↓
Topic lifecycle, switch, import/export application workflows
        ↓
Stable topic ownership + existing package/content/session/progress ports
        ↓
JSON repositories, OPD3 readers/writers, legacy adapters, media storage
```

- OPD3 remains a portable content package.
- Topic records own local installation identity and lifecycle.
- Collections/order remain local library metadata.
- Memory and review history remain learner-item state.
- Session checkpoints link learner and stable topic while retaining immutable content/item scope.
- Import/update/delete workflows coordinate these aggregates transactionally at the application
  boundary.

## Audit conclusion

The repository has credible OPD3 import and canonical content-writing foundations, strong
validation, persistent item-level learner memory and restartable study queues. It does not yet
meet the installed-topic export, learner-topic resume, safe replacement/deletion, or persisted
ordering requirements.

Beta-L01 is the prerequisite: without stable topic and item continuity, a technically valid
export can create a new installation identity and disconnect progress on re-import. Beta-L02 and
Beta-L03 can then reuse the existing import/export/media primitives. Beta-L04 must make
re-import/update progress-safe before the exported file is presented as a supported lifecycle.
Beta-L05 and Beta-L06 complete safe local topic management without contaminating the OPD3
content boundary with learner state or local organization metadata.
