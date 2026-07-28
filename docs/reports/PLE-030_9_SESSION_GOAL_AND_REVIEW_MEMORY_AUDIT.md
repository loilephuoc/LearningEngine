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
