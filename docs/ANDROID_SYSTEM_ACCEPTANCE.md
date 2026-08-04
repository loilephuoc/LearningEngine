# Android System Acceptance

## Architecture

The acceptance boundary follows real Android orchestration rather than queue helpers:

```text
SavedState / document callback
  -> Android ViewModel or platform operation
  -> Android facade / stream adapter
  -> canonical Application service
  -> persisted engine or recovery manager
  -> refreshed Android state
```

`AndroidAcceptanceFixture` creates isolated deterministic data, media, import, and backup roots.
It composes the production persisted `LearningApplicationContext`, media storage, recovery manager,
and Android graph. Tests use controlled coroutine dispatchers and deterministic operation IDs; they
do not use network, real profiles, speakers, device storage, or wall-clock waits.

## Automated coverage

- Library lifecycle: package/item and Unicode global-query restoration, canonical browser refresh,
  stale package fallback, saved identity cleanup, and cancellation of rapidly replaced searches.
- Storage and provider faults: permission exception, destination write failure, corrupt restore,
  staging cleanup, durable-state preservation, duplicate callback protection, interruption and
  stable operation identity.
- Existing Android regression suites retain Library collections/package/browser/editor/scoped
  Study, five Recall runtimes, Practice/Undo, import/backup/restore, media lifecycle, responsive UI,
  accessibility, manifest and release graph coverage.
- Existing Root/Desktop suites remain the authority for transaction rollback, package formats,
  Scheduler/FSRS/Evidence, queue policy, Practice membership, Undo and client parity.
- Release qualification builds the minified graph and verifies R8 outputs, manifest, APK/AAB
  integrity, hashes, permissions, services and ABI/native inventory.

## Lifecycle matrix and invariants

Representative transitions cover initial load, ViewModel recreation, SavedState restoration,
callback redelivery, retry, interruption, stale navigation and rapid cancellation. Enforced
invariants are: at-most-once destination mutation, stable operation identity, no replay after a
successful callback, no stale selected entity, no lost durable state on failure, typed recovery,
no temp-file leak, and no cancelled search publishing stale results.

## Defects remediated

1. Library recreation discarded the saved selected content identity. The ViewModel reopened only
   the package and therefore returned a browser with no selected detail. Restoration now sends the
   stable content ID back through `AndroidLibraryFacade.openPackage` and the canonical browser query.
2. A saved package destination removed before recreation produced a terminal error state. The
   restoration-only path now clears stale package/content identities and reloads the safe Library
   root. Normal interactive package-open failures retain their existing behavior.
3. Global search text was persisted but the root was reloaded without replaying the canonical
   search query. Nonblank restored queries now execute once through `searchGlobal`.

The worker dispatcher is constructor-injected with the existing I/O default so lifecycle and
cancellation behavior can be tested deterministically. No learning, Recall, Practice, queue,
Scheduler, FSRS, Evidence, package, or backup semantics changed.

## Commands

Focused acceptance:

```powershell
.\gradlew.bat :android:testDebugUnitTest --tests "vn.loi.learning.android.acceptance.*" --no-daemon --console=plain
```

Complete automated acceptance and Android RC qualification:

```powershell
.\scripts\verify-android-system-acceptance.ps1
```

During an intentional dirty capability worktree, maintainers may pass `-AllowTrackedChanges`.
Generated release artifacts and evidence remain under ignored build directories.

## Automated limits

Automation does not claim physical launch, touch/IME ergonomics, real document-provider behavior,
speaker quality, image rendering on GPU/device variants, TalkBack output, rotation/process death on
Android OS, low-storage behavior, battery/memory/ANR observations, production signing, Play upload,
Play Integrity, or pre-launch report. These remain the one-pass device UAT gates.

## Verified evidence

- Focused acceptance: 2 suites / 9 tests.
- Full XML: Root 392 suites / 2,054 tests; Desktop 230 / 1,326; Android 9 / 46; total
  631 suites / 3,426 tests with zero failures, errors, or skipped tests.
- Android RC qualification: PASS; debug APK, unsigned release APK and unsigned release AAB built.
- Persisted-data device startup: PASS on `24090RA29C` after non-destructive `adb install -r`.
