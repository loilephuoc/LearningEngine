# Learning Engine — Master Handoff

## Phase 4 fast safety-backup discovery

- Previous checkpoint: `339682b9dbaf6a3cf12b6c860b9b806c3803eed1`.
- This capability separates lightweight Android list discovery from full archive validation. The
  capability commit is the commit containing this handoff; use `git log -1 --format=%H` for its SHA.
- `safety-backup-index.json` is a disposable schema-v1 cache in the internal backup directory. It
  stores canonical path, filename, size, last-modified identity, manifest-derived package/count/byte
  metadata, validation state, and validation time. It contains no canonical learning records.
- States are `UNKNOWN`, `VALIDATING`, `VALIDATED`, `INVALID`, and `MISSING`. Only an identity-matching
  validated entry is shown as valid, while preview and restore always execute full validation again.
- Android renders the filesystem/index scan first, then validates candidates sequentially on the IO
  dispatcher. Refresh cancels the prior validation worker, re-scans disk, invalidates changed files,
  and starts a replacement worker. Retention remains full-validator-owned and newest-two.
- Changed boundaries: recovery index/model and JVM manager; Android graph, ViewModel and screen;
  focused engine/Android tests; recovery architecture, roadmap, matrix and handoff documents.
- Verification: `clean test` passes 2,251 Engine, 968 Android and 1,802 Desktop tests (5,021 total),
  with zero failures/errors/skips; `:android:assembleDebug` and `git diff --check` pass.
- APK: `android/build/outputs/apk/debug/android-debug.apk`; SHA-256
  `75081D8610821CC576F12B9D10CD68910A5087519BF8FF1C96886C6BA24332E7`.
- Next physical UAT: open Backup & Restore with multiple ~734 MB safety archives, confirm immediate
  list render and incremental statuses, then preview and restore a validated archive.
