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
- Recoverable Desktop error states for study/review persistence failures
- Atomic persisted review transactions including study queue state
- OPD3 manifest/metadata identity consistency validation
- Persisted OPD3 import-to-review restart integration coverage
- Persisted study queues in the Desktop application composition root
- Desktop lesson-scoped study and restart isolation coverage
- Content Library lesson-study navigation boundary coverage
- Real OPD3 Content Library browse-to-study presentation coverage
- OPD3 Desktop grading, persisted restart, resume, and completion coverage
- State-aware Desktop Study keyboard workflow with visible shortcuts

## In progress

### Desktop Beta UX hardening

The functional Desktop Beta learning path is now covered end to end:

```text
Import real OPD3 package
→ browse/select lesson
→ start session
→ recreate the application
→ resume the same lesson queue
→ reveal answer and grade
→ persist completion
```

Batch31 closes the real OPD3 package browsing hierarchy through lesson selection and Study navigation. Batch32 extends that same presentation-level flow through persisted restart, lesson-isolated resume, reveal, grading, and persisted completion. Batch33 adds the first UX-hardening increment: a state-aware keyboard workflow for starting, revealing, and grading study items. The next work should continue accessibility and empty/error-state hardening without reopening the completed functional boundary.

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
