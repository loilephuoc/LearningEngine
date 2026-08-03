# Desktop Recall Pipeline

Desktop is a renderer and interaction adapter for recall. Typing, Multiple Choice, Listening,
Image Recall, and Example Completion are integrated.
Their shared focus, keyboard, submission, result, transition, Practice, accessibility, and
completion lifecycle is defined in [`DESKTOP_RECALL_INTERACTION_CONTRACT.md`](DESKTOP_RECALL_INTERACTION_CONTRACT.md).

```text
Study session -> ProductionRecallPlanResolver -> capability + adaptive strategy
               -> ordered RecallPlanFactory attempts -> resolved RecallPlan
RecallPlan -> Desktop rendering/input -> RecallSubmission
RecallSubmission -> LearningEngine.executeRecall -> RecallResult
RecallResult -> LearningEngine.executeRecallLearning -> existing review transaction
```

The Desktop layer owns rendering, input lifecycle, animation, accessibility, navigation, and
media playback. Shared Core owns the canonical answer contract, normalization, correctness,
evidence eligibility, and learning-state commit. The existing typing timing presentation remains
an input to the established automatic-rating compatibility policy; it does not bypass the recall
execution or learning bridge.

LQ-007C routes `RecallPlan.mode` to a dedicated Multiple Choice renderer, preserves the plan's two
to four option identities and order, and submits only the selected option ID. Click and number-key
input share one attempt gate. Unsupported modes receive an explicit Desktop fallback and never
silently render as Typing. Reverse Translation and Dictation remain unwired.

LQ-007C.1 removes the production Typing decision previously fabricated by `StudyFacade`. Shared
Application now supplies strategy-selected mode authority and deterministic MCQ inventory;
unavailable MCQ attempts follow the strategy's ordered typed candidates with retained failure
provenance. Desktop adds no mode toggle, randomizer, eligibility rule, or silent fallback, and
reuses the resolved plan for the same session/item attempt across recomposition.

LQ-007D routes `RecallPlan.mode == LISTENING` to a plan-bound audio-first scene. Desktop resolves
the opaque audio identity only at its media adapter, exposes replay and Ctrl+R, collects unchanged
raw text, and submits `RecallSubmission.TypedText` through the shared execution engine and learning
bridge. Canonical answer/supporting meaning remain absent before submission. Missing or failed
audio is explicit and accessible; Desktop adds no evaluator, normalizer, rating, or mode policy.

LQ-007E routes `RecallPlan.mode == IMAGE_RECALL` to a plan-bound image-first scene. Desktop resolves
the opaque image identity through its existing media storage boundary, verifies local readability
and decoding, preserves aspect-fitted rendering, and submits unchanged raw text through the shared
execution and learning authorities. Loading, unavailable, and decode-failure states are localized
and block submission; canonical answer and supporting lexical content remain hidden. Capability
failure continues through the production resolver's typed fallback with provenance intact.

LQ-007F routes `RecallPlan.mode == EXAMPLE_COMPLETION` to a plan-bound contextual scene. Desktop
splits only the already-masked prompt at its typed `RecallTextSpan`, renders that supplied blank
without searching or replacing Content, and sends unchanged raw text through the shared execution
and learning authorities. Invalid/out-of-range or answer-bearing segments are unavailable and
cannot submit; production capability failure retains typed fallback provenance.

LQ-007X keys non-Typing initial focus by plan identity and closes the Listening unavailable-media
submission gap. It changes no mode-specific prompt/reveal UX and no Shared execution, learning,
evidence, scheduling, or queue semantics.
