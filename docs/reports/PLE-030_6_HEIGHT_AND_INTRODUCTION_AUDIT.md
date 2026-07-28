# PLE-030.6 — Height and Introduction Audit

## Manual UAT and root cause

The Product Owner's small-monitor path was approximately 720–900 logical dp in both axes.
PLE-030.5 correctly invalidated layout resolution with width, height, density, and font scale,
so stale constraints were not the remaining cause. The resolver still selected vertical
reserves and spacing mainly from width class, retained dashboard subtitles and technical
progress text, and imposed a 120dp image-budget floor. Reflow therefore occurred but the
selected presentation remained too tall.

## Height decision and zones

`StudyVisualLayoutResolver` remains the only responsive authority. It now resolves
`COMFORTABLE`, `COMPACT_HEIGHT`, or `MINIMUM_HEIGHT` from effective available height, with font
scale included. Header/dashboard, action dock, and footer remain fixed/reserved zones. The
center learning pane receives the remaining height and retains one vertical scroll state;
statistics refresh does not recreate it. Density, font-scale, height, and monitor changes
re-resolve the immutable layout. No child reads viewport constraints and no monitor-specific
dimension is encoded.

Compact modes reduce vertical spacing, header reserve, dashboard reserve, and image cap in
that order. Images continue to use bounded `ContentScale.Fit`; they are not cropped or
stretched. Scroll remains the deterministic fallback for long meanings/examples or large font
scales, and zero-scroll is not claimed for every viewport.

Dashboard subtitles (`Latest ratings`, `Session`, `Remaining`, `Due now`) are never rendered,
while metric accessibility text remains complete. The technical experience line is removed
from the learner-facing header; its underlying progress model remains available to
accessibility/diagnostics. Generic learning badges are omitted in constrained height modes,
and a stage count is displayed only for a genuinely multi-stage flow.

## Content introduction authority

Introduction eligibility is projected from persisted session origin and `ContentId`:

- NEW plus a ContentId absent from `introducedContentIds` requires Introduction.
- NEW plus a persisted introduced ContentId continues into the existing learning flow.
- REVIEW never enters Introduction.

Introduction shows the fitted image when available, Vietnamese meaning, POS, and a replayable
Vietnamese meaning audio control. The English answer and examples remain hidden. Next Stage
persists the ContentId through `LearningEngine` and the established `StudySessionRepository`;
restart before Next returns to Introduction, while restart after Next resumes the ordinary
flow. The additive record field defaults to an empty list for legacy JSON compatibility.

The UI audio transition is keyed by current LearningItem plus persisted Introduction state.
Entering REQUIRED binds the existing `LearningContentAudioController` and calls `playOnce` for
the meaning-translation path. Recomposition, statistics refresh, resize, density changes, and
monitor moves do not change that key. Missing audio is a no-op; no English fallback or loop is
introduced, and the visible control still permits explicit replay.

Introduction exposure does not call the review transaction and does not change New, Total, or
rating buckets. The first successful rating remains the sole counter/event authority. Existing
Content-level identity, queue order, scheduler/FSRS, rating mapping, Undo, and continuous
Review Mode contracts are unchanged.

Manual UAT remains pending.

## PLE-030.7 addendum

Manual UAT found that exposure completion resumed the original technical front stage, requiring
a second Next. PLE-030.7 supersedes that transition for single non-Typing flows with one atomic
Introduction-to-persisted-Answer operation. See
`PLE-030_7_INTRODUCTION_AND_CHROME_AUDIT.md`.
