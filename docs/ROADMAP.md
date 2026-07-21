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
- Actionable Desktop Study idle state without placeholder learning content

- Recoverable Desktop Study error state with retry guidance and keyboard recovery
- Accessible Desktop Study state announcements and semantic lesson progress
- Screen-reader action descriptions for every Desktop Study control
- Contextual recall and scheduling guidance for every Study rating
- Screen-reader confirmation of the persisted scheduler decision after grading
- Complete semantic reading order for the visible scheduler feedback card
- Explicit prompt and revealed-answer semantics in Desktop Study
- Stable Study keyboard focus across start, reveal, grade, retry, and completion transitions
- Ordered screen-reader summary for completed Study sessions
- Encoding-safe, readable scheduler stage transitions in Desktop Study
- Actionable first-run empty state in Desktop Content Library
- Recoverable Desktop Content Library load errors with direct retry
- Ordered screen-reader summaries for Desktop Review History
- Ordered semantic summaries for Desktop Statistics metrics
- Ordered semantic summaries for Desktop Settings configuration
- Explicit selected-state semantics for Desktop sidebar navigation
- Semantic heading and ordered status for persistent Desktop shell chrome
- Ordered semantic headings and metric summaries for Desktop Dashboard
- Semantic chart identity, empty states, and retention percentage for Dashboard visualizations
- Per-value semantics for Dashboard bars, legends, and heatmap days
- Ordered semantic summaries for Lesson Browser headers, cards, details, and properties
- Semantic page counts, import announcements, and section headings for Content Library
- Ordered purpose, context, destructive scope, and selected-state semantics for Content Library dialogs

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

Batch31 closes the real OPD3 package browsing hierarchy through lesson selection and Study navigation. Batch32 extends that same presentation-level flow through persisted restart, lesson-isolated resume, reveal, grading, and persisted completion. Batch33 adds a state-aware keyboard workflow for starting, revealing, and grading study items. Batch34 replaces the ambiguous idle placeholder with explicit start guidance and the same keyboard contract. Batch36 adds screen-reader-oriented Study state announcements and semantic lesson-progress context. Batch37 gives every Study action an explicit screen-reader description with its exact keyboard shortcut. Batch38 explains the recall and scheduling consequence of each review rating while preserving the 1–4 keyboard order. Batch39 announces the persisted scheduler result before the next question, including the saved rating, interval, and next review time. Batch40 makes the complete visible scheduler feedback card readable as one ordered semantic unit. Batch41 explicitly labels the active learning content as a prompt and exposes the translation as an answer only after reveal. Batch42 keeps the Study keyboard surface focused across all meaningful state transitions so shortcuts remain available without mouse recovery. Batch43 presents completed-session results as one ordered screen-reader summary with counts, lesson progress, and the next keyboard action. Batch44 removes corrupted scheduler stage text and routes visible and screen-reader transition labels through one tested UTF-8 presentation boundary. Batch45 makes the Content Library first-run empty state directly actionable and screen-reader clear. Batch46 separates repository/load failures from import diagnostics and gives them preserved detail, recovery guidance, assertive semantics, and a direct retry action. Batch47 gives Review History correct count grammar, a complete empty-state announcement, and one ordered semantic summary per review event. Batch48 groups every Statistics metric into one semantic unit, adds a screen-level ordered summary, and replaces ambiguous placeholder speech with an explicit unavailable state. Batch49 groups every Settings label/value pair into one semantic unit and exposes each configuration section in visible order. Batch50 gives every sidebar destination an explicit tab role, selected state, and concise spoken label. Batch51 exposes the persistent app header as one heading and the status bar as one ordered engine/dashboard announcement with stable fallbacks. Batch52 exposes Dashboard page and section headings plus ordered metric summaries with stable blank-value wording. Batch53 adds semantic chart identity, explicit no-data announcements, grouped empty states, and a clamped percentage announcement for the retention gauge. Batch54 adds per-value semantics for forecast and pressure bars, memory-stage legends, and dated heatmap activity cells. Batch55 adds ordered semantic summaries for Lesson Browser headers, lesson cards, selected lesson availability, and detail properties. Batch56 adds normalized Content Library page counts, live import announcements, and semantic section headings. Batch57 adds ordered purpose, target context, destructive scope, and selected-state semantics to all Content Library dialogs. The next work should continue the highest-value Desktop Beta UX hardening without reopening the completed functional boundary.

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
