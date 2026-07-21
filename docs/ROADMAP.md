# Roadmap

The source and tests are authoritative. Update this file in every completed batch.

## Done

- Core learning, review, memory-state, and scheduling foundations
- Study-session selection and queue-planning foundations
- JSON persistence and repository/store infrastructure
- Content-package import/registration/query foundations
- Content-library collections and package attachment workflows
- Dashboard, statistics, analytics, and review-history foundations
- Compose Desktop shell and major screens
- Desktop content-library and study presentation foundations
- Git-based canonical baseline and guarded batch workflow through Batch18
- Desktop recovery of the latest persisted active study session and queue
- Recovery reconciliation for missing or already-completed persisted queues
- Actionable per-package diagnostics for partial and failed Desktop directory imports
- Context-rich diagnostics for incompatible persisted content records
- Recoverable Desktop error states for incompatible persisted content data

## In progress

### Desktop Beta end-to-end learning

Target flow:

```text
Import real package
→ browse/select lesson
→ start session
→ answer/reveal/grade
→ persist progress
→ resume correctly after restart
```

Batch19 restores a valid persisted active session. Batch20 reconciles incomplete restart state. Batch21 makes real directory imports resilient by preserving valid packages and reporting incompatible candidates with their source and exact error. Batch22 identifies incompatible persisted content records by entity type and record ID while preserving the original cause. Batch23 keeps the Content Library usable when those failures occur, preserves the last good state, and provides a Refresh retry path. Remaining work must validate the complete study flow against representative imported packages and extend equivalent resilience to the study/review path where needed.

## Planned after the end-to-end flow

- Real-data robustness and actionable validation/error reporting
- Session recovery and persistence restart coverage
- Desktop UX polish, accessibility, keyboard flow, and empty/error states
- Performance profiling with large real packages
- Desktop Beta packaging and release checklist
- Android client
- iOS client
- Web client

## Batch policy

Each batch must deliver one complete, testable increment toward the nearest product milestone. Do not reserve batch numbers for placeholders and do not mark work Done until it exists in the supplied source and passes the required verification.
