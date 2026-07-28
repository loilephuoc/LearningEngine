# PLE-030.9 — Session Goal and Review Memory Audit

## Root causes

`LearningShell` remembered `StudyFacade` once, including a policy-provider closure created from
the initial `runtimeConfiguration` parameter. Settings were persisted and the Compose state
updated, but a later session could still receive the startup goals from that retained closure.
The session itself already owned the correct downstream authority: queue admission and header
configured targets both consume `StudySession.policy`.

The pre-answer dock also projected four rating-like segments for NEW and REVIEW. Content-level
previous-rating resolution was already correct, but NEW should expose no memory indicator and
the visual treatment looked like disabled controls.

## Corrected data flow

```text
Persisted DesktopRuntimeConfiguration
    → fresh load for each new session
    → SessionPolicy
    → StudySession.policy
    → queue limits + header configured targets
```

An active session retains its immutable policy. Editing Settings therefore affects the next
new/restarted session without rewriting completed counters or history.

```text
ContentId + immutable SessionItemOrigin
    → ContentLearningState.latestEffectiveRating
    → CurrentStudyItemReviewContext
    → REVIEW-only read-only footer
    → existing interactive Rating Dock after Full Answer
```

NEW renders no memory status. REVIEW uses the same four-label, latest-rating-only semantics for
Prompt, Image, Listening, and Typing recall. The labels have no click, hover, ripple, focus, or
pointer behavior and use only subdued typography/underline emphasis. Scheduler identity and
rating semantics remain unchanged.

Focused policy/queue/header/identity/dock/keyboard verification passed 32 tests.
`.\gradlew.bat clean test --no-daemon` passed 2,607 tests (root 1,698; Desktop 909), with no
failures, errors, or skipped tests. Manual UAT remains pending.

## Final UAT remediation

### Exact Review Experience audit

Before remediation:

| Experience | Memory Status render path | Gap |
| --- | --- | --- |
| Prompt | `FRONT_CONTEXT` | rendered only while `canRevealAnswer=true` |
| Image | `FRONT_CONTEXT` | rendered only while `canRevealAnswer=true` |
| Listening | `FRONT_CONTEXT` | rendered only while `canRevealAnswer=true` |
| Typing | `REVIEW_CONTEXT` | rendered only while `canRevealAnswer=true` |
| Introduction | `INTRODUCTION` calls the shared renderer | planner/state policy makes Introduction NEW-only, so REVIEW correctly never enters it |

The missing-status root cause was the shared dock-mode predicate: REVIEW eligibility was tied
to the reveal action flag. A valid pre-answer REVIEW state without that action fell through to
`HIDDEN`, while the independent scheduler feedback remained visible. The corrected fallback is
semantic: any `SessionItemOrigin.REVIEW` state before Full Answer uses `REVIEW_CONTEXT`,
regardless of action availability. Prompt/Image/Listening retain their corresponding Next
action when one exists; Typing retains its input; the footer itself remains read-only.

### Image-size root cause and correction

Three independent constraints reduced image area:

1. pre-answer `LearningSceneRenderer` did not receive `StudyVisualLayout`, so
   `VocabularyImageBlock` used its 620×340 fallback;
2. Standard/Wide layout policy capped images at 620×150/200 even when the viewport had room;
3. the child `Image` applied another fixed 340dp height cap.

The resolver now allocates 90% of available content width plus the remaining adaptive vertical
answer budget. Pre-answer scenes are constrained to the same content width and pass the layout
to `VocabularyImageBlock`; the child consumes the resolved height. `ContentScale.Fit` remains
authoritative, so images enlarge without distortion or crop and stay bounded by the viewport
with the existing center-scroll fallback.

Focused remediation verification passed 82 tests. Final
`.\gradlew.bat clean test --no-daemon` passed 2,607 tests (root 1,698; Desktop 909), with no
failures, errors, or skipped tests. Manual re-UAT remains pending.

## Active-session goal remediation

Settings persistence was not the remaining failure. Navigation back to Study called the normal
refresh path, which recovered the existing active session and its immutable 50/200 policy.
`studySessionPolicyProvider` was consulted only by new-session creation, so the persisted 10/20
values never reached that resumed session.

Study entry now compares an explicit `(newItemLimit, reviewItemLimit)` fingerprint from the
active session policy with a fresh persisted-policy read. A match resumes the session unchanged;
unrelated runtime settings therefore cannot reset Study. A mismatch finishes the stale session
without mutating its policy or history, preserves its package/topic/lesson scope, clears transient
Study state, and creates a new session with the exact freshly read policy. Header, queue, planner,
and both admission limits consequently consume the same new immutable `StudySession.policy`, and
the new session starts with zero counters rather than producing an invalid value such as 21/10.

Focused stale-session verification passed 22 tests. Final
`.\gradlew.bat clean test --no-daemon` passed 2,633 tests (root 1,721; Desktop 912), with no
failures, errors, or skipped tests.
