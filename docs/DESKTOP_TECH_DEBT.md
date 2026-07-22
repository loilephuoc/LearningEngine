# Desktop Product Technical Debt

This register contains evidence-backed debt affecting the proposed Desktop product roadmap.
It excludes deferred features and historical cleanup with no demonstrated learner impact.

| Debt | Evidence | Impact | Treatment boundary |
|---|---|---|---|
| Study facade breadth | `StudyFacade` coordinates recovery, projection, formatting and commands | harder media/mode evolution | extract only when a listening capability creates a second consumer; keep transactions in application |
| Generic media roles | `LearningContentBlock.Audio` exposes a reference, not language/voice/example purpose | cannot express safe VI→EN or voice choice | backward-compatible optional role metadata |
| Playback is view-local | renderer creates `JavaSoundLearningContentAudioPlayer` | no shared sequence/loop lifecycle | inject a Desktop media-session port at composition root |
| Library hierarchy is flat | Lesson Browser projects package lesson items | Android section/topic navigation cannot map cleanly | add query projection, not persistence duplication |
| Study strings are mixed | dedicated strings coexist with literals in facade/state | localization drift risk | migrate touched flows through existing locale resources |
| Manual UI evidence pending | Test Matrix and handoff retain Phase 5/6/7 gates | release confidence is incomplete | Product Owner UAT; do not recast automation as evidence |
| Original 179 MB package untracked | only deterministic scale and wire-format fixtures exist | real media/allocation behavior remains unknown | manual benchmark and defect capture |
| Performance thresholds observational | scale harness avoids brittle timing assertions | regression can escape functional tests | optional benchmark profile after representative hardware baseline |
| Favorites absent as learner relation | Android mutates sentence fields; engine has no equivalent | tempting UI-only persistence | require durable data/compatibility decision first |
| Exercise evaluation absent | no typed response/evaluator contract | typing cannot be safely added in Compose | define application/domain contract with normalization policy |
| No product telemetry | privacy policy and measurement contract absent | learning-outcome claims lack field evidence | Product Owner privacy/measurement decision; no hidden telemetry |

## Boundaries not to refactor lightly

- atomic review/session/queue/memory/review-event transaction;
- `StudySession` recovery, pending review, and one-step undo contract;
- scheduler/FSRS and queue planning;
- OPD3/legacy package compatibility and diagnostics;
- JSON snapshot compatibility and recovery;
- safe rich-content parser and local media resolution;
- asynchronous large-data workflows added at `075b098`.

## Debt acceptance rule

A roadmap capability may retire debt only when it delivers the learner outcome that proves the
new boundary is needed. Standalone architecture cleanup is not a substitute for a vertical
capability.
